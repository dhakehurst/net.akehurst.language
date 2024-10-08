package net.akehurst.language.grammar.processor

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.PropertyCharacteristic
import net.akehurst.language.typemodel.api.PropertyName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglGrammar {

    @Test
    fun typeModel() {

        val actual = AglGrammar.typeModel

        assertNotNull(actual)
        val grm = actual.findFirstByNameOrNull(SimpleName("GrammarDefault"))
        assertNotNull(grm)
        val grm_name = grm.findPropertyOrNull(PropertyName("name"))
        assertNotNull(grm_name)
        assertEquals("SimpleName",grm_name.typeInstance.declaration.name.value)
        val grm_extends = grm.findPropertyOrNull(PropertyName("extends"))
        assertNotNull(grm_extends)
        assertTrue(grm_extends.isReadWrite)
    }
}