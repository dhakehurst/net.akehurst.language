/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.expressions.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * basic reflection: '.$type' (std.Type) and std.Property, over AsmSimple and over Kotlin objects (by reflection)
 */
class test_ExpressionsInterpreter_reflection {

    companion object {
        interface Item {
            val name: String
        }

        data class Circle(override val name: String, val radius: Long) : Item
        data class Square(override val name: String) : Item
        data class Model(val items: List<Item>)

        /** a domain with a language type 'Type', that must be found before 'std.Type'; with Kotlin classes or for AsmSimple */
        fun domain(kotlin: Boolean) = typesDomain("test", true) {
            namespace("ns") {
                interface_("Item", if (kotlin) Item::class else null) {
                    if (kotlin) propertyOfWithBinding<Item, String>(setOf(VAL, CMP, STR), "name", "String", false, accessor = Item::name)
                    else propertyOf(setOf(VAL, CMP, STR), "name", "String", false)
                }
                data("Circle", if (kotlin) Circle::class else null) {
                    supertype("Item")
                    if (kotlin) propertyOfWithBinding<Circle, Long>(setOf(VAL, CMP, STR), "radius", "Integer", false, accessor = Circle::radius)
                    else propertyOf(setOf(VAL, CMP, STR), "radius", "Integer", false)
                }
                data("Square", if (kotlin) Square::class else null) {
                    supertype("Item")
                }
                data("Model", if (kotlin) Model::class else null) {
                    if (kotlin) propertyOfWithBinding<Model, List<Item>>(setOf(VAL, CMP, STR), "items", "List", false, accessor = Model::items) { typeArgument("Item") }
                    else propertyOf(setOf(VAL, CMP, STR), "items", "List", false) { typeArgument("Item") }
                }
                data("Type") {
                    propertyOf(setOf(VAL, CMP, STR), "label", "String", false)
                }
            }
        }

        val kotlinDomain = domain(true)
        val asmDomain = domain(false)

        val kotlinModel = Model(listOf(Circle("c1", 5), Square("s1")))

        val asmModel = asmSimple(typesDomain = asmDomain) {
            element("Model") {
                propertyListOfElement("items") {
                    element("Circle") {
                        propertyString("name", "c1")
                        propertyInteger("radius", 5)
                    }
                    element("Square") {
                        propertyString("name", "s1")
                    }
                }
            }
        }.root[0]

        fun accessorAsmSimple(issues: IssueHolder): ObjectGraphAccessorMutator = objectGraphSimpleAsm(asmDomain, null, issues, LocationMapDefault())
        fun accessorByReflection(issues: IssueHolder): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(kotlinDomain, issues, LocationMapDefault())

        fun test(mkAccessor: (IssueHolder) -> ObjectGraphAccessorMutator, self: Any, expression: String, expected: Any) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = mkAccessor(issues)
            val st = og.typesDomain.findByQualifiedNameOrNull("ns.Model".asQualifiedName)!!.type()
            val interpreter = ExpressionsInterpreterOverTypedObjectCombined(og)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(og.typedAs(self, st)), expression)
            assertTrue(interpreter.issues.errors.isEmpty(), interpreter.issues.toString())
            assertEquals(expected, og.untyped(actual), "for '$expression'")
        }

        suspend fun testSuspend(mkAccessor: (IssueHolder) -> ObjectGraphAccessorMutator, self: Any, expression: String, expected: Any) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = mkAccessor(issues)
            val st = og.typesDomain.findByQualifiedNameOrNull("ns.Model".asQualifiedName)!!.type()
            val interpreter = ExpressionsInterpreterOverTypedObjectCombined(og)
            val actual = interpreter.evaluateStrSuspend(EvaluationContext.ofSelf(og.typedAs(self, st)), expression)
            assertTrue(interpreter.issues.errors.isEmpty(), interpreter.issues.toString())
            assertEquals(expected, og.untyped(actual), "for '$expression'")
        }

        /** each expression, with its expected value, is tested over AsmSimple and over Kotlin objects */
        val cases = listOf(
            $$"$self.$type.name" to "Model",
            $$"$self.$type.qualifiedName" to "ns.Model",
            $$"$self.$type.isData" to true,
            $$"$self.$type.isInterface" to false,
            $$"$self.$type.isNullable" to false,
            // runtime type, not the declared element type 'Item'
            $$"items[0].$type.name" to "Circle",
            $$"items[1].$type.name" to "Square",
            $$"(items[0] as Item).$type.name" to "Circle",
            $$"items.map({ it -> it.$type.name })" to listOf("Circle", "Square"),
            $$"items[0].$type.supertype.map({ t -> t.name })" to listOf("Item"),
            $$"items[0].$type.supertype[0].isInterface" to true,
            $$"items[0].$type.allSupertype.map({ t -> t.name }).contains('Item')" to true,
            $$"items[0].$type.property.size" to 2L,
            $$"items[0].$type.findPropertyByName('radius').name" to "radius",
            $$"items[0].$type.findPropertyByName('radius').type.name" to "Integer",
            $$"items[0].$type.findPropertyByName('radius').type.isPrimitive" to true,
            $$"items[0].$type.findPropertyByName('radius').owner.name" to "Circle",
            $$"items[0].$type.findPropertyByName('name').owner.name" to "Item",
            $$"items[0].$type.findPropertyByName('radius').isComposite" to true,
            $$"items[0].$type.findPropertyByName('radius').isReference" to false,
            $$"items[0].$type.findPropertyByName('radius').isReadOnly" to true,
            $$"items[0].$type.findPropertyByName('radius').isDerived" to false,
            $$"items[0].$type.findPropertyByName('radius').getValue(items[0])" to 5L,
            $$"items[1].$type.findPropertyByName('name').getValue(items[1])" to "s1",
            $$"items[0].$type.findPropertyByName('none')" to Unit,
            $$"$self.$type.findPropertyByName('items').type.isCollection" to true,
            $$"$self.$type.findPropertyByName('items').type.typeArgument[0].name" to "Item",
            $$"items.$type.name" to "List",
            $$"items.$type.typeArgument[0].name" to "Item",
            $$"items[0].$type.conformsTo(items[1].$type.supertype[0])" to true,
            $$"items[1].$type.conformsTo(items[0].$type)" to false,
            $$"'abc'.$type.isPrimitive" to true,
            $$"'abc'.$type.name" to "String",
            $$"items[0].$type.$type.name" to "Type",
            $$"items[0].$type.$type.qualifiedName" to "std.Type",
            $$"items[0].$type.property.map({ p -> p.$type.qualifiedName }).first" to "std.Property",
            $$"items[0] is Item" to true,
        )
    }

    @Test
    fun std_Type_and_Property_are_interfaces() {
        assertTrue(StdLibDefault.Type is net.akehurst.language.types.api.InterfaceType)
        assertTrue(StdLibDefault.Property is net.akehurst.language.types.api.InterfaceType)
    }

    @Test
    fun simple_name_finds_language_type_before_std() {
        val domain = kotlinDomain
        assertEquals("ns.Type", domain.findFirstDefinitionByNameOrNull(SimpleName("Type"))?.qualifiedName?.value)
        assertEquals("std.String", domain.findFirstDefinitionByNameOrNull(SimpleName("String"))?.qualifiedName?.value)
        assertEquals("std.Property", domain.findFirstDefinitionByNameOrNull(SimpleName("Property"))?.qualifiedName?.value)
    }

    @Test
    fun asmSimple() {
        val failed = cases.mapNotNull { (expr, expected) -> runCatching { test(::accessorAsmSimple, asmModel, expr, expected) }.exceptionOrNull()?.let { "$expr: ${it.message}" } }
        assertTrue(failed.isEmpty(), failed.joinToString("\n"))
    }

    @Test
    fun byReflection() {
        val failed = cases.mapNotNull { (expr, expected) -> runCatching { test(::accessorByReflection, kotlinModel, expr, expected) }.exceptionOrNull()?.let { "$expr: ${it.message}" } }
        assertTrue(failed.isEmpty(), failed.joinToString("\n"))
    }

    @Test
    fun byReflection_suspend() = runTest {
        val failed = cases.mapNotNull { (expr, expected) ->
            try {
                testSuspend(::accessorByReflection, kotlinModel, expr, expected); null
            } catch (t: Throwable) {
                "$expr: ${t.message}"
            }
        }
        assertTrue(failed.isEmpty(), failed.joinToString("\n"))
    }

    @Test
    fun type_of_nothing_is_nothing() {
        test(::accessorAsmSimple, asmModel, $$"$nothing.$type", Unit)
        test(::accessorByReflection, kotlinModel, $$"$nothing.$type", Unit)
    }

    @Test
    fun static_type_of_type_special_property() {
        val dt = kotlinDomain.findByQualifiedNameOrNull("ns.Model".asQualifiedName)!!
        val issues = IssueHolder(LanguageProcessorPhase.ALL)
        val typeResolver = ExpressionTypeResolver(kotlinDomain, StdLibDefault, issues)
        assertEquals(StdLibDefault.Type.type(), typeResolver.typeOfExpressionStr($$"items.$type", dt))
        assertEquals(StdLibDefault.String, typeResolver.typeOfExpressionStr($$"items.$type.name", dt))
        assertEquals(StdLibDefault.List.type(listOf(StdLibDefault.Property.type().asTypeArgument)), typeResolver.typeOfExpressionStr($$"$self.$type.property", dt))
        assertTrue(issues.errors.isEmpty(), issues.toString())
    }

    @Test
    fun simple_name_in_namespace_finds_imported_language_type_before_std() {
        // as KerML: a namespace that imports 'std' first, then the namespace that defines 'Type'
        val tm = typesDomain("test", true) {
            namespace("lang.core") {
                interface_("Type") {
                    propertyOf(setOf(VAL, CMP, STR), "ownedRelationship", "String", false)
                }
            }
            namespace("lang.features", listOf("std", "lang.core")) {
                interface_("Feature") {
                    supertype("Type")
                }
            }
        }
        val feature = tm.findByQualifiedNameOrNull("lang.features.Feature".asQualifiedName)!!
        assertEquals("lang.core.Type", feature.supertypes[0].resolvedDefinition.qualifiedName.value)
        assertTrue(feature.allProperty.keys.any { it.value == "ownedRelationship" })
        assertEquals("lang.core.Type", (feature.namespace as net.akehurst.language.types.api.TypesNamespace).findTypeNamed(SimpleName("Type"))?.qualifiedName?.value)
        assertEquals("std.String", (feature.namespace as net.akehurst.language.types.api.TypesNamespace).findTypeNamed(SimpleName("String"))?.qualifiedName?.value)
    }
}
