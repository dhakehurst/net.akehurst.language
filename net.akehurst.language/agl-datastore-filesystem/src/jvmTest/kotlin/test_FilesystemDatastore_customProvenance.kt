package net.akehurst.datastore.filesystem

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.kotlinx.filesystem.api.FileSystemObjectHandle
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class test_FilesystemDatastore_customProvenance {

    data class MyProvenance(val datastore: String, val name: String) : DataItemProvenance

    class MyFilesystemDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : FilesystemDatastore(identity, configuration) {
        override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = when (source) {
            is FileSystemObjectHandle -> MyProvenance(identity.value, source.name)
            else -> super.provenanceFor(input, source)
        }
    }

    class MyFilesystemProvider : FilesystemProvider() {
        override val identity: DatastoreProviderIdentity get() = DatastoreProviderIdentity("test.MyFilesystemProvider")
        override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
            MyFilesystemDatastore(datastoreIdentity, configuration)
    }

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(FilesystemProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(MyFilesystemProvider())
    }

    val tempDir = Files.createTempDirectory("test_FilesystemDatastore_customProvenance")

    @Test
    fun default_provenance() = runTest {
        tempDir.resolve("test.txt").toFile().createNewFile()
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        sut.open()
        val actual = sut.read(accessConfiguration(FilesystemProvider.IDENTITY) {}, emptyFlow()).toList()

        assertEquals(1, actual.size)
        val handle = actual[0].data.self as FileSystemObjectHandle
        assertEquals(FilesystemProvenance(DatastoreIdentity("test"), handle.path), actual[0].provenance)
    }

    @Test
    fun custom_provenance() = runTest {
        tempDir.resolve("test.txt").toFile().createNewFile()
        val prov = MyFilesystemProvider()
        val dscfg = datastoreConfiguration(prov.identity) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = prov.provide(DatastoreIdentity("test"), dscfg)
        sut.open()
        val actual = sut.read(accessConfiguration(prov.identity) {}, emptyFlow()).toList()

        assertEquals(1, actual.size)
        assertEquals(MyProvenance("test", "test.txt"), actual[0].provenance)
    }
}
