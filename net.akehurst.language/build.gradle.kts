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

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    kotlin("multiplatform") version ("1.9.10") apply false
    id("org.jetbrains.dokka") version ("1.9.0") apply false
    id("com.github.gmazzo.buildconfig") version ("4.1.2") apply false
    id("nu.studer.credentials") version ("3.0")
    id("net.akehurst.kotlin.gradle.plugin.exportPublic") version ("1.9.10") apply false
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8

allprojects {
    val version_project: String by project
    val group_project = rootProject.name

    group = group_project
    version = version_project

    project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")
}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.github.gmazzo.buildconfig")
    apply(plugin = "net.akehurst.kotlin.gradle.plugin.exportPublic")

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("net\\.akehurst.+")
            }
        }
        mavenCentral()
    }

    configure<BuildConfigExtension> {
        useKotlinOutput {
            this.internalVisibility = false
        }
        val now = java.time.Instant.now()
        fun fBbuildStamp(): String = java.time.format.DateTimeFormatter.ISO_DATE_TIME.withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildDate(): String = java.time.format.DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildTime(): String = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(java.time.ZoneId.of("UTC")).format(now)

        packageName("${project.group}.${project.name.replace("-", ".")}")
        buildConfigField("String", "version", "\"${project.version}\"")
        buildConfigField("String", "buildStamp", "\"${fBbuildStamp()}\"")
        buildConfigField("String", "buildDate", "\"${fBuildDate()}\"")
        buildConfigField("String", "buildTime", "\"${fBuildTime()}\"")
    }

    configure<KotlinMultiplatformExtension> {
        jvm("jvm8") {
            compilations {
                val main by getting {
                    compilerOptions.configure {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
                val test by getting {
                    compilerOptions.configure {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
            }
        }
        js("js", IR) {
            useEsModules()
            tasks.withType<KotlinJsCompile>().configureEach {
                kotlinOptions {
                    useEsClasses = true
                }
            }
            nodejs {
                testTask {
                    useMocha {
                        timeout = "5000"
                    }
                }
            }
            browser {
                // webpackTask {
                //    outputFileName = "${project.group}-${project.name}.js"
                // }
                testTask {
                    useMocha {
                        timeout = "5000"
                    }
                }
            }
        }

        //macosX64("macosX64") {
        //}

        sourceSets {
            all {
                languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            }
        }
    }

    //val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        //dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        //from(dokkaHtml.outputDirectory)
    }
    tasks.named("publish").get().dependsOn("javadocJar")

    dependencies {
        "commonTestImplementation"(kotlin("test"))
        "commonTestImplementation"(kotlin("test-annotations-common"))
    }

    fun getProjectProperty(s: String) = project.findProperty(s) as String?

    val creds = project.properties["credentials"] as nu.studer.gradle.credentials.domain.CredentialsContainer
    val sonatype_pwd = creds.forKey("SONATYPE_PASSWORD") as String?
        ?: getProjectProperty("SONATYPE_PASSWORD")
        ?: error("Must set project property with Sonatype Password (-P SONATYPE_PASSWORD=<...> or set in ~/.gradle/gradle.properties)")
    project.ext.set("signing.password", sonatype_pwd)

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "sonatype"
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getProjectProperty("SONATYPE_USERNAME")
                        ?: error("Must set project property with Sonatype Username (-P SONATYPE_USERNAME=<...> or set in ~/.gradle/gradle.properties)")
                    password = sonatype_pwd
                }
            }
        }
        publications.withType<MavenPublication> {
            artifact(javadocJar.get())

            pom {
                name.set("AGL Processor")
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
    }

    configure<SigningExtension> {
        useGpgCmd()
        val publishing = project.properties["publishing"] as PublishingExtension
        sign(publishing.publications)
    }

    tasks.named("publishJsPublicationToMavenLocal").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")
    tasks.named("publishJvm8PublicationToMavenLocal").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")
    tasks.named("publishKotlinMultiplatformPublicationToMavenLocal").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")
    tasks.named("publishJsPublicationToSonatypeRepository").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")
    tasks.named("publishJvm8PublicationToSonatypeRepository").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")
    tasks.named("publishKotlinMultiplatformPublicationToSonatypeRepository").get().mustRunAfter("signKotlinMultiplatformPublication", "signJvm8Publication", "signJsPublication")

}