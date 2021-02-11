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

package fr.speekha.httpmocker.demo.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.onFailure
import fr.speekha.httpmocker.demo.model.onSuccess
import fr.speekha.httpmocker.demo.model.resultOf
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val apiService: GithubApiEndpoints,
    private val mocker: MockerWrapper
) : ViewModel() {

    private val data = MutableLiveData<Data>()
    private val state = MutableLiveData<State>()

    fun getData(): LiveData<Data> = data
    fun getState(): LiveData<State> = state

    fun callService() = viewModelScope.launch {
        data.postValue(Data.Loading)
        resultOf {
            loadRepos("kotlin")
        } onSuccess { repos ->
            repos?.map { repo ->
                val contributor =
                    loadTopContributor("kotlin", repo.name).getOrNull()?.firstOrNull()
                repo.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
            }?.also {
                data.postValue(Data.Success(it))
            }
        } onFailure {
            data.postValue(Data.Error(it.message))
        }
    }

    fun setMode(mode: Mode) {
        mocker.mode = mode
        if (mocker.mode == Mode.RECORD) {
            state.postValue(State.Permission)
        }
        state.postValue(
            State.Message(
                when (mocker.mode) {
                    Mode.DISABLED -> R.string.disabled_description
                    Mode.ENABLED -> R.string.enabled_description
                    Mode.MIXED -> R.string.mixed_description
                    Mode.RECORD -> R.string.record_description
                }
            )
        )
    }

    private suspend fun loadRepos(org: String) = withContext(Dispatchers.IO) {
        apiService.listRepositoriesForOrganisation(org)
    }

    private suspend fun loadTopContributor(org: String, repo: String) =
        withContext(Dispatchers.IO) {
            resultOf {
                apiService.listContributorsForRepository(org, repo)
            } onFailure {
                Log.e("ViewModel", it.message, it)
            }
        }
}

sealed class Data {
    object Loading : Data()
    data class Success(val repos: List<Repo>) : Data()
    data class Error(val message: String?) : Data()
}

sealed class State {
    data class Message(@StringRes val message: Int) : State()
    object Permission : State()
}
