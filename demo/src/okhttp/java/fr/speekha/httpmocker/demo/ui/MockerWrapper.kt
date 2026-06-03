package fr.speekha.httpmocker.demo.ui

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor

class MockerWrapper(interceptor: MockResponseInterceptor) {
    var mode: Mode by interceptor::mode
}
