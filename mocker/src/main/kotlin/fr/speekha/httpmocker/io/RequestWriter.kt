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

import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.io.Recorder.CallRecord
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.serialization.readMatches
import fr.speekha.httpmocker.serialization.writeValue
import okhttp3.MediaType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal class RequestWriter(
    private val mapper: Mapper,
    private val filingPolicy: FilingPolicy,
    private val rootFolder: File?,
    private val failOnError: Boolean
) {

    private val logger = getLogger()

    private val extensionMappings: Map<String, String> by lazy { loadExtensionMap() }

    @SuppressWarnings("TooGenericExceptionCaught")
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
            val previousRecords: List<Matcher> = if (requestFile.exists()) {
                mapper.readMatches(requestFile) ?: emptyList()
            } else {
                emptyList()
            }
            return previousRecords + buildMatcher(previousRecords)
        }

    private fun CallRecord.buildMatcher(previousRecords: List<Matcher>) =
        Matcher(
            request.toDescriptor(),
            response?.toDescriptor(
                previousRecords.size,
                getExtension()
            ),
            error?.toDescriptor()
        )

    private fun CallRecord.getExtension(): String? =
        contentType?.getExtension()?.takeIf { body?.isNotEmpty() == true }

    private fun Throwable.toDescriptor() = NetworkError(javaClass.canonicalName, message)

    private fun saveRequestFile(requestFile: File, matchers: List<Matcher>) =
        writeFile(requestFile) {
            mapper.writeValue(it, matchers)
        }

    private fun saveResponseBody(
        matchers: List<Matcher>,
        requestFile: File,
        record: CallRecord
    ) = matchers.last().response?.bodyFile?.let { responseFile ->
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

    private fun MediaType.getExtension() = extensionMappings["${type()}/${subtype()}"] ?: ".txt"
}
