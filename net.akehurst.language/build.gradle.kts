/**
 * Copyright (C) 2016 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import com.github.gmazzo.gradle.plugins.BuildConfigExtension

plugins {
    //kotlin("multiplatform") version ("1.5.20-RC-179") apply false
    kotlin("multiplatform") version ("1.5.10") apply false
    id("org.jetbrains.dokka") version ("1.4.32") apply false
    id("com.github.gmazzo.buildconfig") version("3.0.0") apply false
    //id("jacoco")
}

allprojects {
    //apply(plugin = "jacoco")

    val version_project: String by project
    val group_project = rootProject.name

    group = group_project
    version = version_project

    buildDir = File(rootProject.projectDir, ".gradle-build/${project.name}")

}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.github.gmazzo.buildconfig")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        }
    }

    configure<BuildConfigExtension> {
        val now = java.time.Instant.now()
        fun fBbuildStamp(): String = java.time.format.DateTimeFormatter.ISO_DATE_TIME.withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildDate(): String = java.time.format.DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildTime(): String= java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(java.time.ZoneId.of("UTC")).format(now)

        packageName("${project.group}.agl.processor")
        buildConfigField("String", "version", "\"${project.version}\"")
        buildConfigField("String", "buildStamp", "\"${fBbuildStamp()}\"")
        buildConfigField("String", "buildDate", "\"${fBuildDate()}\"")
        buildConfigField("String", "buildTime", "\"${fBuildTime()}\"")
    }

    configure<KotlinMultiplatformExtension> {

        jvm("jvm8") {
            val main by compilations.getting {
                kotlinOptions {
                    languageVersion = "1.5"
                    apiVersion = "1.5"
                    jvmTarget = JavaVersion.VERSION_1_8.toString()
                    //useIR = true
                }
            }
            val test by compilations.getting {
                kotlinOptions {
                    languageVersion = "1.5"
                    apiVersion = "1.5"
                    jvmTarget = JavaVersion.VERSION_1_8.toString()
                    //useIR = true
                }
            }
        }
        js("js") {
            nodejs()
            browser {
                webpackTask {
                    outputFileName = "${project.group}-${project.name}.js"
                }
            }
        }
        //macosX64("macosX64") {
        // uncomment stuff below too
        //}
        sourceSets {
            val commonMain by getting {
                kotlin.srcDir("$buildDir/generated/kotlin")
            }
        }
    }

    dependencies {
        "commonTestImplementation"(kotlin("test"))
        "commonTestImplementation"(kotlin("test-annotations-common"))
    }

}