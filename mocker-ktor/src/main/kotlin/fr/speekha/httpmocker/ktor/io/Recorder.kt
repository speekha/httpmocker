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

package fr.speekha.httpmocker.ktor.io

import fr.speekha.httpmocker.io.RequestWriter
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

class Recorder(
    private val writer: RequestWriter,
    private val executor: suspend (HttpRequestData) -> HttpResponseData
) {

    suspend fun executeAndRecordCall(request: HttpRequestData): HttpResponseData {
        val (response, record) = executeCall(request)
        writer.saveFiles(record)
        return proceedWithCallResult(record, response)
    }

    private fun proceedWithCallResult(record: RequestWriter.CallRecord, response: HttpResponseData?): HttpResponseData =
        record.error?.let { throw it } ?: response?.withBody(record.body) ?: error("Response is null")

    @Suppress("TooGenericExceptionCaught")
    private suspend fun executeCall(request: HttpRequestData): Pair<HttpResponseData?, RequestWriter.CallRecord> {
        var response: HttpResponseData? = null
        val callRecord = try {
            response = executor(request)
            convertCallResult(request, response)
        } catch (e: Throwable) {
            RequestWriter.CallRecord(request.toModel(), error = e)
        }
        return response to callRecord
    }

    private suspend fun convertCallResult(
        request: HttpRequestData,
        response: HttpResponseData
    ): RequestWriter.CallRecord {
        val body = response.readBody()
        return RequestWriter.CallRecord(
            request = request.toModel(),
            response = response.toDescriptor(request.url),
            body = body,
            contentType = response.getMediaType()
        )
    }
}
