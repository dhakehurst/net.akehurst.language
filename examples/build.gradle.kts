allprojects {

    val version_project: String by project
    val group_project = "net.akehurst.language.examples"

    group = group_project
    version = version_project

    buildDir = File(rootProject.projectDir, ".gradle-build/${project.name}")

}