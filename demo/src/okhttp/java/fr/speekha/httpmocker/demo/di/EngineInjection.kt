package fr.speekha.httpmocker.demo.di

import android.content.Context
import android.os.Environment
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.ui.MockerWrapper
import fr.speekha.httpmocker.jackson.JacksonMapper
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import fr.speekha.httpmocker.okhttp.builder.mockInterceptor
import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

val engineInjection: Module = module {

    single {
        mockInterceptor {
            decodeScenarioPathWith(get<FilingPolicy>())
            loadFileWith(get<Context>().assets::open)
            parseScenariosWith(JacksonMapper())
            saveScenarios(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                get()
            )
            addFakeNetworkDelay(500)
        }
    }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .addInterceptor(get<MockResponseInterceptor>())
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .client(get())
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
    }

    single<GithubApiEndpoints> {
        get<Retrofit>().create(GithubApiEndpoints::class.java)
    }

    single { MockerWrapper(get()) }
}
