package fr.speekha.httpmocker.demo.service

import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.User
import retrofit2.http.GET
import retrofit2.http.Path


interface GithubApiEndpoints {

    @GET("orgs/{org}/repos?per_page=10")
    suspend fun listRepositoriesForOrganisation(@Path("org") org: String): List<Repo>

    @GET("repos/{owner}/{repo}/contributors?per_page=10")
    suspend fun listContributorsForRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<User>
}