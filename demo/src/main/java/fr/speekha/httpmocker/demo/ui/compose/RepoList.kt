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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.ui.Data
import fr.speekha.httpmocker.demo.ui.State
import fr.speekha.httpmocker.demo.ui.compose.theme.mediumPadding

@Composable
fun RepoList(
    state: State
) = LazyColumn {
    when (val data = state.data) {
        is Data.Loading -> item {
            Loading()
        }
        is Data.Success -> items(data.repos) { repo ->
            RepoEntry(repo)
        }
        is Data.Error -> item {
            DisplayMessage(data.message ?: "No result to display")
        }
        is Data.Empty -> item {
            DisplayMessage("No result to display")
        }
    }
}

@Composable
private fun RepoEntry(
    repo: Repo
) {
    Text(
        text = repo.name,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(mediumPadding)
    )
    Text(
        text = repo.topContributor ?: "Error retrieving contributor",
        textAlign = TextAlign.End,
        modifier = Modifier
            .padding(mediumPadding)
            .fillMaxWidth(1f)
    )
}

@Composable
fun LazyItemScope.Loading() = Box(
    modifier = Modifier
        .fillParentMaxWidth(1f)
        .fillParentMaxHeight(1f)
) {
    CircularProgressIndicator(Modifier.align(Alignment.Center))
}

@Composable
fun LazyItemScope.DisplayMessage(message: String) = Box(
    modifier = Modifier
        .fillParentMaxWidth(1f)
        .fillParentMaxHeight(1f)
) {
    Text(
        text = message,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.Center)
    )
}
