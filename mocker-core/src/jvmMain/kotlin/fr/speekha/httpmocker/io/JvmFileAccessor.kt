/*
 *  Copyright 2019-2021 David Blanc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package fr.speekha.httpmocker.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual fun createFileAccessor(path: String): FileAccessor = JvmFileAccessor(path)

fun createFileAccessor(fileHandle: File): FileAccessor = JvmFileAccessor(fileHandle)

class JvmFileAccessor(
    private val fileHandle: File
) : FileAccessor {

    constructor(path: String) : this(File(path))

    override val name: String
        get() = fileHandle.name

    override val parentFile: FileAccessor?
        get() = fileHandle.parentFile?.let { JvmFileAccessor(it) }

    override val absolutePath: String
        get() = fileHandle.absolutePath

    override fun getFile(fileName: String): FileAccessor = JvmFileAccessor(File(fileHandle, fileName))

    override fun exists(): Boolean = fileHandle.exists()

    override fun mkdir() {
        fileHandle.mkdir()
    }

    override fun getReader(): StreamReader = FileInputStream(fileHandle).asReader()

    override fun getWriter(): StreamWriter = FileOutputStream(fileHandle).asWriter()
}
