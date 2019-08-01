package fr.speekha.httpmocker.demo.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule

@ExperimentalCoroutinesApi
open class ViewModelTest {

    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        coroutinesTestRule.testDispatcher.runBlockingTest(block)
}