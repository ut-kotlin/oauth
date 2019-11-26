data class UserProfile (
    val userId: String,
    val displayName: String
)

data class User (
    val profile: UserProfile,
    val password: String
) {
    constructor(email: String, name: String, password: String): this(UserProfile(email, name), password) {}
}