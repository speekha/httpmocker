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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(BuildPlugins.PluginsId.kotlin)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation(kotlin(module = "stdlib-jdk8", version = kotlinVersion))

    testImplementation(project(":mocker"))
    testImplementation(project(":jackson-adapter"))
    testImplementation(project(":gson-adapter"))
    testImplementation(project(":moshi-adapter"))
    testImplementation(project(":kotlinx-adapter"))
    testImplementation(project(":custom-adapter"))
    testImplementation(project(":sax-adapter"))

    testImplementation(Libraries.okHttp)
    testImplementation(TestLibraries.mockServer)

    testImplementation(Libraries.jacksonCore)
    testImplementation(Libraries.jacksonKotlin)
    testImplementation(Libraries.jacksonAnnotations)
    testImplementation(Libraries.jacksonDatabind)

    testImplementation(TestLibraries.junit5api)
    testImplementation(TestLibraries.junit5params)
    testImplementation(TestLibraries.junit5engine)
    testImplementation(TestLibraries.mockitoCore)
    testImplementation(TestLibraries.mockitoKotlin)

    testImplementation(Libraries.slf4jSimple)
}

tasks.withType<KotlinCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

apply(from = "../gradle/coverage.gradle")