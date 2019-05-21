package fr.speekha.httpmocker.demo.di

import android.content.Context
import android.os.Environment
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.service.GithubApiEndpoints
import fr.speekha.httpmocker.demo.ui.MainContract
import fr.speekha.httpmocker.demo.ui.MainPresenter
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

val injectionModule: Module = module {

    single {
        MockResponseInterceptor(MirrorPathPolicy(), {
            get<Context>().assets.open(it)
        }, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
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
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
    }

    single<GithubApiEndpoints> {
        get<Retrofit>().create(GithubApiEndpoints::class.java)
    }

    factory<MainContract.Presenter> { MainPresenter(get(), get()) }
}