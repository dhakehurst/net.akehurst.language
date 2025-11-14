@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8

kotlin {
    applyDefaultHierarchyTemplate()
    jvm("jvm8") {
        compilations {
            val main by getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
            }
            val test by getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
            }
        }
    }
    js("js") {
        binaries.executable()
        compilerOptions {
            target.set("es2015")
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

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.nal.agl.processor)
                implementation(project(":common"))
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvm8Test by getting {
            resources.srcDirs("src/commonMain/resources")
        }
    }
}

