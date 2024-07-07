package org.qbychat.android.utils

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.qbychat.android.Account
import org.qbychat.android.Authorize
import org.qbychat.android.RestBean

private val httpClient = OkHttpClient.Builder()
    .build()
const val HTTP_PROTOCOL = "https://"
const val WS_PROTOCOL = "wss://"
const val BACKEND = "backend.lunarclient.top"

fun login(username: String, password: String): Authorize? {
    val body = "username=$username&password=$password".toRequestBody("application/x-www-form-urlencoded".toMediaType())
    val request = Request.Builder()
        .url("$HTTP_PROTOCOL$BACKEND/user/login")
        .post(body)
        .build()
    with(httpClient.newCall(request).execute()) {
        if (this.body == null) {
            return null
        }
        val response = Json.decodeFromString<RestBean<Authorize>>(this.body!!.string())
        if (response.code != 200) return null
        return response.data
    }
}