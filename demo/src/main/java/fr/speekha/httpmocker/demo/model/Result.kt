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

sealed class Result<T : Any> {
    abstract fun getOrNull(): T?
}

class Success<T : Any>(val value: T) : Result<T>() {
    override fun getOrNull(): T? = value
}

class Failure<T : Any>(val error: Throwable) : Result<T>() {
    override fun getOrNull(): T? = null
}

@SuppressWarnings("TooGenericExceptionCaught")
inline fun <T : Any> resultOf(block: () -> T): Result<T> = try {
    Success(block())
} catch (e: Throwable) {
    Failure(e)
}

inline fun <T : Any> Result<T>.onFailure(block: (Throwable) -> Unit): Result<T> = also {
    if (it is Failure) block(it.error)
}

inline fun <T : Any> Result<T>.onSuccess(block: (T?) -> Unit): Result<T> = also {
    if (it is Success) block(it.value)
}
