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

        /** optional cross-reference definition (scopes and references) for the grammar; without it no references are resolved */
        const val CONFIG_CROSS_REFERENCE_STR = "config.crossReference"

        /**
         * access parameter: true resolves references across all the units of one read (one context for the read);
         * false (the default) resolves them within each unit. When true, the read collects all its input before it emits anything.
         */
        const val ACCESS_RESOLVE_ACROSS_UNITS = "access.resolve-across-units"

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
                    parameter(CONFIG_CROSS_REFERENCE_STR, "Cross-Reference String", true) {
                        type("String")
                        description("Scopes and references of the language. Without it, no references are resolved.")
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
            parameter(ACCESS_RESOLVE_ACROSS_UNITS, "Resolve references across units", optional = true) {
                type("Boolean")
                description("true: references resolve across all the units of one read, and the read waits for all its input. false (default): references resolve within each unit.")
            }
        }
    }

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore  =
        AglTextDatastore(datastoreIdentity,configuration)

}