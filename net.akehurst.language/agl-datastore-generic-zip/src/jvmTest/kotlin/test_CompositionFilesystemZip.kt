package net.akehurst.datastore.generic.zip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.datastore.common.DatastoreCompositionExt.accessConfiguration
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.datastoreComposition
import net.akehurst.datastore.common.datastoreProviderSchema
import net.akehurst.datastore.filesystem.FilesystemProvider
import net.akehurst.datastore.text.AglTextProvider
import net.akehurst.datastore.text.ParsableUnit
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** a file's bytes, as e.g. an Excel, Word or PDF datastore takes its input */
class ZipTestBinaryFile(val bytes: ByteArray)

/** a datastore whose read takes ZipTestBinaryFile items and passes them on unchanged */
class ZipTestBinaryDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
    override suspend fun createTypes(): TypesDomain = typesDomain("ZipTestBinary", true) {
        namespace("test.zipbinary") {
            data("BinaryFile", implementation = ZipTestBinaryFile::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "bytes", "Binary")
                    execution { args -> ZipTestBinaryFile(args[0] as ByteArray) }
                }
            }
        }
    }

    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
}

class ZipTestBinaryProvider : DatastoreProvider {
    companion object {
        val IDENTITY = DatastoreProviderIdentity("test.zipbinary.BinaryTestProvider")
    }

    override val identity: DatastoreProviderIdentity get() = IDENTITY
    override val schema = datastoreProviderSchema {
        datastore { }
        access { }
    }

    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        ZipTestBinaryDatastore(datastoreIdentity, configuration)
}

class test_CompositionFilesystemZip {

    companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = 'Hello' NAME '!' ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val formatStr = $$"""
            namespace test
            format Test {
                S -> "Hello $name !"
            }
        """.trimIndent()

        /** bytes that are not valid UTF-8, so any conversion through String would change them */
        val binaryBytes = byteArrayOf(0x00, 0xC3.toByte(), 0x28, 0xFF.toByte(), 0xFE.toByte(), 0x80.toByte(), 0x7F)
    }

    val tempDir = Files.createTempDirectory("test_CompositionFilesystemZip")

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(FilesystemProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(ZipProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(AglTextProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(ZipTestBinaryProvider())
    }

    @Test
    fun filesystem_zip_text() = runTest {
        tempDir.resolve("specs.reqifz").toFile().writeBytes(zip {
            file("a/one.txt", "Hello World!")
            file("b/two.txt", "Hello Zip!")
            file("b/ignored.bin", "not text")
        })
        tempDir.resolve("other.txt").toFile().writeText("not an archive")

        val composition = datastoreComposition("fs-zip-text") {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("zip", ZipProvider.IDENTITY) {}
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
            }
            connect("fs", "zip", ZipProvider.ZIP_ARCHIVE_TYPE) {
                end("file", "filesystem.FileHandle", "\$self")
                end("bytes", "Binary", "content.asBinary")
            }
            connect("zip", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
                end("owner", ZipProvider.ZIP_ENTRY_TYPE, "\$self")
                end("text", "String", "content.asString")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") { argument(FilesystemProvider.ACCESS_GLOB, "*.reqifz") }
            access("zip") { argument(ZipProvider.ACCESS_GLOB, "**/*.txt") }
            access("txt") {}
        }
        composition.open()
        val actual = composition.read(acc).toList()

        assertEquals(2, actual.size)
        actual.forEach { assertTrue(it.issues.errors.isEmpty(), it.issues.toString()) }
        val units = actual.map { assertIs<ParsableUnit>(it.data.self) }
        assertEquals(listOf("Hello World!", "Hello Zip!"), units.map { it.text })
        assertTrue(units.all { it.asm is AsmStructure })
        // unit item -> zip entry item -> archive item -> file item
        val entryItem = actual[1].parent!!
        assertEquals("b/two.txt", entryItem.identity.value)
        assertIs<ZipEntry>(entryItem.data.self)
        assertEquals(entryItem.data.self, actual[1].data.getPropertySuspend("owner").self)
        assertIs<ZipArchive>(entryItem.parent!!.data.self)
        assertTrue(entryItem.parent!!.parent!!.identity.value.endsWith("specs.reqifz"))
    }

    @Test
    fun filesystem_zip_binary_bytes_unchanged() = runTest {
        tempDir.resolve("data.zip").toFile().writeBytes(zip {
            binary("deflated.bin", binaryBytes)
            binary("stored.bin", binaryBytes, deflate = false)
        })

        val composition = datastoreComposition("fs-zip-binary") {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("zip", ZipProvider.IDENTITY) {}
            datastore("bin", ZipTestBinaryProvider.IDENTITY) {}
            connect("fs", "zip", ZipProvider.ZIP_ARCHIVE_TYPE) {
                end("bytes", "Binary", "content.asBinary")
            }
            connect("zip", "bin", "test.zipbinary.BinaryFile") {
                end("owner", ZipProvider.ZIP_ENTRY_TYPE, "\$self")
                end("bytes", "Binary", "content.asBinary")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") { argument(FilesystemProvider.ACCESS_GLOB, "*.zip") }
            access("zip") {}
            access("bin") {}
        }
        composition.open()
        val actual = composition.read(acc).toList()

        assertEquals(2, actual.size)
        actual.forEach {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            val file = assertIs<ZipTestBinaryFile>(it.data.self)
            assertTrue(binaryBytes.contentEquals(file.bytes), "bytes differ: ${file.bytes.toList()}")
        }
    }
}
