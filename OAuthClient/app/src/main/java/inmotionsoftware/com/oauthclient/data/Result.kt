package inmotionsoftware.com.oauthclient.data

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Result<out T : Any> {

    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
        }
    }

    inline fun <F: Any> mapResult(lambda: (T) -> Result<F> ): Result<F>
        = when (this) {
            is Success -> lambda(data)
            is Error -> Result.Error(this.exception)
        }

    inline fun <F: Any> map(lambda: (T) -> F): Result<F>
        = this.mapResult { Result.Success(lambda(it)) }
}