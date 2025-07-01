plugins {
    alias(libs.plugins.exportPublic)
    alias(libs.plugins.reflex)
}

dependencies {
    commonMainImplementation(project(":agl-regex"))
    commonMainImplementation(project(":collections"))

    commonMainApi(libs.nak.kotlinx.reflect) // needed for KotlinxReflect generated code
    commonMainApi(libs.nak.kotlinx.collections) // for MutableStack
}

//  since change in Kotlin compiler, can't see transitive deps in module (without additional work yet done
// thus we get each module to generate KotlinxReflect for itself - to fix in future FIXME
kotlinxReflect {
    forReflectionMain.set(
        listOf(
            "net.akehurst.language.sppt.**",
            "net.akehurst.language.sentence.**",
            "net.akehurst.language.issues.**",
            "net.akehurst.language.scanner.**",
            "net.akehurst.language.parser.**",

            "net.akehurst.language.agl.runtime.structure.**"
        )
    )
}

/*
exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.issues.api.**",
            "net.akehurst.language.sentence.api.**",
            "net.akehurst.language.sentence.common.**",
            "net.akehurst.language.scanner.api.**",
            "net.akehurst.language.sppt.api.**",
            "net.akehurst.language.sppt.treedata.**",
            "net.akehurst.language.parser.api.**",
            //"net.akehurst.language.parser.runtime.**",

            "net.akehurst.language.issues.ram.**"
        )
    )
}
*/

kotlin {
    js("js") {
        binaries.library()
        generateTypeScriptDefinitions()
    }
}

tasks.forEach {
    if (it.name.startsWith("publish")) {
        it.doFirst {
            check(file("src/commonMain/kotlin/util/debug.kt").readText().contains("const val CHECK = false")) { "To publish, must set Debug.CHECK = false" }
        }
    }
}