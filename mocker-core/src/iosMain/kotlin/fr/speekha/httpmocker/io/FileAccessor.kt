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

actual class FileAccessor(
    private val filePath: String,
    private val fileDescriptor: Int
) {

    actual constructor(path: String) : this(path, 0)

    actual val name: String
        get() = filePath.drop(filePath.lastIndexOf('/'))

    actual val parentFile: FileAccessor?
        get() = TODO()

    actual val absolutePath: String
        get() = TODO()

    actual fun getChild(fileName: String): FileAccessor = TODO()

    actual fun exists(): Boolean = TODO()

    actual fun mkdir() {
        TODO()
    }

    actual fun getReader(): StreamReader = TODO()

    actual fun getWriter(): StreamWriter = TODO()
//
//    func getJsonContent(file: String) -> String {
//        guard
//        let fileUrl = Bundle(for: PulsarApiDefault.self).url(forResource: file, withExtension: "json"),
//        let data = try? Data(contentsOf: fileUrl),
//            let jsonContent = String(data: data, encoding: .utf8)
//            else {
//                fatalError("Mock \(file) not present")
//            }
//            return jsonContent
//        }
}
