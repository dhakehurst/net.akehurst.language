package net.akehurst.language.types.builder

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class test_AssociationLinks {

    class Doc(val name: String) {
        override fun toString() = "Doc($name)"
    }

    class Part(val name: String) {
        override fun toString() = "Part($name)"
    }

    data class Key(val id: String)

    private companion object {
        // Doc.part : Part? <-> Part.doc : Doc?        (one to one, both ends owned by the association)
        // Doc.sections : List<Part> <-> Part.section : Doc?  (one to many, both ends owned by the association)
        // Key.part : Part? <-> Part.key : Key?
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("Doc", implementation = Doc::class)
                data("Part", implementation = Part::class)
                data("Key", implementation = Key::class)
                association {
                    end("Doc", setOf(REF, VAR), "doc", isNullable = true, ownedByAssociation = true)
                    end("Part", setOf(REF, VAR), "part", isNullable = true, ownedByAssociation = true)
                }
                association {
                    end("Doc", setOf(REF, VAR), "section", isNullable = true, ownedByAssociation = true)
                    end("Part", setOf(REF, VAR), "sections", collectionTypeName = "List", ownedByAssociation = true)
                }
                association {
                    end("Key", setOf(REF, VAR), "key", isNullable = true, ownedByAssociation = true)
                    end("Part", setOf(REF, VAR), "keyed", isNullable = true, ownedByAssociation = true)
                }
            }
        }

        fun am(types: TypesDomain = this.types): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(types, IssueHolder(), LocationMapDefault())
        fun ObjectGraphAccessorMutator.obj(o: Any): TypedObject = toTypedObject(o, typeFor(o, net.akehurst.language.types.asm.StdLibDefault.AnyType))
    }

    @Test
    fun one_to_one_navigable_both_ways() {
        val am = am()
        val d = Doc("d")
        val p = Part("p")
        am.setProperty(am.obj(p), "doc", am.obj(d))

        assertSame(d, am.getProperty(am.obj(p), "doc").self)
        assertSame(p, am.getProperty(am.obj(d), "part").self)
    }

    @Test
    fun setting_a_single_end_replaces_the_link_at_both_ends() {
        val am = am()
        val d1 = Doc("d1")
        val d2 = Doc("d2")
        val p = Part("p")
        am.setProperty(am.obj(p), "doc", am.obj(d1))
        am.setProperty(am.obj(p), "doc", am.obj(d2))

        assertSame(d2, am.getProperty(am.obj(p), "doc").self)
        assertSame(p, am.getProperty(am.obj(d2), "part").self)
        assertNull(am.getProperty(am.obj(d1), "part").self.takeIf { it is Part })

        // and the opposite end is single too: linking p2 to d2 unlinks p
        val p2 = Part("p2")
        am.setProperty(am.obj(p2), "doc", am.obj(d2))
        assertSame(p2, am.getProperty(am.obj(d2), "part").self)
        assertNull(am.getProperty(am.obj(p), "doc").self.takeIf { it is Doc })
    }

    @Test
    fun one_to_many() {
        val am = am()
        val d = Doc("d")
        val p1 = Part("p1")
        val p2 = Part("p2")
        am.setProperty(am.obj(p1), "section", am.obj(d))
        am.setProperty(am.obj(p2), "section", am.obj(d))

        assertEquals(listOf(p1, p2), (am.getProperty(am.obj(d), "sections").self as List<*>).map { (it as TypedObject).self })
        assertSame(d, am.getProperty(am.obj(p1), "section").self)
    }

    @Test
    fun keyed_by_equals() {
        val am = am()
        val p = Part("p")
        am.setProperty(am.obj(p), "key", am.obj(Key("k1")))

        // an equal Key is the same object at the link end
        assertSame(p, am.getProperty(am.obj(Key("k1")), "keyed").self)
    }

    @Test
    fun objects_are_not_changed_and_parts_types_are_unchanged() {
        val d = types.findFirstDefinitionByPossiblyQualifiedNameOrNull("test.Doc".asPossiblyQualifiedName)!!
        val p = types.findFirstDefinitionByPossiblyQualifiedNameOrNull("test.Part".asPossiblyQualifiedName)!!
        assertNull(d.findOwnedPropertyOrNull(net.akehurst.language.types.api.PropertyName("part")))
        assertNull(p.findOwnedPropertyOrNull(net.akehurst.language.types.api.PropertyName("doc")))
    }

    @Test
    fun links_are_per_accessorMutator() {
        val am1 = am()
        val am2 = am()
        val d = Doc("d")
        val p = Part("p")
        am1.setProperty(am1.obj(p), "doc", am1.obj(d))

        assertNull(am2.getProperty(am2.obj(p), "doc").self.takeIf { it is Doc })
    }

    @Test
    fun end_name_must_be_unique_on_a_type() {
        kotlin.test.assertFailsWith<IllegalStateException> {
            typesDomain("Test", true) {
                namespace("test") {
                    data("Doc", implementation = Doc::class)
                    data("Part", implementation = Part::class)
                    association {
                        end("Doc", setOf(REF, VAR), "doc", isNullable = true, ownedByAssociation = true)
                        end("Part", setOf(REF, VAR), "part", isNullable = true, ownedByAssociation = true)
                    }
                    association {
                        end("Doc", setOf(REF, VAR), "doc", isNullable = true, ownedByAssociation = true)
                        end("Part", setOf(REF, VAR), "other", isNullable = true, ownedByAssociation = true)
                    }
                }
            }
        }
    }
}
