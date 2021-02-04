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
