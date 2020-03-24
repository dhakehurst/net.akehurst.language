import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput

val version_agl:String by project

dependencies {

    commonMainApi(project(":technology-agl-editor-common"))
    commonMainApi("net.akehurst.language:agl-processor:$version_agl")
}

kotlin {
    js("js") {
        nodejs()
        browser {
            webpackTask {
                output.libraryTarget = KotlinWebpackOutput.Target.SELF
            }
        }
    }
}
