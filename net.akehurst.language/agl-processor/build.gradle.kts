/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import org.gradle.internal.impldep.org.apache.commons.io.output.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

plugins {
    kotlin("multiplatform") version ("1.3.41")
    `maven-publish`
}

fun getPassword(currentUser: String, location: String): String {
    return if (project.hasProperty("maclocal")) {
        println("Getting password using local mac login credentials")
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        exec {
            commandLine = listOf("security", "-q", "find-internet-password", "-a", currentUser, "-s", location, "-w")
            standardOutput = stdout
            errorOutput = stderr
            setIgnoreExitValue(true)
        }
        stdout.toString().trim()
    } else {
        ""
    }
}

val version_kotlin: String by project

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
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

kotlin {
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
        //val main by compilations.getting {
        //    kotlinOptions {
        //        moduleKind = "plain"
        //    }
        // }
        nodejs()
        browser()
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("$buildDir/generated/kotlin")
        }
    }
    
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
//tasks.getByName("compileKotlinMacosX64") {
//    dependsOn("generateFromTemplates")
//}

dependencies {
    "commonMainImplementation"(kotlin("stdlib"))
    "commonTestImplementation"(kotlin("test"))
    "commonTestImplementation"(kotlin("test-annotations-common"))

    "jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    //"jvm8TestImplementation"("org.jetbrains.kotlin:kotlin-test:${version_kotlin}")
    "jvm8TestImplementation"(kotlin("test-junit"))

    "jsMainImplementation"(kotlin("stdlib-js"))
    "jsTestImplementation"(kotlin("test-js"))
}


publishing {
    repositories {
        maven {
            name = "itemis-akehurst"
            url = uri("https://projects.itemis.de/nexus/content/repositories/akehurst/")
            credentials {
                username = if (project.hasProperty("username")) project.property("username") as String else System.getenv("USER")
                password = if (project.hasProperty("password")) project.property("password") as String else if (project.hasProperty("maclocal")) getPassword(System.getenv("USER"), "projects.itemis.de") else System.getenv("PASSWORD")
            }
        }
    }

}
