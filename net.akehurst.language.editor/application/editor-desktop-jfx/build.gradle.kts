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
    id("org.openjfx.javafxplugin") version "0.0.8"
}

// so that the application plugin can find the jars from the kotlin-plugin jvm configuration
val runtimeClasspath by configurations.getting {
    attributes.attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
}
application {
    mainClassName = "net.akehurst.language.editor.desktop.jfx.tornado.MainKt"
}

javafx {
    configuration = "jvm8Implementation"
    //version = "8"
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

val version_tornado: String = "1.7.19"
val version_agl:String by project
dependencies {
    // need this so that the gradle application-plugin can find the module built by the kotlin-plugin
    runtime( project(path=":application-editor-desktop-jfx", configuration="jvm8RuntimeElements") )

    jvm8MainImplementation("no.tornado:tornadofx:$version_tornado")
    jvm8MainImplementation("org.fxmisc.richtext:richtextfx:0.10.3")
    jvm8MainImplementation("net.akehurst.language:agl-processor:$version_agl")

}
