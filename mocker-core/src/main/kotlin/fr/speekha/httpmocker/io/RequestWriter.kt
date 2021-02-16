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

package fr.speekha.httpmocker.io

import fr.speekha.httpmocker.ClassloaderUtils
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.NetworkError
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.model.toDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.serialization.Mapper
import fr.speekha.httpmocker.serialization.readMatches
import fr.speekha.httpmocker.serialization.writeValue

class RequestWriter(
    private val mapper: Mapper,
    private val filingPolicy: FilingPolicy,
    private val rootFolder: FileAccessor,
    private val failOnError: Boolean
) {

    private val logger = getLogger()

    private val extensionMappings: Map<String, String> by lazy { loadExtensionMap() }

    @Suppress("TooGenericExceptionCaught")
    fun saveFiles(record: CallRecord) {
        try {
            val requestFile = getRequestFilePath(record)
            val matchers = buildMatcherList(record, requestFile)
            saveRequestFile(requestFile, matchers)
            saveResponseBody(matchers, requestFile, record)
        } catch (e: Exception) {
            logger.error("Error while writing scenario", e)
            if (failOnError) {
                throw e
            }
        }
    }

    private fun getRequestFilePath(record: CallRecord): FileAccessor =
        rootFolder.getFile(filingPolicy.getPath(record.request)).also {
            logger.debug("Saving scenario file $it")
        }

    private fun buildMatcherList(record: CallRecord, requestFile: FileAccessor): List<Matcher> =
        with(record) {
            val previousRecords: List<Matcher> = if (requestFile.exists()) {
                mapper.readMatches(requestFile) ?: emptyList()
            } else {
                emptyList()
            }
            return previousRecords + buildMatcher(previousRecords.size)
        }

    private fun CallRecord.buildMatcher(offset: Int) = Matcher(
        request.toDescriptor(),
        response?.updateBodyFile(offset, this),
        error?.toDescriptor()
    )

    private fun ResponseDescriptor.updateBodyFile(
        offset: Int,
        record: CallRecord
    ): ResponseDescriptor =
        record.getExtension()?.let { ext -> copy(bodyFile = bodyFile + offset + ext) }
            ?: this.copy(bodyFile = null)

    private fun CallRecord.getExtension(): String? =
        contentType?.getExtension()?.takeIf { body?.isNotEmpty() == true }

    private fun Throwable.toDescriptor() = NetworkError(this::class.qualifiedName ?: "", message)

    private fun saveRequestFile(requestFile: FileAccessor, matchers: List<Matcher>) =
        writeFile(requestFile) {
            mapper.writeValue(it, matchers)
        }

    private fun saveResponseBody(
        matchers: List<Matcher>,
        requestFile: FileAccessor,
        record: CallRecord
    ) = matchers.last().response?.bodyFile?.let { responseFile ->
        val storeFile = (requestFile.parentFile ?: rootFolder).getFile(responseFile)
        record.body?.let { array ->
            saveBodyFile(storeFile, array)
        }
    }

    private fun saveBodyFile(storeFile: FileAccessor, array: ByteArray) {
        logger.debug("Saving response body file ${storeFile.name}")
        writeFile(storeFile) {
            it.write(array)
        }
    }

    private fun writeFile(file: FileAccessor, block: (StreamWriter) -> Unit) {
        createParent(file.parentFile)
        file.getWriter().use {
            block(it)
        }
    }

    private fun createParent(file: FileAccessor?) {
        when {
            file?.parentFile?.exists() == false -> file.apply {
                createParent(parentFile)
                mkdir()
            }
            file?.exists() == false -> file.mkdir()
        }
    }

    private fun loadExtensionMap(): Map<String, String> = ClassloaderUtils.loadExtensionMap()

    private fun MediaType.getExtension() = extensionMappings["$type/$subtype"] ?: ".txt"

    class CallRecord(
        val request: HttpRequest,
        val response: ResponseDescriptor? = null,
        val body: ByteArray? = null,
        val contentType: MediaType? = null,
        val error: Throwable? = null
    )
}
