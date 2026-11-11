# agl-datastore-generic-zip

A read only datastore that gives the entries of zip archives (`.zip`, ReqIF `.reqifz`, Cameo `.mdzip`, ...)
as datastore items, so a composition can go source → zip → parser and parsers never unzip anything.
Package `net.akehurst.datastore.generic.zip`, `ZipProvider` / `ZipDatastore`. Common code: JVM, JS and wasmJs.

## Use in a composition

```kotlin
datastoreComposition("fs-zip-text") {
    datastore("fs", FilesystemProvider.IDENTITY) { argument(FilesystemProvider.CONFIG_FS_ROOT, root) }
    datastore("zip", ZipProvider.IDENTITY) {}
    datastore("txt", AglTextProvider.IDENTITY) { ... }
    connect("fs", "zip", ZipProvider.ZIP_ARCHIVE_TYPE) {
        end("bytes", "Binary", "content.asBinary")       // from a FileHandle; e.g. "bytes" from a SharePoint file
    }
    connect("zip", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
        end("owner", ZipProvider.ZIP_ENTRY_TYPE, "\$self")
        end("text", "String", "content.asString")         // same expression as from a FileHandle
    }
}
```

## Requirements

| id | requirement | state |
|----|-------------|-------|
| Z1 | Read input: items whose data is a `zip.ZipArchive` (constructor `bytes: Binary`) or a plain `ByteArray`. Any other data gives an error item (identity `zipArchive`). | done |
| Z2 | Output: one item per entry matching `access.glob` (default `**`) and not `access.exclude-glob`, globs over the entry path with `toRegexFromGlob('/')`. Directory entries are left out unless `access.include-directories`. | done |
| Z3 | `zip.ZipEntry` has `name`, `path`, `extension`, `isDirectory`, `size`, `compressedSize`, `lastModified` (Timestamp), `crc32` (unsigned, usable as a source version), `archive` and `content: ZipEntryContent` with `asBinary` and `asString` (UTF-8), the same names as `FilesystemDatastore.FileContent`. | done |
| Z4 | Item identity is the entry path; its parent is the archive item, so `qualifiedIdentity` = archive item's + entry path. | done |
| Z5 | `provenanceFor(input = archive item, source = ZipEntry)`; default `ZipProvenance(datastoreIdentity, archiveIdentity = archive item's qualifiedIdentity, entryPath)`. A subclass can chain onto the archive item's provenance. | done |
| Z6 | Content is inflated only when `asBinary` / `asString` is read, every time it is read (nothing is cached). The archive bytes are in memory. | done |
| Z7 | Zips are detected by content (`PK\3\4`, or `PK\5\6` for an empty archive), not extension. Not a zip, truncated or corrupt: an error item. | done |
| Z8 | Limits (datastore config, advanced group "Limits"): `config.max-entries` (100 000), `config.max-entry-size` (1 GiB), `config.max-total-size` (4 GiB, sum of declared sizes of one archive), `config.max-compression-ratio` (200, for entries over 1 MiB). Over the archive limits: an error item; over the entry limits: an error issue on the entry, whose content then reads as null. An entry must inflate to exactly its declared size and match its CRC-32, else its content reads as null and an error issue is added to its item. | done |
| Z9 | Entry names are plain strings; nothing is extracted. Names with `..`, an absolute path or a drive letter get a warning issue. | done |
| Z10 | Encrypted entries and compression methods other than stored/deflated: an error issue on the entry. | done |
| Z11 | ZIP64 (end records and extra field); names in UTF-8 (flag bit 11, or the Info-ZIP Unicode Path extra field) or CP437; extended timestamp extra field, else the MS-DOS time taken as UTC. Split archives are refused. | done |
| Z12 | Nested archives (access, advanced): `access.recurse-nested` (off) and `access.max-nesting-depth` (3). A nested archive is found by content; its entries' parent is the nested archive's entry item (which is output only if it matches the globs). Limits apply to each archive. | done |
| Z13 | Write is not supported: each item gets an error issue. Delete is not supported. | done |

Reading is done by a small zip reader in this module (`ZipFormat`), with korlibs Deflate for inflating
(`java.util.zip.Inflater` on the JVM). korlibs `ZipVfs` / kotlinx-filesystem `openAsZipDirectory` were not used
because they cannot open bytes, do not report errors, do not give sizes, CRC or encryption, and have no ZIP64 or CP437.
