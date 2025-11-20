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

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.exportPublic)
    signing
    alias(libs.plugins.vanniktech.maven.publish)
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("net\\.akehurst.+")
        }
        mavenContent {
            snapshotsOnly()
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

group = rootProject.name
version = libs.versions.project.get()
project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")

fun getProjectProperty(s: String) = project.findProperty(s) as String?

buildConfig {
    useKotlinOutput {
        this.internalVisibility = false
    }
    val now = Instant.now()
    fun fBbuildStamp(): String = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(now)
    fun fBuildDate(): String = DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(ZoneId.of("UTC")).format(now)
    fun fBuildTime(): String = DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(ZoneId.of("UTC")).format(now)

    packageName("${project.group}.${project.name.replace("-", ".")}")
    buildConfigField("String", "version", "\"${project.version}\"")
    buildConfigField("String", "buildStamp", "\"${fBbuildStamp()}\"")
    buildConfigField("String", "buildDate", "\"${fBuildDate()}\"")
    buildConfigField("String", "buildTime", "\"${fBuildTime()}\"")
}

kotlin {
    applyDefaultHierarchyTemplate()
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
    jvm {
        val main by compilations.getting {
            compileTaskProvider.configure {
                compilerOptions {
                    languageVersion.set(kotlin_languageVersion)
                    apiVersion.set(kotlin_apiVersion)
                    jvmTarget.set(jvmTargetVersion)
                }
            }
        }
        val test by compilations.getting {
            compileTaskProvider.configure {
                compilerOptions {
                    languageVersion.set(kotlin_languageVersion)
                    apiVersion.set(kotlin_apiVersion)
                    jvmTarget.set(jvmTargetVersion)
                }
            }
        }
    }
    js {
        binaries.library()
        nodejs()
        browser()
        generateTypeScriptDefinitions()
        compilerOptions {
            target.set("es2015")
            freeCompilerArgs = listOf("-Xes-long-as-bigint")
        }
    }
    wasmJs {
        binaries.library()
        browser {
            testTask {
                useKarma {
                     useChrome()
                }
            }
        }
    }
    //macosArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            implementation("org.junit.jupiter:junit-jupiter")
            runtimeOnly("org.junit.platform:junit-platform-launcher")
            runtimeOnly("org.junit.jupiter:junit-jupiter-params")
        }
    }
}

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}
tasks.named("publish").get().dependsOn("javadocJar")

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val sonatype_pwd = null/*creds.forKey("SONATYPE_PASSWORD")*/
    ?: getProjectProperty("SONATYPE_PASSWORD")
    ?: error("Must set project property with Sonatype Password (-P SONATYPE_PASSWORD=<...> or set in ~/.gradle/gradle.properties)")
project.ext.set("signing.password", sonatype_pwd)

signing {
    setRequired( {  gradle.taskGraph.hasTask("uploadArchives") })
    useGpgCmd()
    val publishing = project.properties["publishing"] as PublishingExtension
    sign(publishing.publications)
}
val signTasks = tasks.matching { it.name.matches(Regex("sign(.)+")) }.toTypedArray()
tasks.forEach {
    when {
        it.name.matches(Regex("publish(.)+")) -> {
            it.mustRunAfter(*signTasks)
        }
    }
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(automaticRelease = false)

    coordinates(group as String, project.name, version as String)
    pom {
        name.set("AGL Parser, Processor, etc")
        description.set("Dynamic, scan-on-demand, parsing; when a regular expression is just not enough")
        url.set("https://medium.com/@dr.david.h.akehurst/a-kotlin-multi-platform-parser-usable-from-a-jvm-or-javascript-59e870832a79")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                name.set("Dr. David H. Akehurst")
                email.set("dr.david.h@akehurst.net")
            }
        }
        scm {
            url.set("https://github.com/dhakehurst/net.akehurst.language")
        }
    }
}
