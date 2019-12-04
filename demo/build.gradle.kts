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

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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


plugins {
    id(BuildPlugins.PluginsId.androidApplication)
    id(BuildPlugins.PluginsId.kotlinAndroid)
    id(BuildPlugins.PluginsId.kotlinAndroidExtensions)
}

android {
    compileSdkVersion(AndroidSdk.compile)
    sourceSets["main"].withConvention(KotlinSourceSet::class) {
        kotlin.srcDirs("src/main/kotlin")
    }
    defaultConfig {
        applicationId = "fr.speekha.httpmocker.demo"
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = TestLibraries.testRunner
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildToolsVersion("29.0.2")
}

//repositories {
//    // For snapshots versions
//    maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
//}

dependencies {
    implementation(kotlin(module = "stdlib", version = kotlinVersion))
    implementation(Libraries.coroutinesCore)
    implementation(Libraries.coroutinesAndroid)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.lifecycle)
    implementation(Libraries.constraintLayout)
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("com.google.android.material:material:1.2.0-alpha02")
    implementation(Libraries.retrofit)
    implementation("com.squareup.retrofit2:converter-jackson:${LibVersions.retrofit}")
    implementation("org.koin:koin-androidx-viewmodel:2.0.1")
    implementation(Libraries.slf4jAndroid)
    //implementation "fr.speekha.httpmocker:jackson-adapter:$httpmock_version"
    implementation(project(":jackson-adapter"))

    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.mockk)
    testImplementation(TestLibraries.archCore)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibVersions.coroutines}")
}
