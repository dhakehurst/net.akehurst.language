package net.akehurst.datastore.text

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema

class AglTextProvider : DatastoreProvider {

    companion object {
        val IDENTITY = DatastoreProviderIdentity("net.akehurst.datastore.text.AglTextProvider")
        const val CONFIG_GRAMMAR_STR = "config.grammar"
        const val CONFIG_FORMAT_STR = "config.format"

        const val CONFIG_LANGUAGE_DIRECTORY = "config.lang-dir"

        /** input type for AglTextDatastore.read, for use as a DatastoreConnection targetType */
        const val PARSABLE_UNIT_TYPE = "net.akehurst.datastore.text.ParsableUnit"

    }

    override val identity get() = IDENTITY

    override val schema = datastoreProviderSchema {
        datastore {
            choice {
                alternative("String Definitions") {
                    parameter(CONFIG_GRAMMAR_STR, "Grammar String", false) {
                        type("String")
                    }
                    parameter(CONFIG_FORMAT_STR, "Format String", false) {
                        type("String")
                    }
                }
                alternative("Language Directory") {
                    parameter(CONFIG_FORMAT_STR, "Format String", false) {
                        type("String")
                    }
                }
            }
        }

        access {

        }
    }

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore  =
        AglTextDatastore(datastoreIdentity,configuration)

}