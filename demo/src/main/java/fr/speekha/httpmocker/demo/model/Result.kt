/*
 * Copyright 2019 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.demo.model

import fr.speekha.httpmocker.demo.model.Result.Companion.failure
import fr.speekha.httpmocker.demo.model.Result.Companion.success

class Result<T> private constructor(private val result: Any?) {

    val isFailure: Boolean get() = result is Failure
    val isSuccess: Boolean get() = result !is Failure

    fun get(): T =
        if (result is Failure) throw result.exception else result as? T ?: error(CAST_ERROR)

    fun getOrNull(): T? = result as? T

    inline fun getOrElse(default: () -> T): T = if (isFailure) default() else value

    fun exceptionOrNull(): Throwable? = (result as? Failure)?.exception

    companion object {
        fun <T> success(value: T): Result<T> = Result(value)
        fun <T> failure(exception: Throwable) = Result<T>(Failure(exception))
    }

    @PublishedApi
    internal val exception: Throwable
        get() = (result as? Failure)?.exception ?: error(CAST_ERROR)

    @PublishedApi
    internal val value: T
        get() {
            return result as? T ?: error(CAST_ERROR)
        }

    private class Failure(@JvmField val exception: Throwable)
}

private const val CAST_ERROR = "Unexpected value"

@SuppressWarnings("TooGenericExceptionCaught")
inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        success(block())
    } catch (e: Throwable) {
        failure(e)
    }

inline fun <T> Result<T>.onFailure(block: (Throwable) -> Unit): Result<T> = also {
    if (isFailure) block(exception)
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (isSuccess) block(value)
    return this
}
