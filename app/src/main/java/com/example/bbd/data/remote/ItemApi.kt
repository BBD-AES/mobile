package com.example.bbd.data.remote

import com.example.bbd.data.remote.dto.ItemDto
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 품목 마스터 REST (item-service). BASE_URL=게이트웨이 루트라 경로에 prefix(item/) 포함.
 * 컨트롤러 @RequestMapping("/api/v1/items") · 게이트웨이 prefix /item.
 */
interface ItemApi {

    /** SKU 단건 해석 — 현장수주 부품 추가(번들 시드 카탈로그 대체). 미존재=404. */
    @GET("item/api/v1/items/{sku}")
    suspend fun byCode(@Path("sku") sku: String): Response<ItemDto>

    /** 품목 목록 — 재고 화면 안전재고 판정 보강용. */
    @GET("item/api/v1/items/filter")
    suspend fun filter(
        @Query("size") size: Int = 500,
        @Query("sortBy") sortBy: String = "name",
        @Query("direction") direction: String = "ASC",
        @Query("active") active: Boolean = true,
    ): JsonElement

    /** OpenSearch 기반 품목 자동검색 — 현장수주 '검색으로 추가'에서 사용. */
    @GET("item/api/v1/items/search/auto")
    suspend fun autocomplete(
        @Query("keyword") keyword: String,
        @Query("size") size: Int = 10,
        @Query("active") active: Boolean = true,
        @Query("sourcingType") sourcingType: String? = null,
    ): JsonElement
}
