import com.google.gson.Gson
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark
import spark.Spark.*
import java.util.*

object Main {
    private const val tokenTTL: Long = 15;
    private const val refreshTTL: Long = 5 * tokenTTL;

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

    fun guard(obj: Any?, status: Int = 400, message: String = ""): Boolean {
        return guard(obj != null, status, message)
    }

    fun guard(cond: Boolean, status: Int = 400, message: String = ""): Boolean {
        if (!cond) halt(status, message)
        return true
    }

    private fun validateClient(req: Request, res: Response) {

        val clientId = req.queryParams("client_id")
        guard(clientId, 400)

        val uuid = UUID.fromString(clientId)
        guard(uuid, 400)

        val secret = mClients[uuid]
        guard(secret, 401)

        val clientSecret = req.queryParams("client_secret")
        guard(clientSecret, 400)
        guard(clientSecret == secret)
    }

    private fun grantPassword(req: Request, res: Response): String {
        validateClient(req, res)

        val _username = req.queryParams("username")
        guard(_username, 400)
        val username = _username.toLowerCase()

        val pass = req.queryParams("password")
        guard(pass, 400)

        val user = mUsers[username]
        if (user != null) {
            guard(user.password == pass, 401)
        } else {
            Spark.halt(401)
        }

        val refresh_token = JWT.Builder()
            .sub(username)
            .ttl(refreshTTL)
            .build(key)
            .toString()

        val access_token = JWT.Builder()
            .sub(username)
            .ttl(tokenTTL)
            .build(key)
            .toString()

        res.header("Content-Type", "application/json")
        return JSONObject()
            .put("access_token", access_token)
            .put("refresh_token", refresh_token)
            .put("token_type", "bearer")
            .put("expires_in", tokenTTL)
            .toString()
    }

    private fun grantRefresh(req: Request, res: Response): String {
        validateClient(req, res)

        val refreshToken = req.queryParams("refresh_token")
        guard(refreshToken, 400)

        val jwt = JWT.parse(refreshToken)
        guard(jwt.verify(key), 401)

        val sub = jwt.claim("sub") as String
        guard(sub, 401)

        val access_token = JWT.Builder()
            .sub(sub)
            .ttl(tokenTTL)
            .build(key)
            .toString()


        res.header("Content-Type", "application/json")
        return JSONObject()
            .put("access_token", access_token)
            .put("refresh_token", refreshToken)
            .put("token_type", "bearer")
            .put("expires_in", tokenTTL)
            .toString()
    }

    fun getUser(req: Request, res: Response): String? {
        val auth = req.headers("Authorization")
        guard(auth, 401)

        val token = auth.replace("Bearer ", "", true)
        guard(token, 401)

        val jwt = JWT.parse(token)
        guard(jwt.verify(key), 401)

        val subject = jwt.claim("sub")
        guard(subject, 401)

        val user = mUsers[subject]
        if (user != null) {
            val jsn = Gson().toJson(user.profile)
            return jsn
        }
        halt(400)
        return null
    }

    fun postToken(req: Request, res: Response): String? {
        val grantType = req.queryParams("grant_type")
        guard(grantType, 400)
        return when (grantType) {
            "password" -> grantPassword(req, res)
            "refresh_token" -> grantRefresh(req, res)
            else -> {
                halt(400)
                null
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>): Unit {
        post("/oauth/token", this::postToken)
        get("/user", this::getUser)
    }
}