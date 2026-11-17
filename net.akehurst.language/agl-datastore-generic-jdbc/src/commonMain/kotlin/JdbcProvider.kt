package net.akehurst.datastore.generic.jdbc

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastorePlatform
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema

/**
 * Provides JdbcDatastores: datastores over a relational database, reached through JDBC.
 * The types are computed from the database metadata: one data type per table (and view), one property per column,
 * and associations from foreign keys. The provider is in common code, so it can be registered (and its schema shown) on every platform,
 * but its datastores run on the JVM only; the application adds the JDBC driver of its database.
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
        const val CONFIG_KEYS = "config.keys"

        const val DEFAULT_FETCH_SIZE = 500L

        // --- access ---
        const val ACCESS_TABLES = "access.tables"
        const val ACCESS_WRITE_MODE = "access.write-mode"

        const val WRITE_MODE_UPSERT = "upsert"
        const val WRITE_MODE_INSERT = "insert"
        const val WRITE_MODE_UPDATE = "update"
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
                parameter(CONFIG_KEYS, "Keys", optional = true) {
                    type("List")
                    description("Key columns for tables or views that have no primary key, one 'NAME: COLUMN, COLUMN' per line. A row's identity is then its key values; a table (not a view) with a key can also be written and deleted.")
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
            parameter(ACCESS_TABLES, "Tables", optional = true) {
                type("List")
                description("The tables to read, one per line, in the order they are read: 'TABLE', or 'TABLE WHERE <SQL condition>'. Each row is an item, typed as its table's type. Not used by write.")
            }
            parameter(ACCESS_WRITE_MODE, "Write mode", optional = true) {
                type("String")
                default(WRITE_MODE_UPSERT)
                description("How write stores a row: 'upsert' (update the row with its primary key, else insert it), 'insert' (an existing key is an error) or 'update' (a missing key is an error). Not used by read.")
            }
        }
    }

    /** override to provide a custom datastore, e.g. a subclass that overrides provenanceFor */
    /** JDBC is a Java API */
    override val supportedPlatforms: Set<DatastorePlatform> = setOf(DatastorePlatform.JVM)

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore {
        checkSupportedOnCurrentPlatform()
        return createJdbcDatastore(datastoreIdentity, configuration)
    }

}

/** a JdbcDatastore on the JVM; throws on other platforms (JdbcProvider.provide checks the platform first) */
internal expect fun createJdbcDatastore(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore
