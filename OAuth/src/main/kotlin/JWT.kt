import com.google.gson.Gson
import org.json.JSONObject
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

class JWT() {
    data class Header (
        val alg: String,
        val typ: String
    )

    internal var signature: String? = null
    internal var header: String? = null
    internal var body: String? = null
    internal var claims = JSONObject()

    companion object {
    }

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

    val now = Instant.now()

    this.claim("nbf")?.let {
        val date = Instant.ofEpochSecond((it as Number).toLong())
        if (now.isBefore(date)) {
            return false
        }
    }

    this.claim("exp")?.let {
        val date = Instant.ofEpochSecond((it as Number).toLong())
        if (now.isAfter(date)) {
            return false
        }
    }

    val rawHeader = this.header
    if (rawHeader == null)  return false

    val body = this.body
    if (body == null) return false

    val bytes = Base64.getDecoder().decode(rawHeader)

    val h = Gson().fromJson(String(bytes, Charsets.UTF_8), JWT.Header::class.java)
    if (h == null) return false

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
    val tokens = token.split('.')
    if (tokens.size != 3)
        throw java.lang.RuntimeException("JWT is malformed")
    return JWT(tokens[0], tokens[1], tokens[2])
}