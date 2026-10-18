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
    }

    override val identity get() = IDENTITY

    override val configuration = datastoreProviderConfiguration {
        datastore {
            parameter(CONFIG_FS_ROOT, "Grammar String", false) {
                type("String")
            }
        }

        access {

        }
    }

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        FilesystemDatastore(datastoreIdentity, configuration)

}

