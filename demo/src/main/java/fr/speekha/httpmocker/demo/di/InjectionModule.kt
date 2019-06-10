/*
 * Copyright 2019 David Blanc
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

package fr.speekha.httpmocker.demo.di

import android.content.Context
import android.os.Environment
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
        MockResponseInterceptor(
            MirrorPathPolicy(),
            { get<Context>().assets.open(it) },
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
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

    factory<MainContract.Presenter> { (view: MainContract.View) -> MainPresenter(view, get(), get()) }
}