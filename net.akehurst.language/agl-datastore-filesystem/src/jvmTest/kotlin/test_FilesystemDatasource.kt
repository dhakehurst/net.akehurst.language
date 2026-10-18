package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_FilesystemDatasource {

    companion object {
    }

    val tempDir = Files.createTempDirectory("test_FilesystemDatasource")

    @Test
    fun construct() {
        val cfg = datastoreConfiguration {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        assertNotNull(sut)
        assertEquals("test", sut.identity.value)
    }

    @Test
    fun types() = runTest {
        val cfg = datastoreConfiguration {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.types()
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
        val cfg = datastoreConfiguration {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), cfg)

        val actual = sut.accessorMutator()

        assertNotNull(actual)
    }

    @Test
    fun read() = runTest {
        val dscfg = datastoreConfiguration {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val cfg = datastoreConfiguration {

        }
        val actual = sut.read(cfg, flowOf(og.toTypedObject("Hello World !", StdLibDefault.String))).toList()

        val expected = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }

        assertNotNull(actual)
        assertEquals(expected.asString(), (actual[0].self as AsmStructure).asString())
    }

    @Test
    fun write() = runTest {
        val dscfg = datastoreConfiguration {
            argument(FilesystemProvider.CONFIG_FS_ROOT,tempDir.absolutePathString())
        }
        val prov = FilesystemProvider()
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()

        val asm = asmSimple(sut.types()) {
            element("S") {
                propertyString("name", "World")
            }
        }.root[0] as AsmStructure
        val cfg = datastoreConfiguration { }
        val actual = sut.write(cfg, flowOf(og.toTypedObject(asm, StdLibDefault.AnyType))).toList()

        assertNotNull(actual)
        assertEquals("Hello World !", actual[0].self as String)
    }
}