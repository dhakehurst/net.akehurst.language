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
}

dependencies {

   // "jvm8MainImplementation"("iguana:iguana:0.0.1-SNAPSHOT")

    //"commonMainImplementation"(kotlin("stdlib"))
    "commonTestImplementation"(kotlin("test"))
    "commonTestImplementation"(kotlin("test-annotations-common"))

    //"jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    "jvm8TestImplementation"(kotlin("test-junit"))
    "jvm8TestImplementation"("junit:junit:4.12") {
        version { strictly("4.12") }
    }


    "jvm8TestImplementation"(project(":common"))
}
