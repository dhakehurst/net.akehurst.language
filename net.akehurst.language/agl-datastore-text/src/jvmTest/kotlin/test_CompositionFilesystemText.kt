package net.akehurst.datastore.composition

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.common.datastoreComposition
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.datastore.filesystem.FilesystemProvider
import net.akehurst.datastore.text.AglTextProvider
import net.akehurst.datastore.text.test_AglTextDatastore
import net.akehurst.language.asm.builder.asmSimple
import java.nio.file.Files
import kotlin.io.path.absolutePathString
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

        val cfg = datastoreCompositeConfiguration {
            datastore("fs") {
                argument(FilesystemProvider.CONFIG_FS_ROOT, tempDir.absolutePathString())
            }
            datastore("txt") {

            }
        }
        val actual = composition.read().toList()

        val expected = listOf(
            asmSimple {
                element("S") {
                    propertyString("name", "World")
                }
            }
        )

        assertEquals(expected, actual)
    }

}