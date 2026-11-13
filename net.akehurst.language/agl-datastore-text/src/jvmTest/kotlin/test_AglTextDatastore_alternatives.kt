package net.akehurst.datastore.text

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.types.asm.StdLibDefault
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private object AltTestProvenance : DataItemProvenance

/** a test for each alternative of the AglTextProvider datastore schema: "String Definitions" and "Language Directory" */
class test_AglTextDatastore_alternatives {

    companion object {
        val grammarStr = """
            namespace test
            grammar Refs {
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = def | use ;
                def = 'def' ID ;
                use = 'use' ref ;
                leaf ref = ID ;
                leaf ID = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val formatStr = $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use $ref"
            }
        """.trimIndent()

        val crossReferenceStr = """
            namespace test.Refs
                identify Def by id
                references {
                    in Use {
                        property ref refers-to Def
                    }
                }
        """.trimIndent()

        val styleStr = """
            namespace test
            styles Refs {
                ID { foreground: blue; }
            }
        """.trimIndent()

        const val SENTENCE = "def a use a"
        const val FORMAT_SENTENCE = "def a def b"
    }

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(AglTextProvider())
    }

    private fun newLanguageDirectory(files: Map<String, String>): Path {
        val dir = Files.createTempDirectory("test_AglTextDatastore_langDir")
        files.forEach { (name, content) -> dir.resolve(name).writeText(content) }
        return dir
    }

    private fun languageDirectoryConfig(dir: Path): DatastoreConfiguration = datastoreConfiguration(AglTextProvider.IDENTITY) {
        argument(AglTextProvider.CONFIG_LANGUAGE_DIRECTORY, dir.toAbsolutePath().toString())
    }

    private suspend fun unitItem(sut: Datastore, text: String): DataItem =
        DataItemDefault(null, DataItemIdentity("u"), sut.accessorMutator().toTypedObject(ParsableUnit(text), StdLibDefault.AnyType), AltTestProvenance)

    /**
     * reads SENTENCE and checks the asm and the resolved reference (so grammar and cross-references were used),
     * then reads FORMAT_SENTENCE and formats its asm back into the unit's text (so the format was used).
     * (FORMAT_SENTENCE has no reference: a resolved reference is currently formatted as its target, e.g. 'use def a'.)
     */
    private suspend fun assertReadsResolvesAndWrites(sut: Datastore) {
        sut.open()
        val read = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, SENTENCE))).toList()
        assertTrue(read[0].issues.errors.isEmpty(), read[0].issues.toString())
        val unit = read[0].data.self as ParsableUnit
        assertTrue(unit.asm is AsmStructure, "asm: ${unit.asm}")
        assertEquals(1, unit.resolvedReferences.size)

        val read2 = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, FORMAT_SENTENCE))).toList()
        val unit2 = read2[0].data.self as ParsableUnit
        sut.beginTransaction()
        unit2.text = ""
        val written = sut.write(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(read2[0])).toList()
        assertTrue(written[0].issues.errors.isEmpty(), written[0].issues.toString())
        assertEquals(FORMAT_SENTENCE, unit2.text.trim())
    }

    @Test
    fun schema_language_directory_alternative_has_lang_dir_parameter() {
        val choice = AglTextProvider().schema.datastore.parameterInfo.filterIsInstance<DsConfigParameterChoice>().single()
        val params = choice.alternative["Language Directory"]!!.parameterInfo.filterIsInstance<DsConfigParameterDefinition>()
        assertEquals(listOf(AglTextProvider.CONFIG_LANGUAGE_DIRECTORY), params.map { it.identity })
        assertTrue(params[0].description.contains(".agl-grm"), params[0].description)
    }

    @Test
    fun string_definitions_alternative() = runTest {
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), datastoreConfiguration(AglTextProvider.IDENTITY) {
            argument(AglTextProvider.CONFIG_GRAMMAR_STR, grammarStr)
            argument(AglTextProvider.CONFIG_FORMAT_STR, formatStr)
            argument(AglTextProvider.CONFIG_CROSS_REFERENCE_STR, crossReferenceStr)
        })
        assertReadsResolvesAndWrites(sut)
    }

    @Test
    fun language_directory_alternative() = runTest {
        // any file names: the kind of each file is given by its extension
        val dir = newLanguageDirectory(
            mapOf(
                "refs.agl-grm" to grammarStr,
                "refs.agl-fmt" to formatStr,
                "refs.agl-ref" to crossReferenceStr,
                "refs.agl-sty" to styleStr,
                "readme.txt" to "not part of the language"
            )
        )
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), languageDirectoryConfig(dir))
        assertReadsResolvesAndWrites(sut)
    }

    @Test
    fun language_directory_with_only_a_grammar_reads_without_references() = runTest {
        val dir = newLanguageDirectory(mapOf("grammar.agl-grm" to grammarStr))
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), languageDirectoryConfig(dir))
        sut.open()
        val read = sut.read(accessConfiguration(AglTextProvider.IDENTITY) {}, flowOf(unitItem(sut, SENTENCE))).toList()
        assertTrue(read[0].issues.errors.isEmpty(), read[0].issues.toString())
        val unit = read[0].data.self as ParsableUnit
        assertNotNull(unit.asm)
        assertTrue(unit.resolvedReferences.isEmpty())
    }

    @Test
    fun language_directory_without_grammar_fails() = runTest {
        val dir = newLanguageDirectory(mapOf("refs.agl-fmt" to formatStr))
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), languageDirectoryConfig(dir))
        val e = assertFailsWith<IllegalStateException> { sut.types() }
        assertTrue(e.message!!.contains(".agl-grm"), e.message)
    }

    @Test
    fun language_directory_with_two_grammars_fails() = runTest {
        val dir = newLanguageDirectory(mapOf("a.agl-grm" to grammarStr, "b.agl-grm" to grammarStr))
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), languageDirectoryConfig(dir))
        val e = assertFailsWith<IllegalStateException> { sut.types() }
        assertTrue(e.message!!.contains("more than one"), e.message)
    }

    @Test
    fun language_directory_not_found_fails() = runTest {
        val dir = Files.createTempDirectory("test_AglTextDatastore_langDir").resolve("missing")
        val sut = AglTextProvider().provide(DatastoreIdentity("txt"), languageDirectoryConfig(dir))
        val e = assertFailsWith<IllegalStateException> { sut.types() }
        assertTrue(e.message!!.contains("not found"), e.message)
    }
}
