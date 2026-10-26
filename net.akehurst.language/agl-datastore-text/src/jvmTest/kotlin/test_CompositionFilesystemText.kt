package net.akehurst.datastore.composition

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreComposition
import net.akehurst.datastore.filesystem.FilesystemProvider
import net.akehurst.datastore.text.AglTextProvider
import net.akehurst.datastore.text.test_AglTextDatastore
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.types.builder.typesDomain
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

        val acc = composition.accessConfiguration {
            access("fs") {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            access("txt") {

            }
        }

        val actual = composition.types(acc)
        val expected = typesDomain("",true) {

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
        }

        val acc = composition.accessConfiguration {
            access("fs") {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            access("txt") {

            }
        }
        val actual = composition.read(acc).toList()

        val expected = listOf(
            asmSimple(typesDomain = composition.types(acc)) {
                element("S") {
                    propertyString("name", "World")
                }
            }
        )

        assertEquals(expected.map { it.root[0] }, actual)
    }

}