plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.5.2"
}

val version_agl:String by project
val version_ace:String = "1.4.8"
dependencies {

    //nodeKotlin("net.akehurst.language:agl-processor:$version_agl")

}

val srcDir = project.layout.projectDirectory.dir("src/ng-workspace")
val outDir = project.layout.buildDirectory.dir("angular")

// use newer version of node and yarn
project.rootProject.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
    nodeVersion = "13.7.0"
}

kt2ts {
    nodeSrcDirectory.set(srcDir)
    nodeOutDirectory.set(outDir)

    nodeBuildCommand.set(
            // building an ng library does not support --outputPath
            if (project.hasProperty("prod")) {
                listOf("ng", "build", "--prod")//, "--outputPath=${outDir.get()}/dist")
            } else {
                listOf("ng", "build")//, "--outputPath=${outDir.get()}/dist")
            }
    )
}