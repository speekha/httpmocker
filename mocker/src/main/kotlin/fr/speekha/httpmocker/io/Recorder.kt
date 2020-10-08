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

package fr.speekha.httpmocker.io

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response

internal class Recorder(
    private val writer: RequestWriter
) {

    fun recordCall(chain: Interceptor.Chain): Response {
        val record = convertCallResult(chain)
        writer.saveFiles(record)
        return proceedWithCallResult(record)
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun convertCallResult(chain: Interceptor.Chain): CallRecord = try {
        val response = chain.execute()
        val body = response.body?.bytes()
        val contentType = response.body?.contentType()
        CallRecord(chain.request(), response, body, contentType)
    } catch (e: Throwable) {
        CallRecord(chain.request(), error = e)
    }

    private fun proceedWithCallResult(record: CallRecord): Response =
        if (record.error != null) {
            throw record.error
        } else {
            record.response?.copyResponse(record.body) ?: error("Response is null")
        }

    internal class CallRecord(
        val request: Request,
        val response: Response? = null,
        val body: ByteArray? = null,
        val contentType: MediaType? = null,
        val error: Throwable? = null
    )
}
