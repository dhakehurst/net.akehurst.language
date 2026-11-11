package net.akehurst.datastore.generic.zip

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.datastoreProviderSchema

/**
 * Provides ZipDatastores: read only datastores that give the entries of zip archives
 * (e.g. .zip, ReqIF .reqifz, Cameo .mdzip) that the previous part of a composition reads.
 */
open class ZipProvider : DatastoreProvider {

    companion object {
        val IDENTITY = DatastoreProviderIdentity("net.akehurst.datastore.generic.zip.ZipProvider")

        /** the type a composition connects to: construct it with 'bytes' (Binary) */
        const val ZIP_ARCHIVE_TYPE = "zip.ZipArchive"
        const val ZIP_ENTRY_TYPE = "zip.ZipEntry"

        // --- datastore: safety limits (advanced) ---
        const val CONFIG_MAX_ENTRIES = "config.max-entries"
        const val CONFIG_MAX_ENTRY_SIZE = "config.max-entry-size"
        const val CONFIG_MAX_TOTAL_SIZE = "config.max-total-size"
        const val CONFIG_MAX_COMPRESSION_RATIO = "config.max-compression-ratio"

        const val DEFAULT_MAX_ENTRIES = 100_000L
        const val DEFAULT_MAX_ENTRY_SIZE = 1024L * 1024L * 1024L // 1 GiB
        const val DEFAULT_MAX_TOTAL_SIZE = 4L * 1024L * 1024L * 1024L // 4 GiB
        const val DEFAULT_MAX_COMPRESSION_RATIO = 200L

        /** the compression ratio is only checked for entries bigger than this, as small entries of e.g. repeated text can have a high ratio */
        const val COMPRESSION_RATIO_MIN_SIZE = 1024L * 1024L // 1 MiB

        // --- access ---
        const val ACCESS_GLOB = "access.glob"
        const val ACCESS_EXCLUDE_GLOB = "access.exclude-glob"
        const val ACCESS_INCLUDE_DIRECTORIES = "access.include-directories"
        const val ACCESS_RECURSE_NESTED = "access.recurse-nested"
        const val ACCESS_MAX_NESTING_DEPTH = "access.max-nesting-depth"

        const val DEFAULT_MAX_NESTING_DEPTH = 3L
    }

    override val identity: DatastoreProviderIdentity get() = IDENTITY

    override val schema = datastoreProviderSchema {
        datastore {
            group("Limits", advanced = true) {
                parameter(CONFIG_MAX_ENTRIES, "Maximum entries", optional = true) {
                    type("Integer")
                    default(DEFAULT_MAX_ENTRIES)
                    description("An archive with more entries is not read; it gets an error issue.")
                }
                parameter(CONFIG_MAX_ENTRY_SIZE, "Maximum entry size", optional = true) {
                    type("Integer")
                    default(DEFAULT_MAX_ENTRY_SIZE)
                    description("Uncompressed size in bytes. A bigger entry gets an error issue and its content is not read.")
                }
                parameter(CONFIG_MAX_TOTAL_SIZE, "Maximum total size", optional = true) {
                    type("Integer")
                    default(DEFAULT_MAX_TOTAL_SIZE)
                    description("Uncompressed size in bytes of all entries of one archive. A bigger archive is not read; it gets an error issue.")
                }
                parameter(CONFIG_MAX_COMPRESSION_RATIO, "Maximum compression ratio", optional = true) {
                    type("Integer")
                    default(DEFAULT_MAX_COMPRESSION_RATIO)
                    description("Uncompressed size / compressed size, checked for entries over 1 MiB. An entry over it gets an error issue and its content is not read.")
                }
            }
        }

        access {
            parameter(ACCESS_GLOB, "Glob", optional = true) {
                type("String")
                default("**")
                description("Glob over the entry paths inside the archive, e.g. '**/*.reqif'.")
            }
            parameter(ACCESS_EXCLUDE_GLOB, "Exclude glob", optional = true) {
                type("String")
                description("Glob over the entry paths of entries to leave out, e.g. '**/diagrams/**'.")
            }
            group("Advanced", advanced = true) {
                parameter(ACCESS_INCLUDE_DIRECTORIES, "Include directories", optional = true) {
                    type("Boolean")
                    default(false)
                    description("Also give the directory entries of the archive.")
                }
                parameter(ACCESS_RECURSE_NESTED, "Read nested archives", optional = true) {
                    type("Boolean")
                    default(false)
                    description("Also give the entries of entries that are themselves zip archives (found by content).")
                }
                parameter(ACCESS_MAX_NESTING_DEPTH, "Maximum nesting depth", optional = true) {
                    type("Integer")
                    default(DEFAULT_MAX_NESTING_DEPTH)
                    description("How many levels of nested archives are read, when nested archives are read.")
                }
            }
        }
    }

    /** override to provide a custom datastore, e.g. a subclass that overrides provenanceFor */
    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        ZipDatastore(datastoreIdentity, configuration)

}
