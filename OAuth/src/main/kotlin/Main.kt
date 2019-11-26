import com.google.gson.Gson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Spark
import spark.Spark.*
import java.util.*

object Main {
    var logger: Logger = LoggerFactory.getLogger(Main::class.java)

    private const val tokenTTL: Long = 15;
    private const val refreshTTL: Long = 3 * tokenTTL;

    private val users = UserDatasource()
    private val clients = ClientDatasource()

    val key = "Server Secret"


    private fun validateClient(req: Request, res: Response) {

        val clientId = req.queryParams("client_id")
        guard(clientId, 400)

        val uuid = UUID.fromString(clientId)
        guard(uuid, 400)

        val secret = clients.getSecret(uuid)
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

        val user = users.authenticate(username, pass)
        guard(user, 401)

        val refreshToken = JWT.Builder()
            .sub(username)
            .ttl(refreshTTL)
            .build(key)
            .toString()

        val accessToken = JWT.Builder()
            .sub(username)
            .ttl(tokenTTL)
            .build(key)
            .toString()

        res.header("Content-Type", "application/json")
        return OAuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = tokenTTL
        ).toJson()
    }

    private fun grantRefresh(req: Request, res: Response): String {
        validateClient(req, res)

        val refreshToken = req.queryParams("refresh_token")
        guard(refreshToken, 400)

        val jwt = JWT.parse(refreshToken)
        guard(jwt.verify(key), 401)

        val sub = jwt.claim("sub") as String
        guard(sub, 401)

        val accessToken = JWT.Builder()
            .sub(sub)
            .ttl(tokenTTL)
            .build(key)
            .toString()


        res.header("Content-Type", "application/json")
        return OAuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = tokenTTL
        ).toJson()
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

        val user = users.getUser(subject as String)
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

        Spark.after("*") { req, resp ->
            logger.debug("request: ${ req.uri() } status: ${resp.status()}")
        }
    }
}