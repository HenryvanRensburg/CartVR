package com.cartvr.app.data.remote

import com.cartvr.app.BuildConfig
import com.cartvr.app.data.remote.dto.EdgeActionResponse
import com.cartvr.app.data.remote.dto.FamilyOverviewResponse
import com.cartvr.app.data.remote.dto.InvitePreviewResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FamilyApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun functionUrl(name: String): String =
        "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$name"

    suspend fun previewInvite(code: String): Result<InvitePreviewResponse> =
        postFunction(
            name = "accept-family-invite",
            accessToken = null,
            body = buildJsonObject {
                put("action", "preview")
                put("code", code.trim().uppercase())
            }
        ) { text ->
            json.decodeFromString<InvitePreviewResponse>(text)
        }

    suspend fun acceptInvite(
        code: String,
        email: String,
        password: String,
        displayName: String
    ): Result<EdgeActionResponse> =
        postFunction(
            name = "accept-family-invite",
            accessToken = null,
            body = buildJsonObject {
                put("action", "accept")
                put("code", code.trim().uppercase())
                put("email", email.trim())
                put("password", password)
                put("displayName", displayName.trim())
            }
        ) { text ->
            json.decodeFromString<EdgeActionResponse>(text)
        }

    suspend fun fetchFamilyOverview(accessToken: String): Result<FamilyOverviewResponse> =
        postFunction(
            name = "admin-manage-family",
            accessToken = accessToken,
            body = buildJsonObject {
                put("action", "list")
            }
        ) { text ->
            json.decodeFromString<FamilyOverviewResponse>(text)
        }

    suspend fun createMemberDirectly(
        accessToken: String,
        displayName: String,
        email: String,
        password: String,
        role: String,
        autoListIds: List<String>
    ): Result<EdgeActionResponse> =
        postFunction(
            name = "admin-manage-family",
            accessToken = accessToken,
            body = buildJsonObject {
                put("action", "create_member")
                put("displayName", displayName.trim())
                put("email", email.trim())
                put("password", password)
                put("role", role)
                put("autoListIds", JsonArray(autoListIds.map { JsonPrimitive(it) }))
            }
        ) { text ->
            json.decodeFromString<EdgeActionResponse>(text)
        }

    suspend fun removeMember(
        accessToken: String,
        targetUserId: String
    ): Result<Unit> =
        postFunction(
            name = "admin-manage-family",
            accessToken = accessToken,
            body = buildJsonObject {
                put("action", "remove_member")
                put("targetUserId", targetUserId)
            }
        ) { }

    suspend fun updateMemberRole(
        accessToken: String,
        targetUserId: String,
        role: String
    ): Result<Unit> =
        postFunction(
            name = "admin-manage-family",
            accessToken = accessToken,
            body = buildJsonObject {
                put("action", "update_role")
                put("targetUserId", targetUserId)
                put("role", role)
            }
        ) { }

    private suspend fun <T> postFunction(
        name: String,
        accessToken: String?,
        body: JsonObject,
        decode: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val response = httpClient.post(functionUrl(name)) {
                header("apikey", BuildConfig.SUPABASE_KEY)
                if (!accessToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val errMsg = runCatching {
                    json.decodeFromString<EdgeActionResponse>(text).error
                }.getOrNull()
                error(errMsg ?: "Request failed (${response.status.value})")
            }
            decode(text)
        }
    }
}
