/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
	kotlin("multiplatform") version ("1.9.21") apply false
}

allprojects {

	val version_project: String by project
	val group_project = "${rootProject.name}"

	group = group_project
	version = version_project

	buildDir = File(rootProject.projectDir, ".gradle-build/${project.name}")

}

subprojects {

	repositories {
		mavenLocal {
			content {
				includeGroupByRegex("net\\.akehurst.+")
			}
		}
		mavenCentral()
	}

}