/*
 * Copyright 2019-2026 David Blanc
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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
class IosStreamReader(
    private val data: NSData
) : StreamReader {
    override fun readAsString(): String {
        if (data.length == 0UL) return ""
        val bytes = data.bytes?.readBytes(data.length.toInt())
            ?: throw IOException("Failed to read data bytes")
        return bytes.decodeToString()
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosStreamWriter(
    private val filePath: String
) : StreamWriter {
    override fun write(array: ByteArray) {
        val nsData = array.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), array.size.toULong())
        }
        val success = NSFileManager.defaultManager()
            .createFileAtPath(filePath, contents = nsData, attributes = null)
        if (!success) {
            throw IOException("Failed to write file: $filePath")
        }
    }

    override fun <R : Any> use(block: (StreamWriter) -> R): R = block(this)
}
