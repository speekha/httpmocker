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

actual class FileAccessor(
    private val fileHandle: File
) {

    actual constructor(path: String) : this(File(path))

    actual val name: String
        get() = fileHandle.name

    actual val parentFile: FileAccessor
        get() = FileAccessor(fileHandle.parentFile)

    actual val absolutePath: String
        get() = fileHandle.absolutePath

    actual fun getFile(fileName: String): FileAccessor = FileAccessor(File(fileHandle, fileName))

    actual fun exists(): Boolean = fileHandle.exists()

    actual fun mkdir() {
        fileHandle.mkdir()
    }

    actual fun getReader(): StreamReader = StreamReader(FileInputStream(fileHandle))

    actual fun getWriter(): StreamWriter = StreamWriter(FileOutputStream(fileHandle))
}
