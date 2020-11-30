plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.6.0"
    id("java-library")
    jacoco
}

tasks.withType<ProcessResources> {
    filesMatching("**/package.json") {
        expand(project.properties)
    }
}
/*
kt2ts {
    jvmTargetName.set("jvm8")
    jsTargetName.set("js")
    classPatterns.set(listOf(
            "net.akehurst.language.api.syntaxAnalyser.*",
            "net.akehurst.language.api.semanticAnalyser.*",
            "net.akehurst.language.api.grammar.*",
            "net.akehurst.language.api.parser.*",
            "net.akehurst.language.api.processor.*",
            "net.akehurst.language.api.sppt.*",
            "net.akehurst.language.api.style.*",
            "net.akehurst.language.agl.processor.Agl"
    ))
}
*/
/*
tasks {
    val dokkaJavadoc by creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/dokka"
        multiplatform {
            val jvm8 by creating {
                targets = listOf("JVM")
                platform = "jvm"
                jdkVersion = 8
                noJdkLink = true
            }
        }
    }
}*/

jacoco {
    toolVersion = "0.8.5"
}
tasks.jvm8Test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.withType<JacocoReport>{
    val coverageSourceDirs = arrayOf("src/commonMain", "src/jvm8Main")
    val classFiles = File("${buildDir}/classes/kotlin/jvm8/").walkBottomUp().toSet()

    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(files(coverageSourceDirs))
    executionData.setFrom(files("${buildDir}/jacoco/jvm8Test.exec"))
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

