
dependencies {
    commonMainImplementation(project(":agl-regex"))
    commonMainImplementation(project(":collections"))
}

exportPublic {
    exportPatterns.set(
        listOf(
            "net.akehurst.language.issues.api.**",
            "net.akehurst.language.sppt.api.**",
            "net.akehurst.language.parser.api.**",
            "net.akehurst.language.parser.runtime.**",

            "net.akehurst.language.issues.ram.**"
        )
    )
}

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