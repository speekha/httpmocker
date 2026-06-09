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
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

actual fun createFileAccessor(path: String) : FileAccessor = IosFileAccessor(path)

class IosFileAccessor(path: String) : FileAccessor {

    private val fileManager = NSFileManager.defaultManager()
    private val url = NSURL(fileURLWithPath = path)

    override val name: String
        get() = url.lastPathComponent ?: ""

    override val parentFile: IosFileAccessor?
        get() = url.URLByDeletingLastPathComponent?.path?.let { IosFileAccessor(it) }

    override val absolutePath: String
        get() = url.path ?: ""

    override fun getFile(fileName: String): IosFileAccessor {
        val childUrl = url.URLByAppendingPathComponent(fileName)
        return IosFileAccessor(childUrl?.path ?: "")
    }

    override fun exists(): Boolean = fileManager.fileExistsAtPath(absolutePath)

    override fun mkdir() {
        @OptIn(ExperimentalForeignApi::class)
        fileManager.createDirectoryAtPath(
            absolutePath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    override fun getReader(): StreamReader {
        val data = fileManager.contentsAtPath(absolutePath)
            ?: throw IOException("Unable to read file: $absolutePath")
        return IosStreamReader(data)
    }

    override fun getWriter(): IosStreamWriter = IosStreamWriter(absolutePath)
}
