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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ToggleGroup(
    items: List<Int>,
    selected: Int,
    padding: Dp = 0.dp,
    onClick: (Int) -> Unit
) {
    val cornerRadius = 8.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
    ) {
        items.forEachIndexed { index, item ->
            OutlinedButton(
                modifier = Modifier
                    .offset((-1 * index).dp, 0.dp)
                    .zIndex(if (selected == index) 1f else 0f)
                    .weight(1f),
                onClick = { onClick(item) },
                shape = when (index) {
                    0 -> RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = 0.dp,
                        bottomStart = cornerRadius,
                        bottomEnd = 0.dp
                    )
                    items.size - 1 -> RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = cornerRadius,
                        bottomStart = 0.dp,
                        bottomEnd = cornerRadius
                    )
                    else -> RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                },
                border = BorderStroke(
                    1.dp,
                    if (selected == index) MaterialTheme.colors.primary
                    else Color.DarkGray.copy(alpha = 0.75f)
                ),
                colors = if (selected == index) ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colors.primary
                ) else ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary
                ),
            ) {
                Text(
                    text = stringResource(item),
                    color = if (selected == index) MaterialTheme.colors.primary
                    else Color.DarkGray.copy(alpha = 0.9f),
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
