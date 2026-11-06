package fr.speekha.httpmocker.demo.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.ui.theme.mediumPadding
import fr.speekha.httpmocker.demo.ui.webview.WebViewActivity

@Composable
internal fun WebViewButton(mode: Mode, hasWebViewSupport: Boolean) {
    val context = LocalContext.current
    if (hasWebViewSupport) {
        Button(
            onClick = { WebViewActivity.start(context, mode) },
            modifier = Modifier.padding(bottom = mediumPadding)
        ) {
            Text(
                text = stringResource(R.string.open_webview_button),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(1f)
            )
        }
    }
}
