package com.example.bbd.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.bbd.BuildConfig
import com.example.bbd.data.remote.Net
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** refresh 결과 — 만료(세션 정리 대상)와 네트워크 일시오류(토큰 보존·재시도)를 구분한다. */
enum class TokenRefresh { FRESH, EXPIRED, NETWORK_ERROR }

/**
 * Keycloak OIDC(Authorization Code + PKCE/S256) 인증 — AppAuth 래퍼.
 *
 * 흐름: [beginLogin] 으로 인증 Intent(Custom Tab) → [handleResult] 로 토큰 교환 → access token 을 [Net.bearer] 에 주입.
 * 매 호출 전 [withFreshToken] 으로 refresh. 설정값은 BuildConfig(AUTH_*) — gradle property 로 오버라이드 가능.
 *
 * 참고: AuthState 를 SharedPreferences 에 평문 저장(프로토타입). 운영 hardening = EncryptedSharedPreferences.
 */
object AuthManager {

    private const val PREFS = "bbd_auth"
    private const val KEY_STATE = "auth_state"

    private lateinit var appContext: Context
    private lateinit var service: AuthorizationService
    private var authState: AuthState = AuthState()
    private var serviceConfig: AuthorizationServiceConfiguration? = null

    /**
     * '진짜 만료'(invalid_grant·인가오류·refresh token 부재) 감지 플래그.
     * 백그라운드 refresh(401 Authenticator)가 만료를 만나면 세우고, 포그라운드(홈 폴링/화면)가 보고 로그인으로 라우팅한다.
     * clearLocal 에서 리셋. (네트워크/IO 일시오류로는 세우지 않음 — 토큰 보존·재시도.)
     */
    @Volatile
    var sessionExpired: Boolean = false
        private set

    /**
     * 세션 강제 종료 사유 — 게이트웨이 AUTH003(다른 기기에서 로그인) 등. null=정상.
     * 어느 화면에서든 즉시 로그인 복귀시키기 위해 전역(App)이 collect 하고, 로그인 화면이 사유를 안내한다.
     */
    private val _sessionEnded = MutableStateFlow<String?>(null)
    val sessionEnded: StateFlow<String?> get() = _sessionEnded

    fun init(context: Context) {
        appContext = context.applicationContext
        service = AuthorizationService(appContext)
        prefs().getString(KEY_STATE, null)?.let { json ->
            runCatching { AuthState.jsonDeserialize(json) }.getOrNull()?.let { restored ->
                authState = restored
                serviceConfig = restored.authorizationServiceConfiguration
                restored.accessToken?.let { Net.bearer = it }
            }
        }
    }

    val isAuthorized: Boolean get() = authState.isAuthorized

