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

package fr.speekha.httpmocker.demo.service

import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import io.ktor.client.*
import io.ktor.client.request.*

class GithubEndpointWithKtor(
    private val client: HttpClient
) : GithubApiEndpoints {

    private val baseUrl = "https://api.github.com"

    override suspend fun listRepositoriesForOrganisation(org: String): List<Repo> =
        client.get("$baseUrl/orgs/$org/repos?per_page=10")

    override suspend fun listContributorsForRepository(owner: String, repo: String): List<User> =
        client.get("$baseUrl/repos/$owner/$repo/contributors?per_page=10")
}
