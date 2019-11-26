package inmotionsoftware.com.oauthclient.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.*
import inmotionsoftware.com.oauthclient.data.LoginRepository
import inmotionsoftware.com.oauthclient.data.Result
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

import inmotionsoftware.com.oauthclient.R
import inmotionsoftware.com.oauthclient.data.model.OAuthToken

typealias LoginResult = Result<OAuthToken>

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    suspend fun login(username: String, password: String) {
        viewModelScope.launch (
            context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val rt = loginRepository.login(username, password)
            _loginResult.postValue(rt)
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}
