/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

val version_korio: String by project
val version_kotest: String = "5.7.2"

plugins {
    // id("io.kotest.multiplatform") version ("5.7.2")
}

dependencies {

    commonTestImplementation(project(":agl-processor"))

    commonTestImplementation("com.soywiz.korlibs.korio:korio:$version_korio")

    commonTestImplementation("io.kotest:kotest-assertions-core:$version_kotest")
    commonTestImplementation("io.kotest:kotest-framework-engine:$version_kotest")
    commonTestImplementation("io.kotest:kotest-framework-datatest:$version_kotest")
    jvm8TestImplementation("io.kotest:kotest-runner-junit5:$version_kotest")

}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
        events(PASSED, SKIPPED, FAILED, STANDARD_OUT)
        exceptionFormat = TestExceptionFormat.FULL
    }

}

kotlin {
    sourceSets {
        commonMain.configure {
            resources.srcDir(projectDir.resolve("languages"))
        }
    }
}

tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}