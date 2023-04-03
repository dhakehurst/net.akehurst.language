plugins {
	kotlin("multiplatform") version ("1.8.10")
}

val version_agl:String by project

kotlin {
	jvm("jvm8") {
		//withJava()
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
}


dependencies {
	"commonTestImplementation"(kotlin("test"))
	"commonTestImplementation"(kotlin("test-annotations-common"))

	"jvm8TestImplementation"(project(":common"))
	"jvm8TestImplementation"("net.akehurst.language:agl-processor-jvm8:$version_agl")

}