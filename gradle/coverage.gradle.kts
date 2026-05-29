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

apply(plugin = "jacoco")

val coverageSourceDirs = "src/test/java"

configure<JacocoPluginExtension> {
    toolVersion = "0.8.14"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    group = "Reporting"
    description = "Generate Jacoco coverage reports for Debug build"

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val allClasses = files()
    rootProject.subprojects.forEach { project ->
        project.extensions.findByType<SourceSetContainer>()?.forEach { sourceSet ->
            if (sourceSet.name == "main") {
                sourceSet.output.files.forEach { outputFile ->
                    allClasses.from(fileTree(outputFile).exclude("**/*JsonAdapter.class"))
                }
            }
        }
    }

    additionalSourceDirs.setFrom(files(coverageSourceDirs))
    sourceDirectories.setFrom(files(coverageSourceDirs))
    classDirectories.setFrom(allClasses)
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/test.exec")))
}
