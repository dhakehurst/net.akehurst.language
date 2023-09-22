/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

println("===============================================")
println("Gradle: ${GradleVersion.current()}")
println("JVM: ${org.gradle.internal.jvm.Jvm.current()} '${org.gradle.internal.jvm.Jvm.current().javaHome}'")
println("===============================================")

pluginManagement {
    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("net\\.akehurst.+")
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = file(".").name

fileTree(".") {
    include("**/build.gradle")
    include("**/build.gradle.kts")
    exclude("build.gradle") // Exclude the root _build file.
    exclude("build.gradle.kts") // Exclude the root _build file.
}.forEach {
    val prj = it.parentFile.name
    println("including $prj at " + relativePath(it.parent))
    include(prj)
    project(":$prj").projectDir = File(relativePath(it.parent))
}