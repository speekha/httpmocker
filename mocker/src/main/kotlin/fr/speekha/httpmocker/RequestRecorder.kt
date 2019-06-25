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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.Matcher
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class RequestRecorder(
    private val mapper: Mapper
) {

    private val extensionMappings: Map<String, String> by lazy { loadExtensionMap() }

    fun saveFiles(record: CallRecord) = try {
        val matchers = createMatcher(record)
        saveRequestFile(record.requestFile, matchers)
        matchers.last().response.bodyFile?.let { responseFile ->
            saveResponseBody(File(record.requestFile.parentFile, responseFile), record.body)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    private fun createMatcher(record: CallRecord): List<Matcher> = with(record) {
        val previousRecords: List<Matcher> = if (requestFile.exists())
            mapper.readMatches(requestFile).toMutableList()
        else emptyList()
        return previousRecords + Matcher(
            request.toDescriptor(),
            response.toDescriptor(previousRecords.size, getExtension(response.body()?.contentType()))
        )
    }

    private fun saveRequestFile(requestFile: File, matchers: List<Matcher>) = writeFile(requestFile) {
        mapper.writeValue(it, matchers)
    }

    private fun saveResponseBody(storeFile: File, body: ByteArray?) = writeFile(storeFile) {
        it.write(body)
    }

    private fun writeFile(file: File, block: (OutputStream) -> Unit) {
        createParent(file.parentFile)
        FileOutputStream(file).use {
            block(it)
        }
    }

    private fun createParent(file: File?) {
        when {
            file?.parentFile?.exists() == false -> file.apply {
                createParent(parentFile)
                mkdir()
            }
            file?.exists() == false -> file.mkdir()
        }
    }

    private fun loadExtensionMap(): Map<String, String> =
        javaClass.classLoader.getResourceAsStream("fr/speekha/httpmocker/resources/mimetypes")
            .readAsStringList()
            .associate {
                val (extension, mimeType) = it.split("=")
                mimeType to extension
            }

    private fun getExtension(contentType: MediaType?) = extensionMappings[contentType.toString()] ?: ".txt"

    class CallRecord(
        val request: Request,
        val response: Response,
        val body: ByteArray?,
        val requestFile: File
    )
}

