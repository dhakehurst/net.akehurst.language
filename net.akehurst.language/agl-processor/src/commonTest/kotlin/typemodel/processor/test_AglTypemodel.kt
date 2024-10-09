package net.akehurst.language.typemodel.processor

import net.akehurst.language.base.api.SimpleName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglTypemodel2 {

    @Test
    fun typemodel() {
        val actual = AglTypemodel.typeModel

        assertNotNull(actual)
    }


    @Test
    fun domainTypes() {
        val td = AglTypemodel.typeModel.findFirstByNameOrNull(SimpleName("TypeModel"))
        assertNotNull(td)
        assertEquals("TypeModel", td.name.value)
        assertEquals("Model", td.supertypes[0].typeName.value)
        assertEquals("TypeNamespace", td.supertypes[0].typeArguments[0].type.typeName.value)
        assertEquals("TypeDeclaration", td.supertypes[0].typeArguments[1].type.typeName.value)
    }
}