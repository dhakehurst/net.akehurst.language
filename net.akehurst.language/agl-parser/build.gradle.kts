import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    id("project-conventions")
    alias(libs.plugins.exportPublic)
//    alias(libs.plugins.reflex)
}


kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":agl-regex"))
                implementation(project(":collections"))

                api(libs.nak.kotlinx.reflect) // needed for KotlinxReflect generated code
                api(libs.nak.kotlinx.collections) // for MutableStack
            }
        }
        jvmTest {
            dependencies {
                // JMH (Java Microbenchmark Harness). Used by ParserBenchmarks.kt
                // and the `jmh` Gradle task below. We deliberately do NOT use
                // the `me.champeau.jmh` plugin: it doesn't understand KMP
                // source sets cleanly. Instead we drive JMH ourselves with
                // two JavaExec tasks (generator + runner).
                implementation("org.openjdk.jmh:jmh-core:1.37")
                implementation("org.openjdk.jmh:jmh-generator-bytecode:1.37")
            }
        }
    }
}
/*
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
*/
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


tasks.forEach {
    if (it.name.startsWith("publish")) {
        it.doFirst {
            check(file("src/commonMain/kotlin/util/debug.kt").readText().contains("const val CHECK = false")) { "To publish, must set Debug.CHECK = false" }
        }
    }
}
