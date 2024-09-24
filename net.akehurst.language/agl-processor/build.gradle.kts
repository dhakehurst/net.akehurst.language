plugins {
//    alias(libs.plugins.reflex)
}

dependencies {
    commonMainApi(project(":agl-parser"))
    commonMainApi(project(":agl-regex"))
    commonMainApi(project(":collections")) //TODO merge with kotlinx collections

    // commonMainApi(project(":type-model"))
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

   // macosArm64()

    sourceSets {
        commonTest.configure {
            // add language repository so we can test the grammars with specific sentences here
            resources.srcDir(projectDir.resolve("../language-repository/languages"))
        }
    }
}

/*
kotlinxReflect {
    forReflectionMain.set(
        listOf(
            "net.akehurst.language.api.language.base.*",
            "net.akehurst.language.agl.language.base.*",
            "net.akehurst.language.api.language.grammar.*",
            "net.akehurst.language.agl.language.grammar.asm.*",
            "net.akehurst.language.typemodel.api.*",
            "net.akehurst.language.typemodel.simple.*",
            "net.akehurst.language.api.grammarTypeModel.*",
            "net.akehurst.language.agl.grammarTypeModel.*",
            "net.akehurst.language.api.language.expressions.*",
            "net.akehurst.language.agl.language.expressions.asm.*",
            "net.akehurst.language.api.language.reference.*",
            "net.akehurst.language.agl.language.reference.asm.*",
        )
    )
}
*/

exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.agl.Agl",
            "net.akehurst.language.api.**",
            "net.akehurst.language.base.api.**",
            "net.akehurst.language.style.api.**",

            "net.akehurst.language.typemodel.api.**",
            "net.akehurst.language.agl.regex.**",
            "net.akehurst.language.agl.scanner.**",
            "net.akehurst.language.agl.processor.**",
            "net.akehurst.language.agl.language.**",
            "net.akehurst.language.typemodel.simple.**",
            "net.akehurst.language.asm.simple.**",
            "net.akehurst.language.agl.syntaxAnalyser.**",
            "net.akehurst.language.agl.semanticAnalyser.**",
            "net.akehurst.language.agl.sppt.**",
            "net.akehurst.language.agl.grammarTypeModel.**"
//            "net.akehurst.language.agl.default.**"
        )
    )
}



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