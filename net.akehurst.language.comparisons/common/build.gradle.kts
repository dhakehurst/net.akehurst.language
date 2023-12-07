import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    kotlin("multiplatform")
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8

kotlin {
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
        tasks.withType<KotlinJsCompile>().configureEach {
            kotlinOptions {
                moduleKind = "es"
                useEsClasses = true
            }
        }
        nodejs {
        }
        browser {
        }
    }
}

val version_klock:String by project
val version_korge:String by project
val version_coroutines:String by project
dependencies {
    //"commonMainImplementation"(kotlin("stdlib"))
    "commonTestImplementation"(kotlin("test"))
    "commonTestImplementation"(kotlin("test-annotations-common"))
    commonMainApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines")
    //commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    commonMainApi("com.soywiz.korge:korge-core:$version_korge")

    //"jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    "jvm8TestImplementation"(kotlin("test-junit"))

    "jvm8MainImplementation"("org.apache.poi:poi:4.1.2")
    "jvm8MainImplementation"("org.apache.poi:poi-ooxml:4.1.2")
}