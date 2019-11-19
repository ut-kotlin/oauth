package inmotionsoftware.com.oauthclient.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class UserProfile (
        val userId: String,
        val displayName: String
)
