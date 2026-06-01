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

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    kotlin("plugin.serialization")
}

kotlin {

    iosArm64 {
        binaries.framework {
            baseName = "httpmocker_tests"
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "httpmocker_tests"
        }
    }

    iosX64 {
        binaries.framework {
            baseName = "httpmocker_tests"
        }
    }

    sourceSets {
        iosTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.core)

                // Ktor
                implementation(libs.bundles.ktor)
                implementation(libs.kotlinx.serialization)

                // Local projects - multiplatform modules only
                implementation(project(":mocker-core"))
                implementation(project(":mocker-ktor"))
                implementation(project(":kotlinx-adapter"))
                implementation(project(":custom-adapter"))
            }
        }
    }
}
