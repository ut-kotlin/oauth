package inmotionsoftware.com.oauthclient.data

import inmotionsoftware.com.oauthclient.data.model.UserProfile
import inmotionsoftware.com.oauthclient.data.model.OAuthToken

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    // in-memory cache of the loggedInUser object
    var userProfile: UserProfile? = null
        private set

    val isLoggedIn: Boolean
        get() = dataSource.isLoggedIn

    fun logout() {
        userProfile = null
        dataSource.logout()
    }

    suspend fun getUserProfile(username: String): Result<UserProfile> {
        val result = dataSource.getUserProfile(username)
        this.userProfile = if (result is Result.Success) result.data else null
        return result
    }

    suspend fun login(username: String, password: String): Result<OAuthToken>
            = dataSource.login(username, password)
}
