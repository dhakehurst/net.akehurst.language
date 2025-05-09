package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.typemodel.api.PropertyName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglGrammar {

    @Test
    fun grammarModel_EQ_grammarString() {
        val actual = AglGrammar.grammarModel.asString()
        val expected = AglGrammar.grammarString

        assertEquals(expected, actual)
    }

    @Test
    fun proces_grammarString_EQ_grammarModel() {
        val res = Agl.registry.agl.grammar.processor!!.process(
            AglBase.grammarString + "\n" + AglGrammar.grammarString,
            Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            }
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val actual = res.asm!!.asString()
        val expected = AglGrammar.grammarModel.asString()

        assertEquals(expected, actual)
    }

    @Test
    fun typeModel() {
        val actual = AglGrammar.typeModel

        assertNotNull(actual)
        val grm = actual.findFirstDefinitionByNameOrNull(SimpleName("GrammarDefault"))
        assertNotNull(grm)

        val grm_name = grm.findAllPropertyOrNull(PropertyName("name"))
        assertNotNull(grm_name)
        assertEquals("SimpleName", grm_name.typeInstance.resolvedDeclaration.name.value)

        val grm_extends = grm.findAllPropertyOrNull(PropertyName("extends"))
        assertNotNull(grm_extends)
        assertTrue(grm_extends.isReadWrite)

    }

    @Test
    fun transformModel_EQ_transformString() {
        val actual = AglGrammar.asmTransformModel.asString()
        val expected = AglGrammar.asmTransformString

        assertEquals(expected, actual)
    }

    @Test
    fun styleModel_EQ_styleString() {
        val actual = AglGrammar.styleModel.asString()
        val expected = AglGrammar.styleString

        assertEquals(expected, actual)
    }

    @Test
    fun supertype_correctly_created() {
        val grmNs = AglGrammar.typeModel.findFirstDefinitionByNameOrNull(SimpleName("GrammarNamespaceDefault"))
        assertNotNull(grmNs)
        assertEquals("GrammarNamespace", grmNs.supertypes[0].typeName.value)
        assertEquals("NamespaceAbstract", grmNs.supertypes[1].typeName.value)
    }

}