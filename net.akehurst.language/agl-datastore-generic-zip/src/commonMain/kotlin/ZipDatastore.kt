package net.akehurst.datastore.generic.zip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.kotlinx.filesystem.DirectoryHandleZip
import net.akehurst.kotlinx.filesystem.FileHandleZip
import net.akehurst.kotlinx.filesystem.ZipFileSystem
import net.akehurst.kotlinx.filesystem.ZipOptions
import net.akehurst.kotlinx.filesystem.api.DirectoryHandle
import net.akehurst.kotlinx.filesystem.api.FileHandle
import net.akehurst.kotlinx.filesystem.api.FileMetadata
import net.akehurst.kotlinx.filesystem.api.FileSystemObjectHandle
import net.akehurst.kotlinx.filesystem.api.ZipException
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.kotlinx.issues.ram.IssueHolder
import net.akehurst.kotlinx.text.toRegexFromGlob
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.time.Instant

/** where an issue of a ZipDatastore item is: the entry path inside its archive, or null for the archive as a whole */
data class ZipIssueLocation(
    val entryPath: String?
) : DataItemIssueLocation

/**
 * Provenance of a DataItem read from a ZipDatastore: an entry of an archive.
 * 'archiveIdentity' is the qualified identity of the item that holds the archive (the input item).
 */
data class ZipProvenance(
    val datastoreIdentity: DatastoreIdentity,
    val archiveIdentity: List<DataItemIdentity>,
    val entryPath: String
) : DataItemProvenance

/** a zip archive, held in memory. It is opened (its central directory read) when its entries are read. */
class ZipArchive(val bytes: ByteArray) {
    override fun toString(): String = "ZipArchive(${bytes.size} bytes)"
}

/**
 * An entry of a zip archive. It has the same property names as a filesystem FileHandle
 * ('name', 'extension', 'content.asString', 'content.asBinary'), so the same connect expressions work downstream.
 * Its content is not read until 'content.asBinary' or 'content.asString' is.
 */
class ZipEntry internal constructor(
    /** the archive this entry is in */
    val archive: ZipArchive,
    /** the full '/'-separated path inside the archive, without a leading '/'; a directory's path ends with '/' */
    val path: String,
    /** the kotlinx-filesystem handle of the entry: a FileHandle, or a DirectoryHandle for a directory */
    val handle: FileSystemObjectHandle,
    private val metadata: FileMetadata,
    /** the issues of the item that holds this entry; an error reading the content is added to them */
    internal val issues: IssueHolder<DataItemIssueLocation>,
    /** the entry can be read (not encrypted, a supported compression, within the limits) */
    internal val readable: Boolean,
) {
    /** the last segment of the path */
    val name: String get() = handle.name

    /** the part of the name after its last '.', or "" */
    val extension: String get() = if (name.contains('.')) name.substringAfterLast('.') else ""

    val isDirectory: Boolean get() = handle is DirectoryHandle

    /** uncompressed size in bytes, as declared by the archive */
    val size: Long get() = metadata.size ?: 0L
    val compressedSize: Long get() = metadata.compressedSize ?: 0L

    /** from the extended timestamp field if there is one (UTC), else the MS-DOS time of the entry, taken as UTC; null if neither */
    val lastModified: Instant? get() = metadata.lastModified

    /** CRC-32 of the uncompressed content, as declared by the archive (unsigned). Changes when the content changes. */
    val crc32: Long get() = metadata.crc32 ?: 0L

    val content: ZipEntryContent get() = ZipEntryContent(this)

    /**
     * the uncompressed content, inflated on each call (not kept, so that big entries are not all held in memory).
     * null if it cannot be read; the reason is then an error issue of the entry's item.
     */
    suspend fun readBytes(): ByteArray? {
        val file = handle as? FileHandle ?: return null
        if (readable.not()) return null
        return try {
            file.readBytes()
        } catch (e: ZipException) {
            issues.error(ZipIssueLocation(path), e.message ?: "'$path' cannot be read")
            null
        }
    }

    override fun toString(): String = "ZipEntry($path)"
}

/** The content of a zip entry: 'asBinary' gives its bytes, 'asString' those bytes decoded as UTF-8. Read only. */
class ZipEntryContent(val entry: ZipEntry) {
    suspend fun asBinary(): ByteArray? = entry.readBytes()
    suspend fun asString(): String? = entry.readBytes()?.decodeToString()

