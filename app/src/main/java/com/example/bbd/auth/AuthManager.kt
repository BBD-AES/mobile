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

    /** 로그아웃 = end-session Intent(없으면 null). 호출부에서 launch 후 [clearLocal]. */
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
        prefs().edit().remove(KEY_STATE).apply()
    }

    fun dispose() {
        if (::service.isInitialized) service.dispose()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun persist() = prefs().edit().putString(KEY_STATE, authState.jsonSerializeString()).apply()
}
