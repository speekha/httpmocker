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

class FileAccessor(
    private var fileHandle: File
) {

    constructor(path: String) : this(File(path))

    val name: String
        get() = fileHandle.name

    val parentFile: FileAccessor?
        get() = fileHandle.parentFile?.let { FileAccessor(it) }

    val absolutePath: String
        get() = fileHandle.absolutePath

    fun getFile(fileName: String): FileAccessor = FileAccessor(File(fileHandle, fileName))

    fun exists(): Boolean = fileHandle.exists()

    fun mkdir() {
        fileHandle.mkdir()
    }

    fun getReader(): StreamReader = StreamReader(FileInputStream(fileHandle))

    fun getWriter(): StreamWriter = StreamWriter(FileOutputStream(fileHandle))
}
