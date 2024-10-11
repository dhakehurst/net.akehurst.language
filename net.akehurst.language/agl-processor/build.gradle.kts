plugins {
    alias(libs.plugins.reflex)
}

dependencies {
    commonMainApi(project(":agl-parser"))
    commonMainApi(project(":agl-regex"))
    commonMainApi(project(":collections")) //TODO merge with kotlinx collections

    commonMainApi(libs.nak.kotlinx.reflect) // needed for KotlinxReflect generated code
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

//  since change in Kotlin compiler, can't see transitive deps in module (without additional work yet done
// thus we get each module to generate KotlinxReflect for itself - to fix in future FIXME
kotlinxReflect {
    forReflectionMain.set(
        listOf(
            "net.akehurst.language.base.**",
            "net.akehurst.language.grammar.**",
            "net.akehurst.language.typemodel.**",
            "net.akehurst.language.grammarTypemodel.*",
            "net.akehurst.language.asm.**",
            "net.akehurst.language.expressions.**",
            "net.akehurst.language.reference.**",

            "net.akehurst.language.api.processor.**",
            "net.akehurst.language.agl.processor.ProcessOptionsDefault"
        )
    )
}

/*
exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.agl.Agl",
            "net.akehurst.language.api.**",
            "net.akehurst.language.base.api.**",
            "net.akehurst.language.base.asm.**",
            "net.akehurst.language.grammar.api.**",
            "net.akehurst.language.grammar.asm.**",
            "net.akehurst.language.style.api.**",
            "net.akehurst.language.style.asm.**",

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