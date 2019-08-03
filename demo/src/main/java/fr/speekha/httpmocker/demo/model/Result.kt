package fr.speekha.httpmocker.demo.model

class Result<T> private constructor(private val result: Any?) {

    val isFailure: Boolean get() = result is Failure
    val isSuccess: Boolean get() = result !is Failure

    fun get(): T =
        if (result is Failure) throw result.exception
        else result as T

    fun getOrNull(): T? =
        if (result is Failure) null
        else result as T

    inline fun getOrElse(default: () -> T): T =
        if (isFailure) default()
        else value

    fun exceptionOrNull(): Throwable? =
        if (result is Failure) result.exception
        else null

    companion object {
        fun <T> success(value: T): Result<T> = Result(value)
        fun <T> failure(exception: Throwable) = Result<T>(Failure(exception))
    }

    @PublishedApi
    internal val exception: Throwable
        get() = (result as Failure).exception

    @PublishedApi
    internal val value: T
        get() = result as T

    private class Failure(@JvmField val exception: Throwable)
}

inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }

inline fun <U, T> Result<T>.map(block: (T) -> U): Result<U> =
    if (isFailure) this as Result<U>
    else resultOf { block(value) }

inline fun <U, T : U> Result<T>.handle(block: (Throwable) -> U): Result<U> =
    if (isFailure) resultOf { block(exception) }
    else this as Result<U>

inline fun <T> Result<T>.onFailure(block: (Throwable) -> Unit): Result<T> {
    if (isFailure) block(exception)
    return this
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (isSuccess) block(value)
    return this
}
