plugins {
    id("project-conventions")
    alias(libs.plugins.exportPublic)
}

// JDBC is a Java API, so the datastore is JVM only. JdbcProvider is in commonMain, so it can be registered on every platform;
// on JS and wasmJs its provide() throws DatastorePlatformNotSupportedException.
// No JDBC driver is a dependency: the application adds the driver for its database (e.g. com.oracle.database.jdbc:ojdbc11).
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":agl-datastore-common"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.nak.kotlinx.utils)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.h2)
            }
        }
    }
}