    /** issuer 디스커버리 후 인증 요청 Intent 를 콜백으로 전달(null = 연결 실패). 콜백은 메인 스레드. */
    fun beginLogin(onReady: (Intent?) -> Unit) {
        AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.AUTH_ISSUER)) { config, _ ->
            if (config == null) {
                onReady(null)
            } else {
                serviceConfig = config
                authState = AuthState(config)
                val req = AuthorizationRequest.Builder(
                    config,
                    BuildConfig.AUTH_CLIENT_ID,
                    ResponseTypeValues.CODE,
                    Uri.parse(BuildConfig.AUTH_REDIRECT),
                ).setScope(BuildConfig.AUTH_SCOPES).build()
                onReady(service.getAuthorizationRequestIntent(req))
            }
        }
    }

    /** 리다이렉트 결과 처리 → 토큰 교환 → [Net.bearer] 주입. onResult(성공, 에러메시지?). */
    fun handleResult(data: Intent?, onResult: (Boolean, String?) -> Unit) {
        if (data == null) { onResult(false, "로그인이 취소되었어요."); return }
        val resp = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)
        authState.update(resp, ex)
        if (resp == null) { onResult(false, ex?.errorDescription ?: "인증에 실패했어요."); return }
        service.performTokenRequest(resp.createTokenExchangeRequest()) { tokenResp, tokenEx ->
            authState.update(tokenResp, tokenEx)
            persist()
            if (tokenResp?.accessToken != null) {
                Net.bearer = tokenResp.accessToken
                onResult(true, null)
            } else {
                onResult(false, tokenEx?.errorDescription ?: "토큰 교환에 실패했어요.")
            }
        }
    }

    /** API 호출 전 신선한 토큰 확보(필요 시 자동 refresh) → [Net.bearer] 갱신. */
    fun withFreshToken(onToken: (String?) -> Unit) {
        authState.performActionWithFreshTokens(service) { accessToken, _, _ ->
            accessToken?.let { Net.bearer = it }
            onToken(accessToken)
        }
    }

    /**
     * suspend 버전 — 시작 시 세션 자동복원(영속 토큰)에서 사용.
     * 실패를 만료(TYPE_OAUTH_TOKEN_ERROR=invalid_grant 등 진짜 무효 → 세션만료)와
     * 네트워크/IO 일시오류(TYPE_GENERAL_ERROR 등 → 토큰 보존)로 구분한다(일시 끊김에 정상 토큰 파기 방지).
     */
    suspend fun freshToken(): TokenRefresh = suspendCancellableCoroutine { cont ->
        authState.performActionWithFreshTokens(service) { accessToken, _, ex ->
            val result = when {
                // 성공: 토큰 주입 + 회전된 refresh token 디스크 반영(persist) — rotation+재시작 조합에서 옛 토큰 복원 방지.
                accessToken != null -> { Net.bearer = accessToken; persist(); TokenRefresh.FRESH }
                isExpiry(ex) -> TokenRefresh.EXPIRED          // 진짜 만료 — 정리 대상
                else -> TokenRefresh.NETWORK_ERROR            // 네트워크/IO 일시오류 — 토큰 보존·재시도
            }
            if (cont.isActive) cont.resume(result)
        }
    }

    /**
     * 동기 refresh — 401 OkHttp Authenticator 전용(백그라운드 스레드에서 블로킹 호출).
     * 새 access token 또는 null(refresh 실패). 진짜 만료면 [sessionExpired] 만 세우고 로컬은 보존(정리/라우팅은 포그라운드가).
     * await 타임아웃은 readTimeout(20s) 아래로 둔다. (AppAuth 가 동시 refresh 를 coalesce — 단일 authState.)
     */
    fun blockingFreshToken(): String? {
        if (!::service.isInitialized) return null
        val latch = java.util.concurrent.CountDownLatch(1)
        val ref = java.util.concurrent.atomic.AtomicReference<String?>()
        authState.performActionWithFreshTokens(service) { accessToken, _, ex ->
            if (accessToken != null) { Net.bearer = accessToken; persist(); ref.set(accessToken) }
            else if (isExpiry(ex)) sessionExpired = true
            latch.countDown()
        }
        runCatching { latch.await(15, java.util.concurrent.TimeUnit.SECONDS) }
        return ref.get()
    }

    /**
     * refresh 실패가 '진짜 만료'인가 — invalid_grant(토큰오류)/인가오류/ refresh token 부재.
     * AppAuth 는 refresh token 부재/회전 무효를 TYPE_OAUTH_AUTHORIZATION_ERROR(type 1)로도 던지므로 둘 다 포함.
     * (TYPE_GENERAL_ERROR 등 네트워크/IO 는 false → 보존·재시도.)
     */
    private fun isExpiry(ex: AuthorizationException?): Boolean =
        authState.refreshToken == null ||
            ex?.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR ||
            ex?.type == AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR

    /**
     * 로그아웃 1단계 — refresh_token back-channel 무효화(public client, secret 없음).
     * POST {end_session_endpoint} (client_id + refresh_token, x-www-form-urlencoded). best-effort:
     * 실패해도 브라우저 end-session·로컬 삭제는 진행. **clearLocal 전에 호출**(refresh/id 토큰 보존 순서).
     */
    suspend fun revokeRefreshToken() = withContext(Dispatchers.IO) {
        val endpoint = serviceConfig?.endSessionEndpoint?.toString() ?: return@withContext
        val refresh = authState.refreshToken ?: return@withContext
        runCatching {
            val body = FormBody.Builder()
                .add("client_id", BuildConfig.AUTH_CLIENT_ID)
                .add("refresh_token", refresh)
                .build()
            OkHttpClient().newCall(Request.Builder().url(endpoint).post(body).build())
                .execute().use { /* 응답 무시 — best-effort */ }
        }
        Unit
    }

    /**
     * Gateway 모바일 세션 슬롯 해제. Redis current sid 는 access token 의 sid 기준으로 지워지므로,
     * 로컬 토큰 삭제/Keycloak end-session 전에 best-effort 로 호출한다.
     */
    suspend fun logoutGatewayMobileSession() = withContext(Dispatchers.IO) {
        runCatching { freshToken() }
        val token = authState.accessToken ?: Net.bearer ?: return@withContext
        val endpoint = BuildConfig.BASE_URL.trimEnd('/') + "/api/auth/mobile/logout"
        runCatching {
            OkHttpClient().newCall(
                Request.Builder()
                    .url(endpoint)
                    .post(ByteArray(0).toRequestBody())
                    .header("Authorization", "Bearer $token")
                    .build()
            ).execute().use { /* best-effort: 실패해도 Keycloak/로컬 로그아웃은 계속 진행 */ }
        }
        Unit
    }

    /** 로그아웃 2단계 — end-session Intent(브라우저, id_token_hint=id_token + post_logout_redirect). 없으면 null. 복귀 후 [clearLocal]. */
    fun endSessionIntent(): Intent? {
        val config = serviceConfig ?: return null
        val idToken = authState.idToken ?: return null
        val req = EndSessionRequest.Builder(config)
            .setIdTokenHint(idToken)
            .setPostLogoutRedirectUri(Uri.parse(BuildConfig.AUTH_END_SESSION_REDIRECT))
            .build()
        return service.getEndSessionRequestIntent(req)
    }

    fun clearLocal() {
        authState = AuthState()
        Net.bearer = null
        sessionExpired = false
        prefs().edit().remove(KEY_STATE).apply()
    }

    /**
     * 게이트웨이 AUTH003(모바일 단일 기기 제한) — 액세스 토큰은 유효하나 게이트웨이가 sid 로 차단한다.
     * refresh 는 무의미(같은 sid 라 계속 막힘)하므로, 차단된 토큰을 버리고 전역 세션 종료 신호를 세운다.
     */
    fun markSessionReplaced() {
        Net.bearer = null
        _sessionEnded.value = "이미 다른 기기에서 로그인 중입니다. 기존 기기에서 로그아웃하거나 관리자에게 문의하세요."
    }

    /** 세션 종료 사유 소거 — 재로그인 시도/성공 시 호출. */
    fun clearSessionEnded() {
        _sessionEnded.value = null
    }

    fun dispose() {
        if (::service.isInitialized) service.dispose()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun persist() = prefs().edit().putString(KEY_STATE, authState.jsonSerializeString()).apply()
}
