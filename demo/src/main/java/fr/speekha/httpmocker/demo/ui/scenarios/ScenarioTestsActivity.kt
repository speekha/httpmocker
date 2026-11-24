/*
 * Copyright 2019-2026 David Blanc
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

package fr.speekha.httpmocker.demo.ui.scenarios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.scenarios.ScenarioResult
import fr.speekha.httpmocker.demo.scenarios.ScenarioRunner
import fr.speekha.httpmocker.demo.scenarios.createScenarioClient
import fr.speekha.httpmocker.demo.ui.theme.DemoTheme
import fr.speekha.httpmocker.demo.ui.theme.StatusGreen
import fr.speekha.httpmocker.demo.ui.theme.StatusOrange
import fr.speekha.httpmocker.demo.ui.theme.StatusRed
import fr.speekha.httpmocker.demo.ui.theme.mediumPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LABEL_WEIGHT = 0.25f
private const val VALUE_WEIGHT = 0.75f
private const val FIRST_SUCCESS_CODE = 200
private const val FIRST_REDIRECT_CODE = 300
private const val FIRST_ERROR_CODE = 400
private val SUCCESS_CODES = FIRST_SUCCESS_CODE until FIRST_REDIRECT_CODE
private val REDIRECT_CODES = FIRST_REDIRECT_CODE until FIRST_ERROR_CODE
private val BadgeWidth = 52.dp
private val BadgeHeight = 36.dp

class ScenarioTestsActivity : ComponentActivity() {

    private val viewModel by viewModels<ScenarioTestsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoTheme {
                ScenarioTestsScreen(viewModel, onClose = ::finish)
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ScenarioTestsActivity::class.java))
        }
    }
}

data class ScenarioTestsState(
    val results: List<ScenarioResult> = emptyList(),
    val isRunning: Boolean = false
)

class ScenarioTestsViewModel : ViewModel() {

    private val runner = ScenarioRunner(::createScenarioClient)

    private val mutableState = MutableStateFlow(ScenarioTestsState())
    val state: StateFlow<ScenarioTestsState> = mutableState.asStateFlow()

    private var job: Job? = null

    fun runScenarios() {
        job?.cancel()
        mutableState.value = ScenarioTestsState(isRunning = true)
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                runner.runAll { result ->
                    mutableState.update { it.copy(results = it.results + result) }
                }
            } finally {
                mutableState.update { it.copy(isRunning = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioTestsScreen(viewModel: ScenarioTestsViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let { state.results.getOrNull(it) }

    BackHandler(enabled = selected != null) { selectedIndex = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.scenario ?: stringResource(R.string.scenario_tests_title), maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = { if (selected != null) selectedIndex = null else onClose() }) {
                        Text(stringResource(R.string.back_button))
                    }
                },
                actions = {
                    if (selected == null) {
                        TextButton(onClick = viewModel::runScenarios, enabled = !state.isRunning) {
                            Text(stringResource(R.string.run_scenarios_button))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                selected != null -> ResultDetail(selected)
                state.results.isEmpty() && !state.isRunning -> EmptyState()
                else -> ResultList(state) { selectedIndex = it }
            }
        }
    }
}

@Composable
private fun EmptyState() = Box(modifier = Modifier.fillMaxSize()) {
    Text(
        text = stringResource(R.string.scenario_tests_empty),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.Center)
    )
}

@Composable
private fun ResultList(state: ScenarioTestsState, onSelect: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.results.withIndex().toList()) { (index, result) ->
            ResultRow(result, Modifier.clickable { onSelect(index) })
            HorizontalDivider()
        }
        if (state.isRunning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: ScenarioResult, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = mediumPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusBadge(result)
        Column {
            Text(text = result.scenario, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(
                text = "${result.method}  ${result.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusBadge(result: ScenarioResult) {
    Box(
        modifier = Modifier
            .size(width = BadgeWidth, height = BadgeHeight)
            .background(result.statusColor(), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = result.statusText(),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ResultDetail(result: ScenarioResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailCard(stringResource(R.string.scenario_request)) {
            DetailRow(stringResource(R.string.scenario_method), result.method)
            DetailRow(stringResource(R.string.scenario_url), result.url)
            DetailRow(stringResource(R.string.scenario_status), result.statusText())
        }
        DetailCard(stringResource(R.string.scenario_response_body)) {
            Text(
                text = result.body.ifEmpty { stringResource(R.string.scenario_empty_body) },
                fontFamily = FontFamily.Monospace
            )
        }
        if (result.isError) {
            DetailCard(stringResource(R.string.scenario_error), titleColor = StatusRed) {
                Text(text = result.error, fontFamily = FontFamily.Monospace, color = StatusRed)
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    titleColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(mediumPadding)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_WEIGHT)
        )
        Text(text = value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(VALUE_WEIGHT))
    }
}

private fun ScenarioResult.statusText(): String = if (isError) "ERR" else statusCode.toString()

private fun ScenarioResult.statusColor(): Color = when {
    isError -> StatusRed
    statusCode in SUCCESS_CODES -> StatusGreen
    statusCode in REDIRECT_CODES -> StatusOrange
    else -> StatusRed
}
