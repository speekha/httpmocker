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

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.io.RequestWriter.CallRecord
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class Recorder(
    private val writer: RequestWriter
) {

    @SuppressWarnings("TooGenericExceptionCaught")
    fun recordCall(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        val record = try {
            response = chain.execute()
            convertCallResult(request, response)
        } catch (e: Throwable) {
            CallRecord(request.toGenericModel(), error = e)
        }

        writer.saveFiles(record)
        return proceedWithCallResult(record, response)
    }

    private fun convertCallResult(request: Request, response: Response): CallRecord {
        val body = response.body?.bytes()
        return CallRecord(request.toGenericModel(), response.toDescriptor(), body, response.getMediaType())
    }

    private fun Response.getMediaType(): MediaType? {
        val contentType = body?.contentType()
        return contentType?.run { MediaType(type, subtype) }
    }

    private fun proceedWithCallResult(record: CallRecord, response: Response?): Response =
        record.error?.let {
            throw it
        } ?: response?.copyResponse(record.body) ?: error("Response is null")
}
