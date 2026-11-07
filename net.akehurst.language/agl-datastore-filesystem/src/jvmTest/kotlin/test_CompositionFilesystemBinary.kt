package net.akehurst.datastore.filesystem

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
class BinaryFile(val bytes: ByteArray)

/** a datastore whose read takes BinaryFile items and passes them on unchanged */
class BinaryTestDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
    override suspend fun createTypes(): TypesDomain = typesDomain("BinaryTest", true) {
        namespace("test.binary") {
            data("BinaryFile", implementation = BinaryFile::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "bytes", "Binary")
                    execution { args -> BinaryFile(args[0] as ByteArray) }
                }
            }
        }
    }

    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
}

class BinaryTestProvider : DatastoreProvider {
    companion object {
        val IDENTITY = DatastoreProviderIdentity("test.binary.BinaryTestProvider")
    }

    override val identity: DatastoreProviderIdentity get() = IDENTITY
    override val schema = datastoreProviderSchema { }
    override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
        BinaryTestDatastore(datastoreIdentity, configuration)
}

class test_CompositionFilesystemBinary {

    /** bytes that are not valid UTF-8, so any conversion through String would change them */
    private val binaryBytes = byteArrayOf(0x00, 0xC3.toByte(), 0x28, 0xFF.toByte(), 0xFE.toByte(), 0x80.toByte(), 0x7F)

    val tempDir = Files.createTempDirectory("test_CompositionFilesystemBinary")

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(FilesystemProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(BinaryTestProvider())
    }

    private suspend fun readThroughConnect(bytesEndType: String): List<DataItem> {
        tempDir.resolve("b.bin").toFile().writeBytes(binaryBytes)
        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("bin", BinaryTestProvider.IDENTITY) {}
            connect("fs", "bin", "test.binary.BinaryFile") {
                end("owner", "filesystem.FileHandle", "\$self")
                end("bytes", bytesEndType, "content.asBinary")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("bin") {}
        }
        composition.open()
        return composition.read(acc).toList()
    }

    @Test
    fun connect_passes_bytes_unchanged() = runTest {
        val actual = readThroughConnect("Binary")

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        val file = assertIs<BinaryFile>(actual[0].data.self)
        assertTrue(binaryBytes.contentEquals(file.bytes), "bytes differ: ${file.bytes.toList()}")
    }

    @Test
    fun connect_passes_bytes_unchanged_when_end_typed_Any() = runTest {
        val actual = readThroughConnect("Any")

        assertEquals(1, actual.size)
        val file = assertIs<BinaryFile>(actual[0].data.self)
        assertTrue(binaryBytes.contentEquals(file.bytes), "bytes differ: ${file.bytes.toList()}")
    }
}
