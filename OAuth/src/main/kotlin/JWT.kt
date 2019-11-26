import com.google.gson.Gson
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class JWT() {
    companion object {
        var logger: Logger = LoggerFactory.getLogger(JWT::class.java)
    }

    data class Header (
        val alg: String,
        val typ: String
    )

    internal var signature: String? = null
    internal var header: String? = null
    internal var body: String? = null
    internal var claims = JSONObject()

    /**
     * Convenience constructor. Provide the claims and the secret key
     */
    constructor(key: String, claims: JSONObject): this() {
        this.claims = claims

        val encoder = Base64.getEncoder()

        val body = encoder.encodeToString(
            claims.toString().toByteArray(Charsets.UTF_8)
        )

        val header = encoder.encodeToString(
            Gson()
            .toJson(Header("HS256", "JWT"))
            .toByteArray(Charsets.UTF_8)
        )

        this.body = body
        this.header = header
        this.signature = encoder.encodeToString(JWT.sign(key, header, body))
    }

    /**
     * Parse constructor. Provide the header body and signature
     */
    constructor(header: String, body: String, signature: String?): this() {
        this.header = header
        this.body = body
        this.signature = signature.orEmpty()

        val json = Base64.getDecoder().decode(body)
        val str = String(json, Charsets.UTF_8)
        this.claims = JSONObject(str)
    }

    fun claim(name: String): Any? {
        return if (this.claims.has(name)) this.claims.get(name) else null
    }

    class Builder {
        private val mClaims = JSONObject()

        fun iss(issuer: String): Builder {
            return addClaim("iss", issuer)
        }

        fun sub(subject: String): Builder {
            return addClaim("sub", subject)
        }

        fun aud(audience: String): Builder {
            return addClaim("aud", audience)
        }

        fun exp(date: Instant): Builder {
            return addClaim("exp", date)
        }

        fun nbf(notBefore: Instant = Instant.now()): Builder {
            return addClaim("nbf", notBefore)
        }

        fun iat(issuedAt: Instant = Instant.now()): Builder {
            return addClaim("iat", issuedAt)
        }

        fun jti(jwtID: String): Builder {
            return addClaim("jti", jwtID)
        }

        fun ttl(seconds: Long): Builder {
            val now = Instant.now()
            val exp = now.plusSeconds(seconds)
            return iat(now).exp(exp)
        }

        fun addClaim(claim: String, value: Date): Builder {
            return addClaim(claim, value.toInstant())
        }

        fun addClaim(claim: String, value: Instant): Builder {
            return addClaim(claim, value.epochSecond)
        }

        fun addClaim(claim: String, value: Any): Builder {
            mClaims.put(claim, value)
            return this
        }

        fun build(key: String): JWT {
            return JWT(key, mClaims)
        }
    }

    fun validate(key: String): JWT {
        if (!verify(key)) throw RuntimeException("invalid JWT")
        return this
    }

    override fun toString(): String {
        return "${header}.${body}.${signature}"
    }
}

private fun JWT.Companion.sign(key: String, header: String, body: String): ByteArray {
    val encoder = Base64.getEncoder()
    val h = encoder.encodeToString(header.toByteArray(Charsets.UTF_8))
    val b = encoder.encodeToString(body.toByteArray(Charsets.UTF_8))
    val contents = "${h}.${b}";

    val sha256HMAC = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
    sha256HMAC.init(secretKey)
    return sha256HMAC.doFinal(contents.toByteArray(Charsets.UTF_8))
}

fun JWT.verify(key: String): Boolean {
    val rawHeader = this.header ?: return false
    val body = this.body ?: return false

    val now = Instant.now()
    this.claim("nbf")?.let {
        val date = Instant.ofEpochSecond((it as Number).toLong())
        JWT.logger.debug("nbf now: ${now} expiration: ${date} delta: ${ (now.epochSecond - date.epochSecond) }")
        if (now.isBefore(date)) {
            return false
        }
    }

    this.claim("exp")?.let {
        val date = Instant.ofEpochSecond((it as Number).toLong())
        JWT.logger.debug("exp now: ${now} expiration: ${date} delta: ${ (now.epochSecond - date.epochSecond) }")
        if (now.isAfter(date)) {
            return false
        }
    }

    val bytes = Base64.getDecoder().decode(rawHeader)
    val jbytes = String(bytes, Charsets.UTF_8)
    val h = Gson().fromJson(jbytes, JWT.Header::class.java) ?: return false
    when (h.alg) {
        "HS256" -> {
            val actual = JWT.sign(key, rawHeader, body)
            val expected = Base64.getDecoder().decode(this.signature)
            return Arrays.equals(actual, expected)
        }
        // unsupported algorithm
        else -> return false
    }
}

fun JWT.Companion.parse(token: String): JWT {
    // <header>.<body>.<sig>
    val tokens = token.split('.')
    if (tokens.size != 3)
        throw java.lang.RuntimeException("JWT is malformed")
    return JWT(tokens[0], tokens[1], tokens[2])
}