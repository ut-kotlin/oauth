package inmotionsoftware.com.oauthclient.data.model

import com.google.gson.annotations.SerializedName
import java.util.*

class OAuthToken(
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)