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

package fr.speekha.httpmocker.demo.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class User
@JsonCreator
constructor(

    @JsonProperty("login")
    val login: String? = null,

    @JsonProperty("id")
    val id: Int? = null,

    @JsonProperty("avatar_url")
    val avatarUrl: String? = null,

    @JsonProperty("url")
    val url: String? = null,

    @JsonProperty("bio")
    val bio: Any? = null,

    @JsonProperty("public_repos")
    val publicRepos: Int? = null,

    @JsonProperty("contributions")
    val contributions: Int = 0
)