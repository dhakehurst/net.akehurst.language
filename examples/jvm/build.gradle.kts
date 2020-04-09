plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val version_agl:String by project
dependencies {


    implementation("net.akehurst.language:agl-processor:$version_agl")
    testImplementation("junit:junit:4.12")
}
