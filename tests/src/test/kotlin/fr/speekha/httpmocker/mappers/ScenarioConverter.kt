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

package fr.speekha.httpmocker.mappers

import fr.speekha.httpmocker.Mapper
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.readMatches
import fr.speekha.httpmocker.sax.SaxMapper
import fr.speekha.httpmocker.writeValue
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {
    val reader = JacksonMapper()
    val writer = SaxMapper()

    val folder = File(args[0])
    convertFolder(folder, reader, writer)
}

private fun convertFolder(
    folder: File,
    reader: JacksonMapper,
    writer: SaxMapper
) {
    folder.listFiles { file, name ->
        name.endsWith(".json") || File(file, name).isDirectory
    }?.forEach {
        if (it.isDirectory) {
            convertFolder(it, reader, writer)
        } else {
            convertFile(it, reader, writer) { file ->
                file.absolutePath.replace(".json", ".xml")
            }
        }
    }
}

private fun convertFile(file: File, reader: Mapper, writer: Mapper, rename: (File) -> String) {
    reader.readMatches(file)?.let { scenario ->
        val dest = File(rename(file))
        if (!dest.exists()) {
            writer.writeValue(FileOutputStream(dest), scenario)
        }
    }
}
