package fr.speekha.httpmocker.demo.di

import android.content.Context
import android.os.Environment
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.service.GithubEndpointWithKtor
import fr.speekha.httpmocker.demo.ui.MockerWrapper
import fr.speekha.httpmocker.kotlinx.KotlinxMapper
import fr.speekha.httpmocker.ktor.builder.mockableHttpClient
import fr.speekha.httpmocker.policies.FilingPolicy
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import org.koin.core.module.Module
import org.koin.dsl.module

val engineInjection: Module = module {

    single {
        mockableHttpClient(CIO) {
            mock {
                decodeScenarioPathWith(get<FilingPolicy>())
                loadFileWith(get<Context>().assets::open)
                parseScenariosWith(KotlinxMapper())
                saveScenarios(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    get()
                )
                addFakeNetworkDelay(500)
            }

            install(JsonFeature) {
                serializer = JacksonSerializer()
            }

            expectSuccess = false
            followRedirects = false
        }
    }

    single<GithubApiEndpoints> { GithubEndpointWithKtor(get()) }

    single { MockerWrapper(get()) }
}
