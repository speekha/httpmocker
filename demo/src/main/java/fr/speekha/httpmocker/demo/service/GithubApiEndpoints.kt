package fr.speekha.httpmocker.demo.service

import fr.speekha.httpmocker.demo.model.Repo
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path


interface GithubApiEndpoints {

    @GET("orgs/{org}/repos?per_page=100")
    fun listOrgRepos(@Path("org") org: String): Deferred<List<Repo>>
}