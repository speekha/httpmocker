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
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosFileAccessorTest {

    private val fileManager = NSFileManager.defaultManager()

    @Test
    fun `mkdir creates nested directories and succeeds when directory already exists`() {
        val directory = createTemporaryDirectory("nested")
        val nested = directory.getFile("first").getFile("second")

        try {
            nested.mkdir()
            nested.mkdir()

            assertTrue(nested.exists())
        } finally {
            fileManager.removeItemAtPath(directory.absolutePath, error = null)
        }
    }

    @Test
    fun `mkdir throws IOException when path is an existing file`() {
        val directory = createTemporaryDirectory("file-collision")
        val file = directory.getFile("not-a-directory")

        try {
            file.getWriter().use { it.write(byteArrayOf(1)) }

            val error = assertFailsWith<IOException> { file.mkdir() }

            assertTrue(error.message?.contains(file.absolutePath) ?: false)
        } finally {
            fileManager.removeItemAtPath(directory.absolutePath, error = null)
        }
    }

    private fun createTemporaryDirectory(name: String): FileAccessor {
        val directory = createFileAccessor("${NSTemporaryDirectory()}httpmocker-$name-${NSUUID().UUIDString}")
        fileManager.removeItemAtPath(directory.absolutePath, error = null)
        directory.mkdir()
        return directory
    }
}
