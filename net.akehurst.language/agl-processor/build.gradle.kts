plugins {

}

dependencies {
    commonMainApi(project(":type-model"))

    jvm8TestImplementation(project(":type-model", configuration = "jvm8TestFixture"))
    jsTestImplementation(project(":type-model", configuration = "jsTestFixture"))
    //jvm8TestImplementation(group = "$group", name = "type-model-jvm8", version = "$version", classifier = "testFixture")
    //jsTestImplementation(group = "$group", name = "type-model-js", version = "$version", classifier = "testFixture")
}

kotlin {
    js("js") {
        binaries.library()
        generateTypeScriptDefinitions()
        compilations["main"].packageJson {
            customField(
                "author", mapOf(
                    "name" to "Dr. David H. Akehurst",
                    "email" to "dr.david.h@akehurst.net",
                    "url" to "https://medium.com/@dr.david.h.akehurst"
                )
            )
            customField("license", "Apache-2.0")
            customField("keywords", listOf("parser", "grammar", "langauge", "dsl", "agl"))
            customField("homepage", "https://github.com/dhakehurst/net.akehurst.language")
            customField("description:", "Generic Language (DSL) support, (parser, syntax-analyser, formatter, processor, etc), built using Kotlin multiplatform")
        }
    }
}

exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.api.**",
            "net.akehurst.language.agl.regex.**",
            "net.akehurst.language.agl.processor.**",
            "net.akehurst.language.agl.grammar.**",
            "net.akehurst.language.agl.syntaxAnalyser.**",
            "net.akehurst.language.agl.sppt.**",
        )
    )
}

tasks.forEach {
    if (it.name.startsWith("publish")) {
        it.doFirst {
            check(file("src/commonMain/kotlin/util/debug.kt").readText().contains("const val CHECK = false")) { "To publish, must set Debug.CHECK = false" }
        }
    }
}

/*
tasks.named<Copy>("jsProductionLibraryCompileSync") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<Copy>("jsDevelopmentLibraryCompileSync") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
*/
/*
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

*/