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

package fr.speekha.httpmocker.custom

const val WRONG_START_OF_OBJECT_ERROR = "No object starts here: "
const val NO_FIELD_ID_ERROR = "No field starts here: "
const val WRONG_END_OF_OBJECT_ERROR = "Object is not entirely processed: "
const val WRONG_START_OF_LIST_ERROR = "No list starts here: "
const val WRONG_END_OF_LIST_ERROR = "List is not entirely processed: "
const val WRONG_START_OF_STRING_ERROR = "No string starts here"
const val WRONG_START_OF_STRING_FIELD_ERROR = "Not ready to read a string value for a field: "
const val INVALID_NUMBER_ERROR = "Invalid numeric value: "
const val INVALID_TOKEN_ERROR = "Invalid token value: "
const val INVALID_BOOLEAN_ERROR = "Invalid boolean value: "
const val NO_MORE_TOKEN_ERROR = "No more token available: "

internal val numericPattern = Regex("\\d[\\d ]*")
internal val alphanumericPattern = Regex("[^,}\\]\\s]+")
internal val stringPattern =
    Regex("(\"((?=\\\\)\\\\(\"|/|\\\\|b|f|n|r|t|u[0-9a-f]{4})|[^\\\\\"]*)*\")|null")
internal const val DEFAULT_TRUCATE_LENGTH = 10

internal const val ELLIPSIS = "..."
internal const val OPENING_BRACE = "{\n"
internal const val COMMA = ",\n"
