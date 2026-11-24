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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosFileAccessorTest {

    @Test
    fun `mkdir creates nested directories and succeeds when directory already exists`() =
        withTemporaryFolder("nested") { folder ->
            val nested = folder.getFile("first").getFile("second")

            nested.mkdir()
            nested.mkdir()

            assertTrue(nested.exists())
        }

    @Test
    fun `mkdir throws IOException when path is an existing file`() = withTemporaryFolder("file-collision") { folder ->
        val file = folder.getFile("not-a-directory")
        file.writeText("content")

        val error = assertFailsWith<IOException> { file.mkdir() }

        assertTrue(error.message?.contains(file.absolutePath) == true)
    }

    @Test
    fun `getFile builds nested paths with name and parent`() = withTemporaryFolder("paths") { folder ->
        val file = folder.getFile("first").getFile("second").getFile("file.json")

        assertEquals("file.json", file.name)
        assertEquals("${folder.absolutePath}/first/second/file.json", file.absolutePath)
        assertEquals("${folder.absolutePath}/first/second", file.parentFile?.absolutePath)
    }

    @Test
    fun `getFile accepts a relative path containing separators`() = withTemporaryFolder("relative") { folder ->
        val file = folder.getFile("first/second/file.json")

        assertEquals("${folder.absolutePath}/first/second/file.json", file.absolutePath)
    }

    @Test
    fun `written content can be read back`() = withTemporaryFolder("round-trip") { folder ->
        val file = folder.getFile("file.txt")

        file.writeText("simple content")

        assertTrue(file.exists())
        assertEquals("simple content", file.getReader().readAsString())
    }

    @Test
    fun `multibyte UTF-8 content survives a round trip`() = withTemporaryFolder("utf8") { folder ->
        val file = folder.getFile("file.txt")
        val content = "Héllo wörld – 日本語 – 🚀"

        file.writeText(content)

        assertEquals(content, file.getReader().readAsString())
    }

    @Test
    fun `empty file is read as an empty string`() = withTemporaryFolder("empty") { folder ->
        val file = folder.getFile("empty.txt")

        file.writeText("")

        assertTrue(file.exists())
        assertEquals("", file.getReader().readAsString())
    }

    @Test
    fun `writing replaces previous content`() = withTemporaryFolder("overwrite") { folder ->
        val file = folder.getFile("file.txt")

        file.writeText("first and longer content")
        file.writeText("second")

        assertEquals("second", file.getReader().readAsString())
    }

    @Test
    fun `reading a missing file throws IOException`() = withTemporaryFolder("missing") { folder ->
        val file = folder.getFile("missing.txt")

        assertFalse(file.exists())
        assertFailsWith<IOException> { file.getReader() }
    }

    @Test
    fun `writing into a missing folder throws IOException`() = withTemporaryFolder("missing-parent") { folder ->
        val file = folder.getFile("missing-folder").getFile("file.txt")

        assertFailsWith<IOException> { file.writeText("content") }
        assertFalse(file.exists())
    }
}
