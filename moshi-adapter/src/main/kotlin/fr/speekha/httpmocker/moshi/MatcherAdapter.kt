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

package fr.speekha.httpmocker.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.moshi.model.Matcher as JsonMatcher

internal class MatcherAdapter {
    @FromJson
    fun matcherFromJson(matcher: JsonMatcher): Matcher = Matcher(
        matcher.request.fromJson(),
        matcher.response?.fromJson(),
        matcher.error?.fromJson()
    )

    @ToJson
    fun matcherToJson(matcher: Matcher): JsonMatcher = JsonMatcher(
        matcher.request.toJson(),
        matcher.response?.toJson(),
        matcher.error?.toJson()
    )
}
