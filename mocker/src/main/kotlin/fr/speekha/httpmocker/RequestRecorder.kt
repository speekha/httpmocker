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
import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal class RequestRecorder(
    private val mapper: Mapper,
    private val filingPolicy: FilingPolicy,
    private val rootFolder: File?,
    private val failOnError: Boolean
) {

    private val logger = getLogger()

    private val extensionMappings: Map<String, String> by lazy { loadExtensionMap() }

    fun saveFiles(record: CallRecord) {
        try {
            val requestFile = getRequestFilePath(record)
            val matchers = buildMatcherList(record, requestFile)
            saveRequestFile(requestFile, matchers)
            saveResponseBody(matchers, requestFile, record)
        } catch (e: Throwable) {
            logger.error("Error while writing scenario", e)
            if (failOnError) {
                throw e
            }
        }
    }

    private fun getRequestFilePath(record: CallRecord): File =
        File(rootFolder, filingPolicy.getPath(record.request)).also {
            logger.debug("Saving scenario file $it")
        }

    private fun buildMatcherList(record: CallRecord, requestFile: File): List<Matcher> =
        with(record) {
            val previousRecords: List<Matcher> = if (requestFile.exists())
                mapper.readMatches(requestFile).toMutableList()
            else emptyList()
            return previousRecords + buildMatcher(previousRecords, record)
        }

    private fun CallRecord.buildMatcher(previousRecords: List<Matcher>, record: CallRecord) =
        Matcher(
            request.toDescriptor(),
            response.toDescriptor(
                previousRecords.size,
                record.body
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { getExtension(response.body()?.contentType()) }
            )
        )

    private fun saveRequestFile(requestFile: File, matchers: List<Matcher>) =
        writeFile(requestFile) {
            mapper.writeValue(it, matchers)
        }

    private fun saveResponseBody(
        matchers: List<Matcher>,
        requestFile: File,
        record: CallRecord
    ) = matchers.last().response.bodyFile?.let { responseFile ->
        val storeFile = File(requestFile.parentFile, responseFile)
        record.body?.let { array ->
            saveBodyFile(storeFile, array)
        }
    }

    private fun saveBodyFile(storeFile: File, array: ByteArray) {
        logger.debug("Saving response body file ${storeFile.name}")
        writeFile(storeFile) {
            it.write(array)
        }
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
            ?.readAsStringList()
            ?.associate {
                val (extension, mimeType) = it.split("=")
                mimeType to extension
            } ?: mapOf()

    private fun getExtension(contentType: MediaType?) =
        extensionMappings[contentType.toString()] ?: ".txt"

    class CallRecord(
        val request: Request,
        val response: Response,
        val body: ByteArray?
    )
}

