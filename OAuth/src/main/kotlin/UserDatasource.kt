class UserDatasource {
    private val users = mapOf<String,User>(
        "brian@test.com" to User("brian@test.com", "Brian", password = "abc123")
    )

    fun getUser(email: String): User? {
        return users.get(email)
    }

    fun authenticate(email: String, pass: String): User? {
        val user = getUser(email) ?: return null
        return if (user.password == pass) user else null
    }
}