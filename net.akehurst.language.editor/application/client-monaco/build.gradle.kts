dependencies {

    //jsMainImplementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    jsMainImplementation(project(":technology-tabview"))
    jsMainImplementation(project(":technology-agl-monaco"))
}


/*
plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.5.0"
}

val version_agl:String by project
val version_ace:String = "1.4.8"
dependencies {

    nodeKotlin("net.akehurst.language:agl-processor:$version_agl")

    //jsMainImplementation("net.akehurst.language:agl-processor:$version_agl")
    //jsMainImplementation(npm("ace-builds", version_ace))
}

val srcDir = project.layout.projectDirectory.dir("src/typescript")
val outDir = project.layout.buildDirectory.dir("typescript")

kt2ts {
    nodeSrcDirectory.set(srcDir)
    nodeOutDirectory.set(outDir)

    nodeBuildCommand.set(
            if (project.hasProperty("prod")) {
                listOf("webpack", "--output=${outDir.get()}/main.js")
            } else {
                listOf("webpack", "--mode=development", "--output=${outDir.get()}/main.js")
            }
    )
}
 */