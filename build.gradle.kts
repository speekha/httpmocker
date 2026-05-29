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

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.3.21")
    }
}

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.vanniktechPublish) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString() +
        if (System.getProperty("snapshot").toBoolean()) "-SNAPSHOT" else ""
}

// Apply ktlint only to test and demo modules, not to publishable libraries
configure(subprojects.filter { it.name in listOf("tests", "demo") }) {
    apply(from = "../gradle/ktlint.gradle")
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

apply(from = "gradle/detekt.gradle")
