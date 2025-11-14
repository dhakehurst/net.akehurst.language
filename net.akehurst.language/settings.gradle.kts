/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
//        mavenLocal {
//            content {
//                includeGroupByRegex("net\\.akehurst.+")
//            }
//        }
        gradlePluginPortal()
    }
    includeBuild("./0_build-logic")
}

rootProject.name = file(".").name

fileTree(".") {
    exclude("build.gradle.kts")
    exclude("_buildSrc")
    exclude("0_build-logic")
    include("**/build.gradle.kts")
}.forEach {
    val prjName = it.parentFile.name
    val prjPath = relativePath(it.parent)
    println("including $prjName at $prjPath")
    include(prjName)
    // project(":$prjName").name = prjName
    project(":$prjName").projectDir = File(prjPath)
}
