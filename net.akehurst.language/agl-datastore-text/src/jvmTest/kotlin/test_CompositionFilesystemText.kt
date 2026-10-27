package net.akehurst.datastore.composition

import kotlinx.coroutines.flow.toList
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

        assertEquals(expected.map { (it.root[0] as AsmStructure).asString() }, actual.map { (it.data.self as AsmStructure).asString() })

        // the parsed item's parent is the adapted ParsableUnit, whose parent is the file item
        val unitItem = actual[0].parent!!
        assertIs<ConnectionProvenance>(unitItem.provenance)
        val unit = assertIs<ParsableUnit>(unitItem.data.self)
        assertEquals("Hello World!", unit.text)
        assertEquals(unitItem.parent!!.data.self, unit.owner)

        // the generated association: ParsableUnit.owner and the derived FileHandle.parsableUnit
        val am = composition.accessorMutator()
        println(composition.types().findNamespaceOrNull("composition".asQualifiedName)?.asString())
        assertEquals(unitItem.parent!!.data.self, am.getPropertySuspend(unitItem.data, "owner").self)
        assertEquals(unit, am.getPropertySuspend(unitItem.parent!!.data, "parsableUnit").self)

        // the association owns those properties, the parts' types are not changed
        val types = composition.types()
        val fileHandle = types.findFirstDefinitionByPossiblyQualifiedNameOrNull("filesystem.FileHandle".asPossiblyQualifiedName) as StructuredType
        val parsableUnit = types.findFirstDefinitionByPossiblyQualifiedNameOrNull(AglTextProvider.PARSABLE_UNIT_TYPE.asPossiblyQualifiedName) as StructuredType
        assertNull(fileHandle.findOwnedPropertyOrNull(PropertyName("parsableUnit")))
        assertNull(parsableUnit.findOwnedPropertyOrNull(PropertyName("owner")))

        // identities: file path, then the connection identity, then the parsed root index
        assertEquals(listOf(DataItemIdentity("parsableUnit"), DataItemIdentity("0")), actual[0].qualifiedIdentity.drop(1))
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
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
            connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE, name = "second") {
                end("owner", "filesystem.FileHandle", "\$self")
                end("text", "String", "content.asString")
            }
        }
        val acc = composition.accessConfiguration {
            access("fs") {}
            access("txt") {}
        }

        composition.open()

        val actual = composition.read(acc).toList()

        // one parsed item per connection
        assertEquals(2, actual.size)
        assertEquals(
            listOf(listOf("first", "0"), listOf("second", "0")),
            actual.map { it.qualifiedIdentity.drop(1).map { id -> id.value } }
        )
        // each connection has its own derived association end on FileHandle
        val am = composition.accessorMutator()
        val fileData = actual[0].parent!!.parent!!.data
        assertEquals(actual[0].parent!!.data.self, am.getPropertySuspend(fileData, "first").self)
        assertEquals(actual[1].parent!!.data.self, am.getPropertySuspend(fileData, "second").self)
    }

}