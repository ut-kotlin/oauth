package inmotionsoftware.com.oauthclient.ui.login

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import inmotionsoftware.com.oauthclient.R
import inmotionsoftware.com.oauthclient.data.LoginDataSource
import inmotionsoftware.com.oauthclient.data.LoginRepository
import inmotionsoftware.com.oauthclient.data.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class UserActivity: AppCompatActivity() {
    private val scope = CoroutineScope(newSingleThreadContext("UserScope"))
    private lateinit var welcome: TextView
    private lateinit var loading: ProgressBar
    private lateinit var refresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user)

        this.welcome = findViewById<TextView>(R.id.welcome)
        this.refresh = findViewById<Button>(R.id.Refresh)
        this.loading = findViewById<ProgressBar>(R.id.loading2)

        welcome.text = ""

        load(loginRepository)
        refresh.isEnabled = true
        refresh.setOnClickListener {
            load(loginRepository)
        }
    }

    private fun load(repo: LoginRepository) {
        welcome.text = ""

        loading.visibility = View.VISIBLE
        try {
            scope.launch {
                val result = repo.getUserProfile()
                MainScope().launch {
                    when (result) {
                        is Result.Success -> welcomeUser(result.data.displayName)
                        is Result.Error -> showError(result.exception.localizedMessage)
                    }
                }
            }
        } finally {
            loading.visibility = View.GONE
        }
    }

    private fun welcomeUser(name: String) {
        welcome.text = getString(R.string.welcome, name)
    }

    private fun showError(message: String?) {
        Toast.makeText (
            applicationContext,
            message ?: "Unknown",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}
