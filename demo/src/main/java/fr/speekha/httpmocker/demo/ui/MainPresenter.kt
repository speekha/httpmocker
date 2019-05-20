package fr.speekha.httpmocker.demo.ui

import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.*


class MainPresenter(
    private val apiService: GithubApiEndpoints,
    private val mocker: MockResponseInterceptor
) : MainContract.Presenter, CoroutineScope by MainScope() {

    override lateinit var view: MainContract.View

    override fun callService() {
        launch {
            try {
                val repos = loadReposAsync()
                view.setResult(repos.await())
            } catch (e: Throwable) {
                view.setError(e.message)
            }
        }
    }

    private suspend fun loadReposAsync(): Deferred<List<Repo>> = withContext(Dispatchers.IO) {
        apiService.listOrgRepos("kotlin")
    }

    override fun setMode(mode: Int) {
        when(mode) {
            1 -> mocker.enabled = true
            else -> mocker.enabled = false
        }
    }

    override fun stop() {
        coroutineContext.cancel()
    }
}