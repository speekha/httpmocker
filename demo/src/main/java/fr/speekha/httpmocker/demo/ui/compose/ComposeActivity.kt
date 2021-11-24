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

package fr.speekha.httpmocker.demo.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.ui.MainViewModel
import fr.speekha.httpmocker.demo.ui.State
import fr.speekha.httpmocker.demo.ui.compose.theme.DemoTheme
import fr.speekha.httpmocker.demo.ui.compose.theme.mediumPadding
import fr.speekha.httpmocker.getLogger
import io.uniflow.android.livedata.states
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

    @Composable
    private fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                )
            }
        ) {
            logger.debug("Rendering layout")
            (viewModel.states.observeAsState().value as? State)?.let { state ->
                Column(
                    modifier = Modifier.padding(mediumPadding)
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
