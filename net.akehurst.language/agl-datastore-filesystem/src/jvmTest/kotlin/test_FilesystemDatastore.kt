package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
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
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        assertNotNull(sut)
        assertEquals("test", sut.identity.value)
    }

    @Test
    fun types() = runTest {
        val cfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)
        val acc = accessConfiguration(FilesystemProvider.IDENTITY) { }

        val actual = sut.types(acc)
        val expected = typesDomain("Filesystem",true) {
            namespace("filesystem") {
                data("FileSystemObjectHandle") {
                    propertyOf(setOf(REF, VAL),"name","String")
                }
                data("DirectoryHandle") {
                    supertype("FileSystemObjectHandle")
                }
                data("FileHandle") {
                    supertype("FileSystemObjectHandle")
                    propertyOf(setOf(REF, VAL),"extension","String")
                }
            }
        }


        assertEquals(expected.asString(), actual.asString())
    }

    @Test
    fun accessorMutator() = runTest {
        val cfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)
        val acc = accessConfiguration(FilesystemProvider.IDENTITY) { }

        val actual = sut.accessorMutator(acc)

        assertNotNull(actual)
    }

    @Test
    fun read() = runTest {
        tempDir.resolve("test.txt").toFile().createNewFile()

        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val acc = accessConfiguration(FilesystemProvider.IDENTITY) { }
        val og = sut.accessorMutator(acc)

        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) {

        }
        val actual = sut.read(cfg, emptyFlow()).toList()

        val expected = asmSimple(sut.types(acc)) {
            element("FileHandle") {}
        }

        assertNotNull(actual)
        assertEquals(expected.asString(), (actual[0].self as AsmStructure).asString())
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val acc = accessConfiguration(FilesystemProvider.IDENTITY) { }
        val og = sut.accessorMutator(acc)

        val asm = asmSimple(sut.types(acc)) {
            element("S") {
                propertyString("name", "World")
            }
        }.root[0] as AsmStructure
        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) { }
        val actual = sut.write(cfg, flowOf(og.toTypedObject(asm, StdLibDefault.AnyType))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].self as String)
    }
}