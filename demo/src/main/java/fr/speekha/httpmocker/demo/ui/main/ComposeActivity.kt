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

package fr.speekha.httpmocker.demo.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.model.onFailure
import fr.speekha.httpmocker.demo.model.onSuccess
import fr.speekha.httpmocker.demo.model.resultOf
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.ui.MockerWrapper
import fr.speekha.httpmocker.demo.ui.theme.DemoTheme
import fr.speekha.httpmocker.demo.ui.theme.mediumPadding
import fr.speekha.httpmocker.getLogger
import io.uniflow.android.AndroidDataFlow
import io.uniflow.android.livedata.states
import io.uniflow.core.flow.data.UIEvent
import io.uniflow.core.flow.data.UIState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class ComposeActivity : ComponentActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private val logger = getLogger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                Content()
            }
        }
        viewModel.initState()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                )
            }
        ) { paddingValues ->
            logger.debug("Rendering layout")
            (viewModel.states.observeAsState().value as? State)?.let { state ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.mocking_state),
                        modifier = Modifier.padding(mediumPadding)
                    )
                    ToggleGroup(
                        items = listOf(
                            R.string.state_disabled,
                            R.string.state_enabled,
                            R.string.state_mixed,
                            R.string.state_record
                        ),
                        selected = state.mode.ordinal,
                        onClick = viewModel::setMode
                    )
                    Text(
                        text = stringResource(id = state.message),
                        modifier = Modifier.padding(mediumPadding)
                    )
                    Button(
                        onClick = { viewModel.callService() },
                        modifier = Modifier.padding(vertical = mediumPadding)
                    ) {
                        Text(
                            text = getString(R.string.call_service_button),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(1f)
                        )
                    }
                    RepoList(state)
                }
            }
        }
    }
}

class MainViewModel(
    private val apiService: GithubApiEndpoints,
    private val mocker: MockerWrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidDataFlow(
    defaultState = State(
        message = R.string.disabled_description,
        Mode.DISABLED,
        Data.Empty
    )
) {

    private val logger = getLogger()

    private var selectedMode = 0

    fun initState() {
        action {
            setState(
                State(
                    message = R.string.disabled_description,
                    Mode.DISABLED,
                    Data.Empty
                )
            )
        }
    }

    override fun getState(): State = super.getState() as State

    fun callService() = viewModelScope.launch {
        setState(getState().copy(data = Data.Loading))
        resultOf {
            logger.debug("Loading repos")
            loadRepos("kotlin")
        } onSuccess { repos ->
            logger.debug("Loaded repos: $repos")
            repos?.map { repo ->
                val contributor = loadTopContributor("kotlin", repo.name).getOrNull()?.firstOrNull()
                repo.copy(topContributor = contributor?.run { "$login - $contributions contributions" })
            }?.also {
                setState(getState().copy(data = Data.Success(it)))
            }
        } onFailure { error ->
            logger.error("Error loading repos", error)
            setState(getState().copy(data = Data.Error(error.message)))
        }
    }

    fun setMode(selectedId: Int) {
        selectedMode = selectedId
        val mode = when (selectedId) {
            R.string.state_enabled -> Mode.ENABLED
            R.string.state_mixed -> Mode.MIXED
            R.string.state_record -> Mode.RECORD
            else -> Mode.DISABLED
        }
        setMode(mode)
    }

    fun setMode(mode: Mode) = viewModelScope.launch {
        mocker.mode = mode
        if (mocker.mode == Mode.RECORD) {
            sendEvent(Permission)
        }
        setState(
            State(
                message = when (mocker.mode) {
                    Mode.DISABLED -> R.string.disabled_description
                    Mode.ENABLED -> R.string.enabled_description
                    Mode.MIXED -> R.string.mixed_description
                    Mode.RECORD -> R.string.record_description
                },
                mode = mode,
                data = Data.Empty
            )
        )
    }

    private suspend fun loadRepos(org: String) = withContext(ioDispatcher) {
        apiService.listRepositoriesForOrganisation(org)
    }

    private suspend fun loadTopContributor(org: String, repo: String) =
        withContext(ioDispatcher) {
            resultOf {
                apiService.listContributorsForRepository(org, repo)
            } onFailure {
                Log.e("ViewModel", it.message, it)
            }
        }
}

sealed class Data {
    object Empty : Data()
    object Loading : Data()
    data class Success(val repos: List<Repo>) : Data()
    data class Error(val message: String?) : Data()
}

data class State(
    @field:StringRes val message: Int,
    val mode: Mode,
    val data: Data
) : UIState()

object Permission : UIEvent()
