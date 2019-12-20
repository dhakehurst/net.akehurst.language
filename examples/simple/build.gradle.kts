plugins {
    kotlin("multiplatform") version ("1.3.61")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
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
        browser()

    }
}

dependencies {

    commonMainImplementation(kotlin("stdlib"))
    commonMainImplementation("net.akehurst.language:agl-processor:3.3.1")

    commonTestImplementation(kotlin("test"))
    commonTestImplementation(kotlin("test-annotations-common"))

    "jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    "jvm8TestImplementation"(kotlin("test-junit"))

    "jsMainImplementation"(kotlin("stdlib-js"))
    "jsTestImplementation"(kotlin("test-js"))
}
