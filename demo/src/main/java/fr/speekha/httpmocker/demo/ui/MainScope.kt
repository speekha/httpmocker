package fr.speekha.httpmocker.demo.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class MainScope : CoroutineScope {

    val job = Job()

    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

}
