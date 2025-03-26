package net.akehurst.language.base.processor

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglBase {

    @Test
    fun test_typeModel() {
        val actual = AglBase.typeModel

        assertNotNull(actual)
        val ns = actual.findFirstDefinitionByNameOrNull(SimpleName("NamespaceDefault"))
        assertNotNull(ns)
        assertEquals("net.akehurst.language.base.asm.NamespaceDefault",ns.qualifiedName.value)
        val ns__def = ns.findAllPropertyOrNull(PropertyName("_definition"))
        assertNotNull(ns__def)

        val tm = typeModel("Test",true,actual.namespace) {
            namespace("test") {
                data("TestDefinition")
            }
        }
        val tDef = tm.findFirstDefinitionByNameOrNull(SimpleName("TestDefinition"))
        assertNotNull(tDef)
        val nsOfTDef = ns.type(listOf(tDef.type().asTypeArgument))

        val resProp = nsOfTDef.allResolvedProperty[PropertyName("_definition")]
        assertNotNull(resProp)
        assertEquals("Map",resProp.typeInstance.typeName.value)
        assertEquals(2, resProp.typeInstance.typeArguments.size)
        assertEquals("SimpleName",resProp.typeInstance.typeArguments[0].type.typeName.value)
        assertEquals("TestDefinition",resProp.typeInstance.typeArguments[1].type.typeName.value)
    }


    @Test
    fun s() {
        val str = "a.b.c.d"
        val pqn = str.asPossiblyQualifiedName
        assertTrue(pqn is QualifiedName)
    }
}