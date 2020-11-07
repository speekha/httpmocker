/*
 * Copyright 2019-2020 David Blanc
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

package fr.speekha.httpmocker

import org.junit.jupiter.api.Assertions

inline fun <reified T : Throwable> assertThrows(message: String? = null, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        return (e as? T).takeIf { message == null || e.message == message } ?: Assertions.fail("Wrong exception: $e")
    }
    return Assertions.fail("No exception thrown")
}

const val url = "http://www.test.fr/path1?param=1"
