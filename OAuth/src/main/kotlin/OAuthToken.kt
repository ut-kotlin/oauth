import org.json.JSONObject

data class OAuthToken (
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

fun OAuthToken.toJson(): String {
    return JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken)
        .put("token_type", tokenType)
        .put("expires_in", expiresIn)
        .toString()
}