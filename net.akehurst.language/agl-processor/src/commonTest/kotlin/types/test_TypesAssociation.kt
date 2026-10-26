package net.akehurst.language.types.builder

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class test_TypesAssociation {

    private companion object {
        // property 'a' (for end A) is owned by the association, property 'b' (for end B) is added to type A.
        // 'a' is composite: setting a reference on an AsmStructure needs a semanticQualifiedPath, which these objects don't have.
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A")
                data("B")
                association {
                    end("A", setOf(CMP, VAR), "a", ownedByAssociation = true)
                    end("B", setOf(REF, VAR), "b")
                }
            }
        }

        fun TypesDomain.def(name: String): TypeDefinition = findFirstDefinitionByPossiblyQualifiedNameOrNull(name.asPossiblyQualifiedName)!!
    }

    @Test
    fun owned_by_type_is_added_to_the_type() {
        assertNotNull(types.def("test.A").findOwnedPropertyOrNull(PropertyName("b")))
    }

    @Test
    fun owned_by_association_is_not_added_to_the_type() {
        assertNull(types.def("test.B").findOwnedPropertyOrNull(PropertyName("a")))

        val ns = types.findNamespaceOrNull(net.akehurst.language.base.api.QualifiedName("test"))!!
        assertEquals(1, ns.association.size)
        assertEquals(listOf("a"), ns.association[0].ownedProperty.map { it.name.value })
    }

    @Test
    fun owned_by_association_is_found_via_the_domain() {
        val actual = types.findAssociationOwnedPropertyOrNull(types.def("test.B").type(), PropertyName("a"))

        assertNotNull(actual)
        assertSame(types.def("test.A"), actual.typeInstance.resolvedDefinition)
    }

    @Test
    fun owned_by_association_is_navigable_with_accessorMutator() {
        val am = ObjectGraphAccessorMutatorByReflection(types, IssueHolder(), LocationMapDefault())
        val a = am.createStructureValue("test.A".asPossiblyQualifiedName, emptyMap())
        val b = am.createStructureValue("test.B".asPossiblyQualifiedName, emptyMap())

        am.setProperty(b, "a", a)

        assertSame(a.self as AsmStructure, am.getProperty(b, "a").self)
    }

    @Test
    fun asString_shows_association_with_owned_properties() {
        val ns = types.findNamespaceOrNull(net.akehurst.language.base.api.QualifiedName("test"))!!
        println(ns.asString())
        kotlin.test.assertTrue(ns.asString().contains("association {"))
    }

}
