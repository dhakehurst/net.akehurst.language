package net.akehurst.datastore.composition

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.types.asm.StdLibDefault
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.common.ConnectionProvenance
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreComposition
import net.akehurst.datastore.filesystem.FilesystemProvider
import net.akehurst.datastore.text.AglTextProvider
import net.akehurst.datastore.text.ParsableUnit
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.StructuredType
import kotlin.test.assertNull
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.datastore.text.test_AglTextDatastore
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.types.builder.typesDomain
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class test_CompositionFilesystemText {

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
    }

    val tempDir = Files.createTempDirectory("test_FilesystemDatasource")

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(FilesystemProvider())
        DatastoreProviderRegistryDefault.GLOBAL.register(AglTextProvider())
    }

    @Test
    fun types()= runTest {
        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
        }

        val actual = composition.types()
        val expected = typesDomain("",true) {
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
            namespace("test.Test") {
                data("S") {
                    propertyOf(setOf(REF, VAR), "name", "String")
                }
            }
        }

        assertEquals(expected.asString(), actual.asString())
    }

    @Test
    fun compose() = runTest {

        val file = tempDir.resolve("test.txt").toFile().also {
            it.createNewFile()
            it.writeText("Hello World!")
        }

        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
        }

        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {

            }
        }
        composition.open()
        val actual = composition.read(acc).toList()

        val expected = listOf(
            asmSimple(typesDomain = composition.types()) {
                element("S") {
                    propertyString("name", "World")
                }
            }
        )

        // each item is the adapted ParsableUnit, whose asm is set by the text datastore, and whose parent is the file item
        val unitItem = actual[0]
        val unit = assertIs<ParsableUnit>(unitItem.data.self)
        assertEquals("Hello World!", unit.text)
        assertEquals(expected.map { (it.root[0] as AsmStructure).asString() }, actual.map { ((it.data.self as ParsableUnit).asm as AsmStructure).asString() })

        // the generated association: ParsableUnit.owner and FileHandle.parsableUnit, both navigable through the link
        val am = composition.accessorMutator()
        println(composition.types().findNamespaceOrNull("composition".asQualifiedName)?.asString())
        // navigation from the items' own data follows the links of this read
        assertEquals(unitItem.parent!!.data.self, unitItem.data.getPropertySuspend("owner").self)
        assertEquals(unit, unitItem.parent!!.data.getPropertySuspend("parsableUnit").self)
        // the composition's own accessor-mutator holds no links
        assertTrue(am.getPropertySuspend(unitItem.parent!!.data, "parsableUnit").isNothing)

        // the association owns those properties, the parts' types are not changed
        val types = composition.types()
        val fileHandle = types.findFirstDefinitionByPossiblyQualifiedNameOrNull("filesystem.FileHandle".asPossiblyQualifiedName) as StructuredType
        val parsableUnit = types.findFirstDefinitionByPossiblyQualifiedNameOrNull(AglTextProvider.PARSABLE_UNIT_TYPE.asPossiblyQualifiedName) as StructuredType
        assertNull(fileHandle.findOwnedPropertyOrNull(PropertyName("parsableUnit")))
        assertNull(parsableUnit.findOwnedPropertyOrNull(PropertyName("owner")))

        // identities: file path, then the connection identity
        assertEquals(listOf(DataItemIdentity("parsableUnit")), actual[0].qualifiedIdentity.drop(1))
    }

    @Test
    fun compose_fan_out() = runTest {
        tempDir.resolve("test.txt").toFile().also {
            it.createNewFile()
            it.writeText("Hello World!")
        }

        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE, name = "first") {
                end("firstOwner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE, name = "second") {
                end("secondOwner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }

        composition.open()

        val actual = composition.read(acc).toList()

        // one unit item per connection
        assertEquals(2, actual.size)
        assertEquals(
            listOf(listOf("first"), listOf("second")),
            actual.map { it.qualifiedIdentity.drop(1).map { id -> id.value } }
        )
        // each connection has its own derived association end on FileHandle
        val fileData = actual[0].parent!!.data
        assertEquals(actual[0].data.self, fileData.getPropertySuspend("first").self)
        assertEquals(actual[1].data.self, fileData.getPropertySuspend("second").self)
        // and back, each by its own end name
        assertEquals(fileData.self, actual[0].data.getPropertySuspend("firstOwner").self)
        assertEquals(fileData.self, actual[1].data.getPropertySuspend("secondOwner").self)
    }

    @Test
    fun compose_fan_out_with_same_end_name_fails() = runTest {
        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE, name = "first") {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE, name = "second") {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
        }
        // ParsableUnit would have two association ends named 'owner'
        assertFailsWith<IllegalStateException> { composition.types() }
    }


    @Test
    fun two_reads_have_independent_links() = runTest {
        tempDir.resolve("test.txt").toFile().also {
            it.createNewFile()
            it.writeText("Hello World!")
        }
        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()

        val first = composition.read(acc).toList()
        val second = composition.read(acc).toList()

        val unit1 = first[0]
        val unit2 = second[0]
        assertNotSame(unit1.data.self, unit2.data.self)
        // the second read does not change the links of the first
        assertSame(unit1.data.self, unit1.parent!!.data.getPropertySuspend("parsableUnit").self)
        assertSame(unit2.data.self, unit2.parent!!.data.getPropertySuspend("parsableUnit").self)
        assertEquals(unit1.parent!!.data.self, unit1.data.getPropertySuspend("owner").self)
    }

    private fun writeComposition() = datastoreComposition {
        datastore("fs", FilesystemProvider.IDENTITY) {
            argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
        }
        datastore("txt", AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
        }
        connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
            end("owner", "filesystem.FileHandle", "\$self")
            end("text", "String", "content.asString")
        }
    }

    private fun setName(item: DataItem, name: String) {
        val s = (item.data.self as ParsableUnit).asm as AsmStructure
        val p = s.property[PropertyValueName("name")]!!
        s.setProperty(PropertyValueName("name"), AsmPrimitiveSimple.stdString(name), p.index)
    }

    @Test
    fun write_changes_file_on_commit() = runTest {
        val file = tempDir.resolve("test.txt").toFile().also { it.writeText("Hello World!") }
        val composition = writeComposition()
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        val items = composition.read(acc).toList()
        setName(items[0], "Earth")

        composition.beginTransaction()
        val actual = composition.write(acc, items.asFlow()).toList()

        // the file item is written; the file is changed only on commit
        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        assertSame(items[0].parent!!.data.self, actual[0].data.self)
        assertEquals("Hello World!", file.readText())
        composition.commit()
        assertEquals("Hello Earth !", file.readText())
    }

    @Test
    fun write_rollback_leaves_file_unchanged() = runTest {
        val file = tempDir.resolve("test.txt").toFile().also { it.writeText("Hello World!") }
        val composition = writeComposition()
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        val items = composition.read(acc).toList()
        setName(items[0], "Earth")

        composition.beginTransaction()
        composition.write(acc, items.asFlow()).toList()
        composition.rollback()

        assertEquals("Hello World!", file.readText())
    }

    @Test
    fun write_unlinked_item_gives_issue() = runTest {
        tempDir.resolve("test.txt").toFile().also { it.writeText("Hello World!") }
        val composition = writeComposition()
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        val am = composition.accessorMutator()
        val unit = ParsableUnit("Hello Moon !")
        val parsed = composition.read(acc).toList()[0].data.self as ParsableUnit
        unit.asm = parsed.asm
        // created, but not linked to a FileHandle
        val item = DataItemDefault(null, DataItemIdentity("new"), am.toTypedObject(unit, StdLibDefault.AnyType), object : DataItemProvenance {})

        composition.beginTransaction()
        val actual = composition.write(acc, flowOf(item)).toList()

        assertEquals(1, actual.size)
        assertSame(unit, actual[0].data.self)
        assertTrue(actual[0].issues.errors.isNotEmpty())
        composition.rollback()
    }

    @Test
    fun write_outside_transaction_fails() = runTest {
        val composition = writeComposition()
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        assertFailsWith<IllegalStateException> { composition.write(acc, emptyFlow()) }
    }

    @Test
    fun write_with_end_expression_not_assignable_fails() = runTest {
        val composition = datastoreComposition {
            datastore("fs", FilesystemProvider.IDENTITY) {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt", AglTextProvider.IDENTITY) {
                argument(AglTextProvider.CONFIG_GRAMMAR_STR, test_AglTextDatastore.grammarStr)
                argument(AglTextProvider.CONFIG_FORMAT_STR, test_AglTextDatastore.formatStr)
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString + '!'")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        composition.beginTransaction()
        assertFailsWith<IllegalStateException> { composition.write(acc, emptyFlow()) }
        composition.rollback()
    }

    @Test
    fun write_new_unit_creates_new_file_on_commit() = runTest {
        val composition = writeComposition()
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }
        composition.open()
        composition.beginTransaction()
        // as an m2m would: create a new unit and a new file, and link them
        val am = composition.accessorMutator()
        val file = am.createStructureValueSuspend("filesystem.FileHandle".asPossiblyQualifiedName, mapOf("path" to am.toTypedObject("out/moon.txt", StdLibDefault.String)))
        val unit = am.createStructureValueSuspend(AglTextProvider.PARSABLE_UNIT_TYPE.asPossiblyQualifiedName, mapOf("text" to am.toTypedObject("", StdLibDefault.String)))
        (unit.self as ParsableUnit).asm = asmSimple(typesDomain = composition.types()) {
            element("S") { propertyString("name", "Moon") }
        }.root[0]
        am.setPropertySuspend(file, "parsableUnit", unit)
        val item = DataItemDefault(null, DataItemIdentity("moon"), unit, object : DataItemProvenance {})

        val actual = composition.write(acc, flowOf(item)).toList()

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.isEmpty(), actual[0].issues.toString())
        assertSame(file.self, actual[0].data.self)
        assertTrue(tempDir.resolve("out/moon.txt").toFile().exists().not())
        composition.commit()
        assertEquals("Hello Moon !", tempDir.resolve("out/moon.txt").toFile().readText())
    }
}
