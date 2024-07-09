package org.qbychat.android.utils

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.qbychat.android.Account
import org.qbychat.android.Authorize
import org.qbychat.android.Friend
import org.qbychat.android.Group
import org.qbychat.android.Message
import org.qbychat.android.MessengerRequest
import org.qbychat.android.MessengerResponse
import org.qbychat.android.RequestType
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
        val response = JSON.decodeFromString<RestBean<Authorize?>>(this.body!!.string())
        if (response.code != 200) return null
        return response.data
    }
}

private inline fun <reified T> String.invokeAPI(api: String): T? {
    val request = Request.Builder()
        .url("$HTTP_PROTOCOL$BACKEND$api")
        .get()
        .header("Authorization", "Bearer $this")
        .build()
    with(httpClient.newCall(request).execute()) {
        if (this.body == null) return null // unreachable
        val response = JSON.decodeFromString<RestBean<T>>(this.body!!.string())
        return response.data
    }
}

// String: token
fun String.getGroups(): List<Group>? = this.invokeAPI("/user/groups/list")

fun String.getFriends(): List<Friend>?  = this.invokeAPI("/user/friends/list")

fun String.account(): Account? = this.invokeAPI("/user/account")

fun String.updateFCMToken(fcmToken: String): Boolean {
    val body = "newToken=$fcmToken".toRequestBody("application/x-www-form-urlencoded".toMediaType())
    val request = Request.Builder()
        .url("$HTTP_PROTOCOL$BACKEND/user/fcm/token")
        .header("Authorization", this)
        .post(body)
        .build()
    with(httpClient.newCall(request).execute()) {
        return this.isSuccessful
    }
}

fun saveAuthorize(mContext: Context, authorize: Authorize) {
    mContext.filesDir.resolve("account.json").writeText(
        JSON.encodeToString(Authorize.serializer(), authorize)
    )
}

// Int: userId
private val accountMap = mutableMapOf<Int, Account>()

val Int.account: Account?
    get() {
        if (accountMap.containsKey(this)) return accountMap[this]

        val request = Request.Builder()
            .url("$HTTP_PROTOCOL$BACKEND$/user/query?id=$this")
            .get()
            .build()
        with(httpClient.newCall(request).execute()) {
            if (this.body == null) return null // unreachable
            val response = JSON.decodeFromString<RestBean<Account>>(this.body!!.string())
            val account = response.data
            accountMap[this@account] = account
            return account
        }
    }

// WS
fun String.connect(onMessageReceived: (websocket: WebSocket, response: String) -> Unit): WebSocket {
    val request: Request = Request.Builder()
        .url("$WS_PROTOCOL$BACKEND/ws/messenger")
        .header("Authorization", "Bearer $this")
        .build()

    val webSocket: WebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.i("Websocket", "WebSocket opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.i("Websocket" ,"Received message: $text")
            onMessageReceived(webSocket, text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.printStackTrace()
        }
    })
    return webSocket
}


