package net.akehurst.datastore.text

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema

open class AglTextProvider : DatastoreProvider {

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

        /**
         * path of a folder that holds the language definition, as files found by their extension (any file name):
         * see the FILE_EXTENSION_* constants. Only the grammar file is required; at most one file of each kind.
         * When given, the definition strings (config.grammar, config.format, config.crossReference) are not used.
         */
        const val CONFIG_LANGUAGE_DIRECTORY = "config.lang-dir"

        /** extension of the grammar file in a language directory (required) */
        const val FILE_EXTENSION_GRAMMAR = "agl-grm"
        /** extension of the types file in a language directory (optional) */
        const val FILE_EXTENSION_TYPES = "agl-typ"
        /** extension of the asm-transform file in a language directory (optional) */
        const val FILE_EXTENSION_TRANSFORM = "agl-trf"
        /** extension of the cross-reference file in a language directory (optional) */
        const val FILE_EXTENSION_CROSS_REFERENCE = "agl-ref"
        /** extension of the style file in a language directory (optional) */
        const val FILE_EXTENSION_STYLE = "agl-sty"
        /** extension of the format file in a language directory (optional) */
        const val FILE_EXTENSION_FORMAT = "agl-fmt"

        /** input type for AglTextDatastore.read, for use as a DatastoreConnection targetType */
        const val PARSABLE_UNIT_TYPE = "net.akehurst.datastore.text.ParsableUnit"

    }

    override val identity: DatastoreProviderIdentity get() = IDENTITY

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
                    parameter(CONFIG_LANGUAGE_DIRECTORY, "Language Directory", false) {
                        type("String")
                        description(
                            "Path of a folder that holds the language definition, one file of each kind, found by extension (any file name): " +
                                    "grammar (.$FILE_EXTENSION_GRAMMAR, required), types (.$FILE_EXTENSION_TYPES), asm-transform (.$FILE_EXTENSION_TRANSFORM), " +
                                    "cross-references (.$FILE_EXTENSION_CROSS_REFERENCE), style (.$FILE_EXTENSION_STYLE) and format (.$FILE_EXTENSION_FORMAT). " +
                                    "Without a cross-reference file, no references are resolved."
                        )
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

    /** override to provide a custom datastore, e.g. a subclass that overrides provenanceFor */
    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore  =
        AglTextDatastore(datastoreIdentity,configuration)

}