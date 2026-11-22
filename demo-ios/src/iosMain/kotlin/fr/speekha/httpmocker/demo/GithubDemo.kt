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

package fr.speekha.httpmocker.demo

import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.service.GithubEndpointWithKtor
import fr.speekha.httpmocker.io.createFileAccessor
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.ktor.engine.MockEngine
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val FAKE_NETWORK_DELAY = 500L
private const val ORGANISATION = "kotlin"

/**
 * Receives the outcome of [GithubDemo.callService]. Called from a background thread.
 */
interface RepoListCallback {
    fun onSuccess(repos: List<Repo>)
    fun onError(message: String)
}

/**
 * iOS counterpart of the Android demo (Ktor flavor): a Ktor client wrapped by HttpMocker that
 * lists the repositories of the Kotlin organisation on GitHub and their top contributor.
 *
 * @param scenarioFolder absolute path of the folder containing the static scenarios
 * (the app bundle's `mocks` folder)
 * @param recordFolder absolute path of the folder where scenarios are saved in RECORD mode
 */
class GithubDemo(scenarioFolder: String, private val recordFolder: String) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentCall: Job? = null

    private val client: HttpClient = mockableHttpClient(Darwin) {
        mock {
            val policy = MirrorPathPolicy(JSON_FORMAT)
            decodeScenarioPathWith(policy)
            loadFileWith { path ->
                createFileAccessor("$scenarioFolder/$path").takeIf { it.exists() }?.getReader()
            }
            parseScenariosWith(KotlinxMapper())
            recordScenariosIn(createFileAccessor(recordFolder), policy)
            addFakeNetworkDelay(FAKE_NETWORK_DELAY)
            setMode(Mode.DISABLED)
        }

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        expectSuccess = false
        followRedirects = false
    }

    private val engine = client.engine as MockEngine

    private val service = GithubEndpointWithKtor(client)

    /** Folder where recorded scenarios are written. */
    val recordPath: String
        get() = recordFolder

    /** Index of the current mode: 0 = disabled, 1 = enabled, 2 = mixed, 3 = record. */
    val modeIndex: Int
        get() = engine.mode.ordinal

    /** Changes the interception mode, using the same indexes as [modeIndex]. */
    fun setModeIndex(index: Int) {
        currentCall?.cancel()
        engine.mode = Mode.entries[index]
    }

    /** Loads the repositories of the Kotlin organisation, then the top contributor of each one. */
    fun callService(callback: RepoListCallback) {
        currentCall?.cancel()
        currentCall = scope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val repos = service.listRepositoriesForOrganisation(ORGANISATION).map { repo ->
                    repo.copy(topContributor = loadTopContributor(repo.name))
                }
                callback.onSuccess(repos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                callback.onError(e.message ?: e::class.simpleName ?: "Unknown error")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadTopContributor(repo: String): String? = try {
        service.listContributorsForRepository(ORGANISATION, repo).firstOrNull()
            ?.run { "$login - $contributions contributions" }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("Error loading contributors for $repo: ${e.message}")
        null
    }
}
