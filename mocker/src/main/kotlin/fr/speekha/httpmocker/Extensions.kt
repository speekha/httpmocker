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

package fr.speekha.httpmocker

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Reads the content of an input stream and returns it as a list of strings.
 * @return the data as a list, line by line
 */
fun InputStream.readAsStringList(): List<String> =
    bufferedReader().use { reader -> reader.readLines() }

/**
 * Reads the content of an input stream and returns it as a string.
 * @return the data as a single String
 */
fun InputStream.readAsString(): String = bufferedReader().use { reader -> reader.readText() }

inline fun <reified T : Any> T.getLogger(): Logger = LoggerFactory.getLogger(javaClass)
