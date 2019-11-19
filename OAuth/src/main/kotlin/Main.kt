import com.google.gson.Gson
import org.json.JSONObject
import spark.Spark
import spark.Spark.*
import java.util.*

object Main {
    data class UserProfile(val userId: String, val displayName: String)
    data class User(val profile: UserProfile, val password: String) {
        constructor(email: String, name: String, password: String): this(UserProfile(email, name), password) {}
    }

    val mClients = mapOf<UUID,String>(
        UUID.fromString("41515389-7EBE-466B-B532-394B7E9998D4")!! to "btENnDHAiXc8CgW54FXBtO3x9wnkEerepAl0vsim"
    )

    val mUsers = mapOf<String,User>(
        "brian@test.com" to User("brian@test.com", "Brian", password = "abc123")
    )

    val key = "Server Secret"

    fun guard(obj: Any?, status: Int = 400, message: String = "") {
        guard(obj != null, status, message)
    }

    fun guard(cond: Boolean, status: Int = 400, message: String = "") {
        if (!cond) halt(status, message)
    }

    @JvmStatic
    fun main(args: Array<String>): Unit {

        post("/oauth/token") { req, res ->
            val username = req.queryParams("username")
            guard(username, 400)

            val pass = req.queryParams("password")
            guard(pass, 400)

            val clientId = req.queryParams("client_id")
            guard(clientId, 400)

            val uuid = UUID.fromString(clientId)
            guard(uuid, 400)

            val secret = mClients[uuid]
            guard(secret, 401)

            val clientSecret = req.queryParams("client_secret")
            guard(clientSecret, 400)
            guard(clientSecret == secret)

            val user = mUsers[username]
            if (user != null) {
                guard(user.password == pass, 401)
            } else {
                Spark.halt(401)
            }

            val access_token = JWT.Builder()
                .sub(username)
                .ttl(30 * 60)
                .build(key)
                .toString()

            val refresh_token = JWT.Builder()
                .sub(username)
                .ttl(30 * 24 * 60 * 60)
                .build(key)
                .toString()

            res.header("Content-Type", "application/json")
            JSONObject()
                .put("access_token", access_token)
                .put("refresh_token", refresh_token)
                .put("token_type", "bearer")
                .put("expires_in", 30*60)
                .toString()
        }

        get("/user/:name") { req, res -> String
            val auth = req.headers("Authorization")
            guard(auth, 401)

            val token = auth.replace("Bearer ", "", true)
            guard(token, 401)

            val jwt = JWT.parse(token)
            guard(jwt.verify(key), 401)

            val subject = jwt.claim("sub")
            guard(subject, 401)

            val name = req.params("name")
            guard(name, 401)
            guard (name == subject, 401)

            val user = mUsers[name]
            if (user != null) {
                val jsn = Gson().toJson(user.profile);
                jsn
            } else {
                halt(400)
            }
        }
    }
}