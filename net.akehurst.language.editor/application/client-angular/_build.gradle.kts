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
    id("net.akehurst.kotlin.kt2ts") version("1.5.2")
}

dependencies {


}

val ngSrcDir = project.layout.projectDirectory.dir("src/angular")
val ngOutDir = project.layout.buildDirectory.dir("angular")

kt2ts {
    nodeSrcDirectory.set(ngSrcDir)
    nodeOutDirectory.set(ngOutDir)
    nodeBuildCommand.set(
            if (project.hasProperty("prod")) {
                listOf("ng", "build", "--prod", "--outputPath=${ngOutDir.get()}/dist")
            } else {
                listOf("ng", "build", "--outputPath=${ngOutDir.get()}/dist")
            }
    )
    jvmTargetName.set("jvm8")
}

project.tasks.getByName("jvm8ProcessResources").dependsOn("nodeBuild")
kotlin {
    sourceSets {
        val jvm8Main by getting {
            resources.srcDir(ngOutDir)
        }
    }
}