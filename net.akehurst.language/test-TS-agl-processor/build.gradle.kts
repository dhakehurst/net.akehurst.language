import java.nio.file.Files
import java.nio.file.Path

plugins {
    alias(libs.plugins.jsIntegration)
}

dependencies {
    //jsMainImplementation(project(":agl-processor"))
    //jsMainImplementation(npm())
}
/*
val dep_agl_proc:String = rootProject.subprojects.first { it.name=="agl-processor" }.layout.buildDirectory.dir("dist/js/developmentLibrary").get().toString()
val prod_agl_proc:String = rootProject.subprojects.first { it.name=="agl-processor" }.layout.buildDirectory.dir("dist/js/productionLibrary").get().toString()
kotlin.js("js").compilations["main"].packageJson {
    if (Files.exists(Path.of(dep_agl_proc))) {
        dependencies["net.akehurst.language-agl-processor"] = dep_agl_proc
    } else if(Files.exists(Path.of(prod_agl_proc))) {
        dependencies["net.akehurst.language-agl-processor"] = prod_agl_proc
    }
}
*/

configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    js("js", IR) {
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target.set("es2015")
        }
        nodejs()
        browser()
    }

    sourceSets {
        jsMain {
           // resources.srcDirs(jsSrcDir)
        }
    }
}

val jsSrcDir = project.layout.projectDirectory.dir("ts")
val jsOutDir = project.layout.projectDirectory.dir("ts/out")
//val jsOutDir = project.layout.buildDirectory.dir("dist/js/developmentLibrary/tsout")

jsIntegration {
    nodeSrcDirectory.set(jsSrcDir)
    nodeOutDirectory.set(jsOutDir)

    productionCommand.set(mapOf("tscProd" to "run tsc -p ${jsSrcDir} --outDir ${jsOutDir}"))
    developmentCommand.set(mapOf(
        "tscDev" to "run tsc -p ${jsSrcDir} --outDir ${jsOutDir}",
        "test" to "node out/run_all.mjs"
    ))
}

tasks.withType<PublishToMavenLocal> {
    onlyIf { false }
}