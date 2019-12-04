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

buildscript {

    repositories {
        google()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(BuildPlugins.androidGradlePlugin)
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath(BuildPlugins.kotlinSerialization)
        classpath(BuildPlugins.dokka)
        classpath(BuildPlugins.bintray)
        classpath(BuildPlugins.artifactory)
        classpath(BuildPlugins.ktlint)
    }
}

plugins {
    id(BuildPlugins.PluginsId.detekt) version "1.0.0"
    id(BuildPlugins.PluginsId.ktlint) version BuildPlugins.Versions.ktlint
}

allprojects {

    group = "fr.speekha.httpmocker"
    version =
        httpmock_version + if (System.getProperty("snapshot")?.toBoolean() == true) "-SNAPSHOT" else ""

    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/kotlin/kotlinx/")
    }
}

subprojects {
    //        tasks.withType<Test> {
//        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
//    }

    apply(plugin = "org.jmailen.kotlinter")

    kotlinter {
        ignoreFailures = false
        indentSize = 4
        continuationIndentSize = 4
        reporters = arrayOf("checkstyle", "html")
        experimentalRules = false
        disabledRules = emptyArray()
        fileBatchSize = 30
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply(from = "gradle/detekt.gradle")
