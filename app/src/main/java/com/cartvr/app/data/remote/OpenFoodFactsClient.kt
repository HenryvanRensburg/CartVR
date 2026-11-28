package com.cartvr.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

data class OffProductLookupResult(
    val name: String,
    val brand: String?,
    val quantity: String?
)

@Serializable
private data class OffApiResponse(
    val status: Int = 0,
    val product: OffProductPayload? = null
)

@Serializable
private data class OffProductPayload(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    val brands: String? = null,
    val quantity: String? = null
)

/**
 * Lightweight client for Open Food Facts product lookup by barcode.
 * Uses short timeouts so offline or slow in-store connections fall back quickly to manual entry.
 */
object OpenFoodFactsClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(3500, TimeUnit.MILLISECONDS)
                    readTimeout(3500, TimeUnit.MILLISECONDS)
                    callTimeout(4000, TimeUnit.MILLISECONDS)
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    suspend fun lookupProduct(rawBarcode: String): OffProductLookupResult? {
        val cleaned = rawBarcode.trim()
        if (cleaned.isEmpty()) return null
        return runCatching {
            val response = httpClient.get(
                "https://world.openfoodfacts.org/api/v2/product/$cleaned.json?fields=product_name,product_name_en,brands,quantity"
            ) {
                header("User-Agent", "CartVR/2.1.0 (Android; Household Shopping App)")
            }
            if (!response.status.isSuccess()) return@runCatching null
            val parsed = json.decodeFromString<OffApiResponse>(response.bodyAsText())
            if (parsed.status != 1) return@runCatching null
            val payload = parsed.product ?: return@runCatching null
            val rawName = payload.productName?.trim()?.takeIf { it.isNotEmpty() }
                ?: payload.productNameEn?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@runCatching null
            val brand = payload.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            val fullName = if (brand != null && !rawName.contains(brand, ignoreCase = true)) {
                "$brand $rawName"
            } else {
                rawName
            }
            OffProductLookupResult(
                name = fullName,
                brand = brand,
                quantity = payload.quantity?.trim()?.takeIf { it.isNotEmpty() }
            )
        }.getOrNull()
    }
}
