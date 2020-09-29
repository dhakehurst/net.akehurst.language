plugins {
   id("net.akehurst.kotlin.kt2ts") version "1.6.0"
}

tasks.withType<ProcessResources>  {
    filesMatching("**/package.json") {
        expand(project.properties)
    }
}

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