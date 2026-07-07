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
   alias(libs.plugins.kotlin) apply false
   alias(libs.plugins.dokka) apply false
   alias(libs.plugins.buildconfig) apply false
   alias(libs.plugins.exportPublic) apply false
   alias(libs.plugins.vanniktech.maven.publish) apply false
}
project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")
