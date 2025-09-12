@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    kotlin("multiplatform")
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
kotlin {
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
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
               // api(libs.korlibs.korio)

                implementation(kotlin("test"))
                implementation(kotlin("test-annotations-common"))

                api("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
            }
        }

        jsMain {
            dependencies {
              //  api(libs.korlibs.io.nodejs)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}


dependencies {
    //"jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    "jvm8TestImplementation"(kotlin("test-junit"))

    "jvm8MainImplementation"("org.apache.poi:poi:4.1.2")
    "jvm8MainImplementation"("org.apache.poi:poi-ooxml:4.1.2")
}