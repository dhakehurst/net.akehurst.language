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

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()       // for com.android.tools.build
    }

    dependencies {
//        classpath "com.android.tools.build:gradle:$version_abt"   // for com.android.application
        classpath( "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.6")
    }
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
        mavenLocal()
        mavenCentral()
        jcenter()
        maven {
            name ="soywiz"
            url =uri("https://dl.bintray.com/soywiz/soywiz")
        }
    }
    

/*
	bintray {
		user = project.hasProperty('bintrayUser') ? project.property('bintrayUser') : System.getenv('BINTRAY_USER')
		key = project.hasProperty('bintrayApiKey') ? project.property('bintrayApiKey') : System.getenv('BINTRAY_API_KEY')
		publications = ['mavenJava']
		pkg {
			repo = 'maven'
			name = 'net.akehurst.language'
			userOrg = user
			licenses = ['Apache-2.0']
			vcsUrl = 'https://github.com/dhakehurst/net.akehurst.language.git'
			version {
				name = "${project.version}"
				gpg {
                    sign = true
                    passphrase = project.hasProperty('passphrase') ? project.property('passphrase') : System.getenv('BINTRAY_PASSPHRASE')
                }
			}
		}
	}
	*/
}