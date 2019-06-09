package fr.speekha.httpmocker.demo.ui

import android.util.Log
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainPresenter(
    private val view: MainContract.View,
    private val apiService: GithubApiEndpoints,
    private val mocker: MockResponseInterceptor
) : MainContract.Presenter, CoroutineScope by MainScope() {

    override fun callService() {
        launch {
            try {
                val org = "kotlin"
                val repos = loadReposAsync(org)
                    .map {
                        val contributor = loadTopContributor(org, it.name)?.firstOrNull()
                        it.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
                    }
                view.setResult(repos)
            } catch (e: Throwable) {
                view.setError(e.message)
            }
        }
    }

    private suspend fun loadReposAsync(org: String) = withContext(Dispatchers.IO) {
        apiService.listRepositoriesForOrganisation(org)
    }

    private suspend fun loadTopContributor(org: String, repo: String) = withContext(Dispatchers.IO) {
        try {
            apiService.listContributorsForRepository(org, repo)
        } catch (e: Throwable) {
            Log.e("Presenter", e.message, e)
            null
        }
    }

    override fun setMode(mode: Int) {
        mocker.mode = MockResponseInterceptor.MODE.values()[mode]
        if (mocker.mode == MockResponseInterceptor.MODE.RECORD) {
            view.checkPermission()
        }
        view.updateStorageLabel(mocker.mode == MockResponseInterceptor.MODE.RECORD)
    }

    override fun stop() {
        coroutineContext.cancel()
    }
}