    override fun hashCode(): Int = entry.hashCode()
    override fun equals(other: Any?): Boolean = other is ZipEntryContent && other.entry === this.entry
    override fun toString(): String = "ZipEntryContent(${entry.path})"
}

/**
 * A read only datastore whose read takes items holding zip archives (a ZipArchive, or a ByteArray) and gives one
 * item per matching entry. Each entry item's parent is the archive item, and its identity is the entry path,
 * so its qualifiedIdentity is the archive item's followed by the entry path.
 *
 * Archives are read by kotlinx-filesystem's ZipFileSystem. It reduces entry names to plain segments
 * ('..', '.', a leading '/' and drive letters are dropped), so nothing can point outside the archive,
 * and nothing is ever extracted to disk.
 * The archive bytes are in memory; entries are inflated only when their content is read.
 * Input that is not a zip, an archive over the limits, and entries that cannot be read give error issues.
 */
open class ZipDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration
) : DatastoreAbstract(identity, configuration) {

    /** the limits and options of one read */
    protected class ReadOptions(
        val glob: Regex,
        val exclude: Regex?,
        val includeDirectories: Boolean,
        val recurseNested: Boolean,
        val maxNestingDepth: Long,
        val maxEntries: Long,
        val maxEntrySize: Long,
        val maxTotalSize: Long,
        val maxCompressionRatio: Long,
    )

    override suspend fun createTypes(): TypesDomain =
        typesDomain("Zip", true) {
            namespace("zip") {
                data("ZipArchive", implementation = ZipArchive::class) {
                    constructor_ {
                        parameter(setOf(REF, VAL), "bytes", "Binary")
                        execution { args -> ZipArchive(args[0] as ByteArray) }
                    }
                }
                data("ZipEntry", implementation = ZipEntry::class) {
                    propertyOfWithBinding<ZipEntry, String>(setOf(REF, VAL), "name", "String", accessor = { it.name })
                    propertyOfWithBinding<ZipEntry, String>(setOf(REF, VAL), "path", "String", accessor = { it.path })
                    propertyOfWithBinding<ZipEntry, String>(setOf(REF, VAL), "extension", "String", accessor = { it.extension })
                    propertyOfWithBinding<ZipEntry, Boolean>(setOf(REF, VAL), "isDirectory", "Boolean", accessor = { it.isDirectory })
                    propertyOfWithBinding<ZipEntry, Long>(setOf(REF, VAL), "size", "Integer", accessor = { it.size })
                    propertyOfWithBinding<ZipEntry, Long>(setOf(REF, VAL), "compressedSize", "Integer", accessor = { it.compressedSize })
                    propertyOfWithBinding<ZipEntry, Instant?>(setOf(REF, VAL), "lastModified", "Timestamp", isNullable = true, accessor = { it.lastModified })
                    propertyOfWithBinding<ZipEntry, Long>(setOf(REF, VAL), "crc32", "Integer", accessor = { it.crc32 })
                    propertyOfWithBinding<ZipEntry, ZipArchive>(setOf(REF, VAL), "archive", "ZipArchive", accessor = { it.archive })
                    propertyOfWithBinding<ZipEntry, ZipEntryContent>(setOf(REF, VAL), "content", "ZipEntryContent", accessor = { it.content })
                }
                data("ZipEntryContent", implementation = ZipEntryContent::class) {
                    propertyOfWithBinding<ZipEntryContent, String>(
                        setOf(REF, VAL), "asString", "String", isNullable = true,
                        accessorSuspend = { it.asString() }
                    )
                    propertyOfWithBinding<ZipEntryContent, ByteArray>(
                        setOf(REF, VAL), "asBinary", "Binary", isNullable = true,
                        accessorSuspend = { it.asBinary() }
                    )
                }
            }
        }

    protected fun readOptions(accessConfiguration: DatastoreConfiguration): ReadOptions {
        val globStr = accessConfiguration.getAsStringOrNull(ZipProvider.ACCESS_GLOB) ?: "**"
        return ReadOptions(
            glob = globStr.toRegexFromGlob('/'),
            exclude = accessConfiguration.getAsStringOrNull(ZipProvider.ACCESS_EXCLUDE_GLOB)?.takeIf { it.isNotBlank() }?.toRegexFromGlob('/'),
            includeDirectories = accessConfiguration.getAsBooleanOrNull(ZipProvider.ACCESS_INCLUDE_DIRECTORIES) ?: false,
            recurseNested = accessConfiguration.getAsBooleanOrNull(ZipProvider.ACCESS_RECURSE_NESTED) ?: false,
            maxNestingDepth = accessConfiguration.getAsLongOrNull(ZipProvider.ACCESS_MAX_NESTING_DEPTH) ?: ZipProvider.DEFAULT_MAX_NESTING_DEPTH,
            maxEntries = configuration.getAsLongOrNull(ZipProvider.CONFIG_MAX_ENTRIES) ?: ZipProvider.DEFAULT_MAX_ENTRIES,
            maxEntrySize = configuration.getAsLongOrNull(ZipProvider.CONFIG_MAX_ENTRY_SIZE) ?: ZipProvider.DEFAULT_MAX_ENTRY_SIZE,
            maxTotalSize = configuration.getAsLongOrNull(ZipProvider.CONFIG_MAX_TOTAL_SIZE) ?: ZipProvider.DEFAULT_MAX_TOTAL_SIZE,
            maxCompressionRatio = configuration.getAsLongOrNull(ZipProvider.CONFIG_MAX_COMPRESSION_RATIO) ?: ZipProvider.DEFAULT_MAX_COMPRESSION_RATIO,
        )
    }

    /**
     * input: items whose data is a ZipArchive or a ByteArray (the bytes of a zip archive)
     * output: for each input item, an item per entry whose path matches the access glob and not the exclude glob
     * (directories only if asked for), with the input item as parent and the entry path as identity.
     * If the input is not a zip archive, cannot be read, or is over the limits, a single error item
     * (identity 'zipArchive', no data) is output for it instead.
     */
    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        val options = readOptions(accessConfiguration)
        return input.transform { item ->
            when (val self = item.data.self) {
                is ZipArchive -> emitEntries(item, self, options, 0)
                is ByteArray -> emitEntries(item, ZipArchive(self), options, 0)
                else -> emit(archiveErrorItem(item, "The read input to a ZipDatastore must be a ZipArchive or a Binary (the bytes of a zip archive)."))
            }
        }
    }

    private suspend fun FlowCollector<DataItem>.emitEntries(archiveItem: DataItem, archive: ZipArchive, options: ReadOptions, depth: Long) {
        val bytes = archive.bytes
        if (looksLikeZip(bytes).not()) {
            emit(archiveErrorItem(archiveItem, "The input is not a zip archive (it does not start with 'PK\\u0003\\u0004', or 'PK\\u0005\\u0006' for an empty archive)."))
            return
        }
        val entries = try {
            val root = ZipFileSystem.open(bytes, archiveItem.identity.value, ZipOptions(maxEntrySize = options.maxEntrySize))
            walk(root)
        } catch (e: ZipException) {
            emit(archiveErrorItem(archiveItem, e.message ?: "The archive cannot be read."))
            return
        }
        if (entries.size > options.maxEntries) {
            emit(archiveErrorItem(archiveItem, "The archive has ${entries.size} entries, more than the limit of ${options.maxEntries}; it is not read."))
            return
        }
        val withMetadata = entries.map { (path, handle) -> Triple(path, handle, metadataOf(handle)) }
        val total = withMetadata.sumOf { it.third.size ?: 0L }
        if (total > options.maxTotalSize || total < 0) {
            emit(archiveErrorItem(archiveItem, "The entries of the archive are $total bytes uncompressed, more than the limit of ${options.maxTotalSize}; it is not read."))
            return
        }
        val am = accessorMutator()
        for ((path, handle, metadata) in withMetadata) {
            val isDirectory = handle is DirectoryHandle
            val included = (isDirectory.not() || options.includeDirectories) && matches(path, options)
            val nested = options.recurseNested && depth < options.maxNestingDepth && isDirectory.not()
            if (included.not() && nested.not()) continue

            val issues = IssueHolder<DataItemIssueLocation>()
            val readable = checkEntry(path, handle, metadata, options, issues)
            val entry = ZipEntry(archive, path, handle, metadata, issues, readable)
            val entryItem = DataItemDefault(
                parent = archiveItem,
                identity = DataItemIdentity(path),
                data = am.toTypedObject(entry, StdLibDefault.AnyType),
                provenance = provenanceFor(archiveItem, entry),
                issues = issues
            )
            if (included) emit(entryItem)
            if (nested && readable) {
                // a nested archive is found by its content, so the entry is read (within the limits)
                val nestedBytes = entry.readBytes()
                when {
                    null == nestedBytes -> if (included.not()) emit(entryItem) // the reason is in the entry's issues
                    looksLikeZip(nestedBytes) -> emitEntries(entryItem, ZipArchive(nestedBytes), options, depth + 1)
                    else -> Unit
                }
            }
        }
    }

    /** all entries under 'dir', depth first in archive order, with paths relative to the archive root (a directory's path ends with '/') */
    private suspend fun walk(dir: DirectoryHandle): List<Pair<String, FileSystemObjectHandle>> {
        val result = mutableListOf<Pair<String, FileSystemObjectHandle>>()
        suspend fun visit(d: DirectoryHandle, prefix: String) {
            for (h in d.listContent()) {
                when (h) {
                    is DirectoryHandle -> {
                        result.add(Pair(prefix + h.name + "/", h))
                        visit(h, prefix + h.name + "/")
                    }

                    else -> result.add(Pair(prefix + h.name, h))
                }
            }
        }
        visit(dir, "")
        return result
    }

    private suspend fun metadataOf(handle: FileSystemObjectHandle): FileMetadata = when (handle) {
        is FileHandle -> handle.metadata()
        is DirectoryHandleZip -> FileMetadata(size = 0L, lastModified = handle.lastModified)
        else -> FileMetadata.UNKNOWN
    }

    /** true if the bytes start like a zip archive: a local file header (PK\3\4), or the end record of an empty archive (PK\5\6) */
    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            ((bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) || (bytes[2] == 0x05.toByte() && bytes[3] == 0x06.toByte()))

    /** the entry path matches the glob and not the exclude glob. A directory's path is matched without its trailing '/'. */
    private fun matches(path: String, options: ReadOptions): Boolean {
        val p = path.trimEnd('/')
        return options.glob.matches(p) && (null == options.exclude || options.exclude.matches(p).not())
    }

    /** adds the entry's issues; false if its content cannot be read */
    private fun checkEntry(path: String, handle: FileSystemObjectHandle, metadata: FileMetadata, options: ReadOptions, issues: IssueHolder<DataItemIssueLocation>): Boolean {
        if (handle !is FileHandle) return false
        val loc = ZipIssueLocation(path)
        val size = metadata.size ?: 0L
        val compressedSize = metadata.compressedSize ?: 0L
        var readable = true
        if (handle is FileHandleZip && handle.isEncrypted) {
            issues.error(loc, "'$path' is encrypted, which is not supported.")
            readable = false
        }
        if (handle is FileHandleZip && handle.compressionMethod != 0 && handle.compressionMethod != 8) {
            issues.error(loc, "'$path' uses compression method ${handle.compressionMethod}, which is not supported (only stored and deflated).")
            readable = false
        }
        if (size > options.maxEntrySize || size < 0) {
            issues.error(loc, "'$path' is $size bytes uncompressed, more than the limit of ${options.maxEntrySize}; its content is not read.")
            readable = false
        }
        if (size > ZipProvider.COMPRESSION_RATIO_MIN_SIZE) {
            val ratio = if (compressedSize <= 0) Long.MAX_VALUE else size / compressedSize
            if (ratio > options.maxCompressionRatio) {
                issues.error(loc, "'$path' has a compression ratio of $ratio, more than the limit of ${options.maxCompressionRatio}; its content is not read.")
                readable = false
            }
        }
        return readable
    }

    private suspend fun archiveErrorItem(input: DataItem, message: String): DataItem = DataItemDefault(
        parent = input,
        identity = DataItemIdentity("zipArchive"),
        data = accessorMutator().nothing(),
        provenance = provenanceFor(input, null),
        issues = IssueHolder<DataItemIssueLocation>(listOf(IssueDefault(IssueKind.ERROR, ZipIssueLocation(null), message)))
    )

    /**
     * Not supported, a ZipDatastore is read only: each item is output with an error issue added.
     */
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> =
        input.map { item ->
            DataItemDefault(
                item.parent, item.identity, item.data, provenanceFor(item, null),
                IssueHolder<DataItemIssueLocation>(
                    item.issues.all.toList() + IssueDefault(IssueKind.ERROR, ZipIssueLocation(null), "A ZipDatastore is read only; it cannot write.")
                )
            )
        }

    /**
     * ZipProvenance for an entry item ('source' is the ZipEntry, 'input' the archive item).
     * For any other item (an error item), the input item's provenance, else a DatastoreProvenance.
     * Override it to chain onto the archive item's provenance.
     */
    override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = when (source) {
        is ZipEntry -> ZipProvenance(identity, input?.qualifiedIdentity ?: emptyList(), source.path)
        else -> input?.provenance ?: super.provenanceFor(input, source)
    }
}
