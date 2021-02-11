/*
 * Copyright 2019-2021 David Blanc
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

package fr.speekha.httpmocker.client.ktor

import fr.speekha.httpmocker.assertThrows
import fr.speekha.httpmocker.client.HttpClientTester
import fr.speekha.httpmocker.client.RecordTests
import io.ktor.client.*
import io.ktor.client.statement.*
import org.junit.jupiter.api.DisplayName
import java.nio.channels.UnresolvedAddressException

@DisplayName("Record tests with Ktor")
class RecordTests :
    RecordTests<HttpResponse, HttpClient>(),
    HttpClientTester<HttpResponse, HttpClient> by KtorTests() {

    override suspend fun checkIoException(block: suspend () -> Unit): Throwable =
        assertThrows<UnresolvedAddressException> {
            block()
        }
}
