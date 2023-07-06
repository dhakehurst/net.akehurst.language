import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
	kotlin("multiplatform")
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
val version_agl:String by project

kotlin {
	jvm("jvm8") {
		compilations {
			mainRun {
				mainClass.set("net.akehurst.language.comparisons.agl.MainKt")
			}
			val main by getting {
				compilerOptions.configure {
					languageVersion.set(kotlin_languageVersion)
					apiVersion.set(kotlin_apiVersion)
					jvmTarget.set(jvmTargetVersion)
				}
			}
			val test by getting {
				compilerOptions.configure {
					languageVersion.set(kotlin_languageVersion)
					apiVersion.set(kotlin_apiVersion)
					jvmTarget.set(jvmTargetVersion)
				}
			}
		}
	}
	js("js", IR) {
		binaries.executable()
		generateTypeScriptDefinitions()
		tasks.withType<KotlinJsCompile>().configureEach {
			kotlinOptions {
			//	moduleKind = "es"
			//	useEsClasses = true
			}
		}
		nodejs {
		}
		browser {
		}
	}
}

dependencies {
	"commonMainImplementation"(kotlin("test"))
	"commonMainImplementation"(kotlin("test-annotations-common"))
	"commonMainImplementation"("net.akehurst.language:agl-processor:$version_agl")

	"commonMainImplementation"(project(":common"))
	"jvm8TestImplementation"(project(":common"))
	//"jvm8TestImplementation"("net.akehurst.language:agl-processor-jvm8:$version_agl")

}