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

plugins {
    application
}

// so that the application plugin can find the jars from the kotlin-plugin jvm configuration
val runtimeClasspath by configurations.getting {
    attributes.attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
}
application {
    mainClassName = "net.akehurst.language.editor.desktop.jfx.tornado.MainKt"
}

val version_kaf:String by project
dependencies {
    // need this so that the gradle application-plugin can find the module built by the kotlin-plugin
    runtime( project(path=":application-editor-desktop-jfx", configuration="jvm8RuntimeElements") )

    // --- framework ---
    jvm8MainImplementation("net.akehurst.kaf:kaf-common-realisation:$version_kaf")

    // --- services
    commonMainImplementation("net.akehurst.kaf:kaf-service-logging-api:$version_kaf")
    jvm8MainImplementation("net.akehurst.kaf:kaf-service-logging-log4j2:$version_kaf")
    commonMainImplementation("net.akehurst.kaf:kaf-service-configuration-persistence-fs-korio-hjson:$version_kaf")
    jvm8MainImplementation("net.akehurst.kaf:kaf-service-commandLineHandler-kaf:$version_kaf")

    // -- information ---
    commonMainImplementation(project(":information-editor"))

}
