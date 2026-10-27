package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private object TestProvenance : DataItemProvenance

class test_FilesystemDatastore {

    companion object {
    }

    val tempDir = Files.createTempDirectory("test_FilesystemDatasource")

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(FilesystemProvider())
    }

    @Test
    fun construct() {
        val cfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        assertNotNull(sut)
        assertEquals("test", sut.identity.value)
    }

    @Test
    fun types() = runTest {
        val cfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.types()
        val expected = typesDomain("Filesystem", true) {
            namespace("filesystem") {
                data("FileSystemObjectHandle") {
                    propertyOf(setOf(REF, VAL), "name", "String")
                }
                data("DirectoryHandle") {
                    supertype("FileSystemObjectHandle")
                }
                data("FileHandle") {
                    supertype("FileSystemObjectHandle")
                    propertyOf(setOf(REF, VAL), "extension", "String")
                    propertyOf(setOf(REF, VAL), "content", "FileContent")
                }
                data("FileContent") {
                    propertyOf(setOf(REF, VAR), "asString", "String")
                }
            }
        }

        assertEquals(expected.asString(), actual.asString())
    }

    @Test
    fun accessorMutator() = runTest {
        val cfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.accessorMutator()

        assertNotNull(actual)
    }

    @Test
    fun read() = runTest {
        tempDir.resolve("test.txt").toFile().createNewFile()

        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) {

        }
        val actual = sut.read(cfg, emptyFlow()).toList()
        assertEquals(1, actual.size)
        val actualAsString = Agl.formatByReflection(
            FormatString(
                $$"""
                    namespace filesystem
                    format Test {
                       FileHandle -> "FileHandle { name = '$name' }"
                    }
                """.trimIndent()
            ),
            sut.types(),
            actual[0].data.self,
        ).sentence

        val expected = "FileHandle { name = 'test.txt' }"

        assertEquals(expected, actualAsString)
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val asm = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }.root[0] as AsmStructure
        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) { }
        val actual = sut.write(cfg, flowOf(DataItemDefault(null, DataItemIdentity("test"), og.toTypedObject(asm, StdLibDefault.AnyType), TestProvenance))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].data.self as String)
    }

    @Test
    fun read_with_glob() = runTest {
        tempDir.resolve("a.txt").toFile().createNewFile()
        tempDir.resolve("b.md").toFile().createNewFile()

        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.ACCESS_GLOB, "*.txt")
        }

        val actual = sut.read(cfg, emptyFlow()).toList()

        assertEquals(listOf("a.txt"), actual.map { it.identity.value.substringAfterLast('/') })
    }
}
