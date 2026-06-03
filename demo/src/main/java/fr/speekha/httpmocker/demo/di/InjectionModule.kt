/*
 * Copyright 2019-2021 David Blanc
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

import fr.speekha.httpmocker.demo.ui.MainViewModel
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.serialization.JSON_FORMAT
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val injectionModule: Module = module {

    single<FilingPolicy> {
        MirrorPathPolicy(JSON_FORMAT)
    }

    viewModel {
        MainViewModel(get(), get())
    }
}
