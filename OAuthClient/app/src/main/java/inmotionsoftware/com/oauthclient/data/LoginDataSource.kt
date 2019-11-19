package inmotionsoftware.com.oauthclient.data

import inmotionsoftware.com.oauthclient.data.model.UserProfile
import java.io.IOException
import inmotionsoftware.com.oauthclient.data.model.OAuthToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.lang.RuntimeException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resumeWithException
import com.google.gson.Gson
import java.lang.Exception


val JSON = "application/json".toMediaTypeOrNull()!!

fun JSONObject.toRequestBody(): RequestBody {
    return this.toString().toRequestBody(JSON)
}

suspend fun Call.await(): Response {
    return suspendCoroutine { continuation ->
        enqueue(object: Callback {

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource() {
    private val clientId = "41515389-7EBE-466B-B532-394B7E9998D4"
    private val clientSecret = "btENnDHAiXc8CgW54FXBtO3x9wnkEerepAl0vsim"
    private val host = "10.0.5.42"
    private val port = 4567
    private val loginURL = URL("http://${host}:${port}/oauth/token")
    private val userURL = URL("http://${host}:${port}/user")
    private var auth: OAuthToken? = null

    private val client = OkHttpClient()

    val isLoggedIn: Boolean
        get() = auth != null

    private suspend fun<T: Any> send(request: Request, type: Class<T>): Result<T> {
        try {
            val response = client
                .newCall(request)
                .await()

            return when (response.code) {
                200 -> {
                    val body = response.body?.string()
                    val obj = Gson().fromJson(body, type)
                    Result.Success(obj)
                }
                401 ->  Result.Error(RuntimeException("Unauthorized"))
                500 ->  Result.Error(RuntimeException("Server Error"))
                400 ->  Result.Error(RuntimeException("Client Error"))
                else -> Result.Error(RuntimeException("HTTP Error: ${response.code}"))
            }
        } catch(t: Exception) {
            return Result.Error(t)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        val auth = this.auth ?: return Result.Error(RuntimeException("Unauthorized"))

        val request = Request.Builder()
            .url("${userURL}/${userId}")
            .get()
            .addHeader("Authorization", "${auth.tokenType} ${auth.accessToken}")
            .build()

        return send(request, UserProfile::class.java)
    }

    suspend fun login(username: String, password: String): Result<OAuthToken> {
        val body = FormBody.Builder()
            .add("grant_type", "password")
            .add("username", username)
            .add("password", password)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()

        val request = Request.Builder()
            .url(loginURL)
            .post(body)
            .build()

        val result = send(request, OAuthToken::class.java)
        this.auth = if (result is Result.Success) result.data else null
        return result
    }

    fun logout() {
        this.auth = null
    }
}

