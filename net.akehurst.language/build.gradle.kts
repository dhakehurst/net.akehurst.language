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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

//import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform") version ("1.4.0") apply false
    id("org.jetbrains.dokka") version ("1.4.0") apply false
    id("nu.studer.credentials") version ("2.1")
}

allprojects {

    val version_project: String by project
    val group_project = "${rootProject.name}"

    group = group_project
    version = version_project

    buildDir = File(rootProject.projectDir, ".gradle-build/${project.name}")

}

fun getProjectProperty(s: String) = project.findProperty(s) as String?


subprojects {

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.named("publish").get().dependsOn("javadocJar")

    configure<KotlinMultiplatformExtension> {
        jvm("jvm8") {
            val main by compilations.getting {
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_1_8.toString()
                }
            }
            val test by compilations.getting {
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_1_8.toString()
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

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from(project.properties["javadoc"])
    }

    val now = Instant.now()
    fun fBbuildStamp(): String {
        return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(now)
    }

    fun fBuildDate(): String {
        return DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(ZoneId.of("UTC")).format(now)
    }

    fun fBuildTime(): String {
        return DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(ZoneId.of("UTC")).format(now)
    }
    tasks.register<Copy>("generateFromTemplates") {
        val templateContext = mapOf(
            "version" to project.version,
            "buildStamp" to fBbuildStamp(),
            "buildDate" to fBuildDate(),
            "buildTime" to fBuildTime()
        )
        inputs.properties(templateContext) // for gradle up-to-date check
        from("src/template/kotlin")
        into("$buildDir/generated/kotlin")
        expand(templateContext)
    }
    tasks.getByName("compileKotlinMetadata") {
        dependsOn("generateFromTemplates")
    }
    tasks.getByName("compileKotlinJvm8") {
        dependsOn("generateFromTemplates")
    }
    tasks.getByName("compileKotlinJs") {
        dependsOn("generateFromTemplates")
    }
//    tasks.getByName("compileKotlinMacosX64") {
//        dependsOn("generateFromTemplates")
//    }
    dependencies {
        "commonMainImplementation"(kotlin("stdlib"))
        "commonTestImplementation"(kotlin("test"))
        "commonTestImplementation"(kotlin("test-annotations-common"))

        "jvm8MainImplementation"(kotlin("stdlib-jdk8"))
        "jvm8TestImplementation"(kotlin("test-junit"))

        "jsMainImplementation"(kotlin("stdlib-js"))
        "jsTestImplementation"(kotlin("test-js"))
    }

    val creds = project.properties["credentials"] as nu.studer.gradle.credentials.domain.CredentialsContainer
    val sonatype_pwd = creds.propertyMissing("SONATYPE_PASSWORD") as String?
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
        }
    }

    configure<SigningExtension> {
        val publishing = project.properties["publishing"] as PublishingExtension
        sign(publishing.publications)
    }
}