package net.akehurst.datastore.filesystem

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderConfiguration

class FilesystemProvider : DatastoreProvider {

    companion object {
        val IDENTITY = DatastoreProviderIdentity("net.akehurst.datastore.filesystem.FilesystemProvider")
        const val CONFIG_FS_ROOT = "config.fs-root"

        const val ACCESS_GLOB = "access.glob"
    }

    override val identity get() = IDENTITY

    override val configuration = datastoreProviderConfiguration {
        datastore {
            parameter(CONFIG_FS_ROOT, "Filesystem Root", false) {
                type("String")
            }
        }

        access {
            parameter(ACCESS_GLOB, "Glob", optional = true) {
                type("String")
                description("Glob to define the relevant files under the given Filesystem Root.")
            }
        }
    }

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        FilesystemDatastore(datastoreIdentity, configuration)

}

