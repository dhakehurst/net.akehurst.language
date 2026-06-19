plugins {
    alias(libs.plugins.kotlin)
}

// do not publish
tasks.withType<AbstractPublishToMaven> { onlyIf { false } }

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("net\\.akehurst.+")
        }
        mavenContent {
            snapshotsOnly()
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

group = rootProject.name
version = libs.versions.project.get()
project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")

kotlin {
    applyDefaultHierarchyTemplate()
    jvm {

    }
    sourceSets {
        jvmTest {
            dependencies {
                implementation(project(":agl-processor"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}
