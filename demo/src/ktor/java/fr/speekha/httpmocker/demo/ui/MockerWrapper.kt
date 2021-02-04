package fr.speekha.httpmocker.demo.ui

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.ktor.engine.MockEngine
import io.ktor.client.*

class MockerWrapper(client: HttpClient) {
    var mode: Mode by (client.engine as MockEngine)::mode
}
