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
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import net.akehurst.language.base.api.asPossiblyQualifiedName
import kotlin.test.assertFailsWith

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
        sut.open()
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

    private suspend fun readFile(sut: net.akehurst.datastore.api.Datastore, name: String) =
        sut.read(accessConfiguration(FilesystemProvider.IDENTITY) {}, emptyFlow()).toList().first { it.identity.value.endsWith("/$name") }

    @Test
    fun write_is_applied_on_commit() = runTest {
        val file = tempDir.resolve("w.txt").toFile().also { it.writeText("old") }
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        val item = readFile(sut, "w.txt")

        sut.beginTransaction()
        val content = item.data.getPropertySuspend("content")
        content.setPropertySuspend("asString", og.toTypedObject("new", StdLibDefault.String))
        val actual = sut.write(accessConfiguration(FilesystemProvider.IDENTITY) {}, flowOf(item)).toList()

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.isEmpty())
        // staged: the file is unchanged, but reading through the datastore gives the staged content
        assertEquals("old", file.readText())
        assertEquals("new", item.data.getPropertySuspend("content").getPropertySuspend("asString").self)

        sut.commit()
        assertEquals("new", file.readText())
    }

    @Test
    fun write_is_discarded_on_rollback() = runTest {
        val file = tempDir.resolve("w.txt").toFile().also { it.writeText("old") }
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        val item = readFile(sut, "w.txt")

        sut.beginTransaction()
        item.data.getPropertySuspend("content").setPropertySuspend("asString", og.toTypedObject("new", StdLibDefault.String))
        sut.write(accessConfiguration(FilesystemProvider.IDENTITY) {}, flowOf(item)).toList()
        sut.rollback()

        assertEquals("old", file.readText())
        assertEquals("old", item.data.getPropertySuspend("content").getPropertySuspend("asString").self)
    }

    @Test
    fun set_content_outside_transaction_fails() = runTest {
        tempDir.resolve("w.txt").toFile().writeText("old")
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        val item = readFile(sut, "w.txt")

        assertFailsWith<IllegalStateException> {
            item.data.getPropertySuspend("content").setPropertySuspend("asString", og.toTypedObject("new", StdLibDefault.String))
        }
    }

    @Test
    fun write_non_file_gives_issue() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        sut.beginTransaction()
        val actual = sut.write(
            accessConfiguration(FilesystemProvider.IDENTITY) {},
            flowOf(DataItemDefault(null, DataItemIdentity("x"), og.toTypedObject("not a file", StdLibDefault.String), TestProvenance))
        ).toList()

        assertTrue(actual[0].issues.errors.isNotEmpty())
    }

    private suspend fun newFile(sut: net.akehurst.datastore.api.Datastore, path: String) = sut.accessorMutator().let { og ->
        og.createStructureValueSuspend("filesystem.FileHandle".asPossiblyQualifiedName, mapOf("path" to og.toTypedObject(path, StdLibDefault.String)))
    }

    @Test
    fun new_file_is_created_on_commit() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        sut.beginTransaction()
        val file = newFile(sut, "out/new.txt")
        file.getPropertySuspend("content").setPropertySuspend("asString", og.toTypedObject("hello", StdLibDefault.String))
        val actual = sut.write(accessConfiguration(FilesystemProvider.IDENTITY) {}, flowOf(DataItemDefault(null, DataItemIdentity("new"), file, TestProvenance))).toList()

        assertTrue(actual[0].issues.errors.isEmpty())
        assertEquals("new.txt", file.getPropertySuspend("name").self)
        assertFalse(tempDir.resolve("out").toFile().exists())
        sut.commit()
        assertEquals("hello", tempDir.resolve("out/new.txt").toFile().readText())
    }

    @Test
    fun new_file_is_not_created_on_rollback() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        sut.beginTransaction()
        val file = newFile(sut, "new.txt")
        file.getPropertySuspend("content").setPropertySuspend("asString", og.toTypedObject("hello", StdLibDefault.String))
        sut.rollback()

        assertFalse(tempDir.resolve("new.txt").toFile().exists())
    }

    @Test
    fun new_file_with_path_outside_root_fails() = runTest {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        sut.open()
        assertFailsWith<IllegalStateException> { newFile(sut, "../x.txt") }
        assertFailsWith<IllegalStateException> { newFile(sut, "/x.txt") }
    }

    @Test
    fun new_file_where_a_directory_is_fails_on_commit() = runTest {
        tempDir.resolve("d").toFile().mkdir()
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val og = sut.accessorMutator()
        sut.open()
        sut.beginTransaction()
        newFile(sut, "d").getPropertySuspend("content").setPropertySuspend("asString", og.toTypedObject("hello", StdLibDefault.String))

        assertFailsWith<IllegalStateException> { sut.prepare() }
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

        sut.open()

        val actual = sut.read(cfg, emptyFlow()).toList()

        assertEquals(listOf("a.txt"), actual.map { it.identity.value.substringAfterLast('/') })
    }

    private suspend fun readRelative(glob: String?): List<String> {
        val dscfg = datastoreConfiguration(FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        val sut = FilesystemProvider().provide(DatastoreIdentity("test"), dscfg)
        val cfg = accessConfiguration(FilesystemProvider.IDENTITY) {
            if (null != glob) argument(FilesystemProvider.ACCESS_GLOB, glob)
        }
        sut.open()
        // a handle's path is '/<root dir name>/<path relative to root>'
        val rootPath = "/" + tempDir.fileName.toString() + "/"
        return sut.read(cfg, emptyFlow()).toList().map { it.identity.value.removePrefix(rootPath) }
    }

    private fun createTree() {
        tempDir.resolve("a.txt").toFile().createNewFile()
        tempDir.resolve("sub").toFile().mkdir()
        tempDir.resolve("sub/b.txt").toFile().createNewFile()
    }

    @Test
    fun read_subfolders_default_glob() = runTest {
        createTree()
        assertEquals(setOf("a.txt", "sub", "sub/b.txt"), readRelative(null).toSet())
    }

    @Test
    fun read_subfolders_glob_doublestar() = runTest {
        createTree()
        assertEquals(setOf("a.txt", "sub", "sub/b.txt"), readRelative("**").toSet())
    }

    @Test
    fun read_subfolders_glob_doublestar_txt() = runTest {
        createTree()
        assertEquals(setOf("a.txt", "sub/b.txt"), readRelative("**/*.txt").toSet())
    }

    @Test
    fun read_subfolders_glob_star_txt() = runTest {
        createTree()
        assertEquals(listOf("a.txt"), readRelative("*.txt"))
    }

    @Test
    fun read_subfolders_glob_sub() = runTest {
        createTree()
        assertEquals(listOf("sub/b.txt"), readRelative("sub/**"))
    }

    @Test
    fun read_subfolders_depth_first() = runTest {
        createTree()
        tempDir.resolve("sub/deeper").toFile().mkdir()
        tempDir.resolve("sub/deeper/c.txt").toFile().createNewFile()
        val actual = readRelative("**")
        // each directory comes before its content, and its content comes before the next sibling of the directory
        val i = actual.indexOf("sub")
        assertEquals(listOf("sub/b.txt", "sub/deeper", "sub/deeper/c.txt").toSet(), actual.subList(i + 1, i + 4).toSet())
        assertTrue(actual.indexOf("sub/deeper") < actual.indexOf("sub/deeper/c.txt"))
    }
}
