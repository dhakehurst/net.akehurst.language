package net.akehurst.datastore.generic.jdbc

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema

/**
 * Provides JdbcDatastores: datastores over a relational database, reached through JDBC.
 * The types are computed from the database metadata: one data type per table (and view), one property per column,
 * and associations from foreign keys. JVM only; the application adds the JDBC driver of its database.
 */
open class JdbcProvider : DatastoreProvider {

    companion object {
        val IDENTITY = DatastoreProviderIdentity("net.akehurst.datastore.generic.jdbc.JdbcProvider")

        // --- datastore ---
        const val CONFIG_URL = "config.url"
        const val CONFIG_USER = "config.user"
        const val CONFIG_PASSWORD = "config.password"
        const val CONFIG_SCHEMA = "config.schema"
        const val CONFIG_TABLES = "config.tables"
        const val CONFIG_INCLUDE_VIEWS = "config.include-views"
        const val CONFIG_DRIVER_PROPERTIES = "config.driver-properties"
        const val CONFIG_FETCH_SIZE = "config.fetch-size"

        const val DEFAULT_FETCH_SIZE = 500L

        // --- access ---
        const val ACCESS_TABLES = "access.tables"
    }

    override val identity: DatastoreProviderIdentity get() = IDENTITY

    override val schema = datastoreProviderSchema {
        datastore {
            parameter(CONFIG_URL, "URL", optional = false) {
                type("String")
                description("The JDBC URL of the database, e.g. 'jdbc:oracle:thin:@//host:1521/SERVICE' or 'jdbc:postgresql://host/db'. The driver for it must be on the classpath.")
            }
            parameter(CONFIG_USER, "User", optional = true) {
                type("String")
                description("The user to log in as. Leave it out if the URL or the driver properties give it.")
            }
            parameter(CONFIG_PASSWORD, "Password", optional = true) {
                type("String")
                sensitive()
                description("The password of the user. Give it as a secret reference.")
            }
            parameter(CONFIG_SCHEMA, "Schema", optional = true) {
                type("String")
                description("The database schema whose tables are given types, e.g. 'HR' in Oracle or 'public' in PostgreSQL. Without it, the connection's current schema. The types are in a namespace of this name.")
            }
            parameter(CONFIG_TABLES, "Tables", optional = true) {
                type("List")
                description("Names of the tables to give types, one per line; globs such as 'EMP*' are allowed, and names are matched ignoring case. Without it, all tables of the schema.")
            }
            group("Advanced", advanced = true) {
                parameter(CONFIG_INCLUDE_VIEWS, "Include views", optional = true) {
                    type("Boolean")
                    default(true)
                    description("Also give types for the views of the schema (that match 'Tables').")
                }
                parameter(CONFIG_DRIVER_PROPERTIES, "Driver properties", optional = true) {
                    type("List")
                    description("Extra properties for the JDBC driver, one 'key=value' per line.")
                }
                parameter(CONFIG_FETCH_SIZE, "Fetch size", optional = true) {
                    type("Integer")
                    default(DEFAULT_FETCH_SIZE)
                    description("How many rows the driver fetches from the database at a time.")
                }
            }
        }

        access {
            parameter(ACCESS_TABLES, "Tables", optional = false) {
                type("List")
                description("The tables to read, one per line, in the order they are read. Each row is an item, typed as its table's type.")
            }
        }
    }

    /** override to provide a custom datastore, e.g. a subclass that overrides provenanceFor */
    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        JdbcDatastore(datastoreIdentity, configuration)

}
