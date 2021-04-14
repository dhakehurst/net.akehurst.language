plugins {
    kotlin("multiplatform") version ("1.4.32")
}

val version_agl:String by project

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
}

dependencies {

    "jvm8MainImplementation"("iguana:iguana:0.0.1-SNAPSHOT")

    //"commonMainImplementation"(kotlin("stdlib"))
    "commonTestImplementation"(kotlin("test"))
    "commonTestImplementation"(kotlin("test-annotations-common"))

    //"jvm8MainImplementation"(kotlin("stdlib-jdk8"))
    "jvm8TestImplementation"(kotlin("test-junit"))
    "jvm8TestImplementation"("junit:junit:4.12") {
        version { strictly("4.12") }
    }


    "jvm8TestImplementation"(project(":common"))
}
