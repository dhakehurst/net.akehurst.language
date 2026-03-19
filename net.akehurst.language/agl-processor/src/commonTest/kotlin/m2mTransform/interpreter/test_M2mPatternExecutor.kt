package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.language.asm.simple.toAsmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorAsmSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.PropertyTemplateRhs
import net.akehurst.language.m2mTransform.asm.CollectionTemplateDefault
import net.akehurst.language.m2mTransform.asm.ObjectTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateExpressionDefault
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_M2mPatternExecutor {

    private companion object {
        fun doTest(types: TypesDomain, lhsType: TypeInstance, template: PropertyTemplateRhs, input: Map<String, Any>, expectedPlan: List<String>, expectedResult: Any) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val accessorMutator = ObjectGraphAccessorMutatorAsmSimple(types, issues)
            val sut = M2mPatternExecutor(issues, accessorMutator, emptyList())

            val tgtName = template.identifier?.value ?: M2mPatternExecutor.RESULT
            sut.build(tgtName,template, lhsType)
            val actualPlan = sut.executionPlan().map { it.toString() }
            println(actualPlan.joinToString("\n"))
            assertEquals(expectedPlan, actualPlan)

            val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v.toAsmSimple)) }
            val res = sut.execute(EvaluationContext.of(typedInput), tgtName)
            val actualResult = res
            val expectedTypedResult = accessorMutator.toTypedObject(expectedResult.toAsmSimple)
            assertEquals(expectedTypedResult, actualResult)
        }
    }

    @Test
    fun executionPlan_unnamed_x() {
        // x
        val types = typesDomain("Test", true) { }
        val template = PropertyTemplateExpressionDefault(
            RootExpressionDefault("x")
        )
        val lhsType = StdLibDefault.Integer
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = listOf(
            $$"[0] Execute expression: push EVC with $result := x | [x] -> [$result] ^ []"
        )
        val expectedResult = 1
        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_x() {
        // y : x
        val types = typesDomain("Test", true) { }
        val template = PropertyTemplateExpressionDefault(
            RootExpressionDefault("x")
        ).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.Integer
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = listOf(
            "[0] Execute expression: push EVC with y := x | [x] -> [y] ^ []"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_unnamed_empty_nonSusbset_collection() {
        // []
        val types = typesDomain("Test", true) { }
        val template = CollectionTemplateDefault(false, emptyList())
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = listOf(
            $$"[0] { push EVC // Start elements for Collection | [] -> [] ^ [1]",
            $$"[1] } // Finish elements for Collection | [] -> [] ^ [2]",
            $$"[2] Create Collection: $result := List( <elements> ) | [] -> [] ^ []",
        )
        val expectedResult = listOf<String>()

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_empty_nonSusbset_collection() {
        // y: []
        val types = typesDomain("Test", true) { }
        val template = CollectionTemplateDefault(false, emptyList()).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String,Any>()

        val expectedPlan = listOf(
            "[0] { push EVC // Start elements for Collection | [] -> [] ^ [1]",
            "[1] } // Finish elements for Collection | [] -> [] ^ [2]",
            "[2] Create Collection: y := List( <elements> ) | [] -> [y] ^ []",
        )
        val expectedResult = listOf<String>()

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_unamed() {
        // y: [ a, b, c]
        val types = typesDomain("Test", true) { }
        val elms = listOf(
            PropertyTemplateExpressionDefault(RootExpressionDefault("a")),
            PropertyTemplateExpressionDefault(RootExpressionDefault("b")),
            PropertyTemplateExpressionDefault(RootExpressionDefault("c"))
        )
        val template = CollectionTemplateDefault(false, elms).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String,Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "[3] { push EVC // Start elements for Collection | [] -> [] ^ [4, 0, 1, 2]",
            "[0] Execute expression: push EVC with temp0 := a | [a] -> [temp0] ^ [4]",
            "[1] Execute expression: push EVC with temp1 := b | [b] -> [temp1] ^ [4]",
            "[2] Execute expression: push EVC with temp2 := c | [c] -> [temp2] ^ [4]",
            "[4] } // Finish elements for Collection | [] -> [] ^ [5]",
            "[5] Create Collection: y := List( <elements> ) | [temp0, temp1, temp2] -> [y] ^ []"
        )
        val expectedResult = listOf(1, 2, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_named() {
        val types = typesDomain("Test", true) { }
        val elms = listOf(
            PropertyTemplateExpressionDefault(RootExpressionDefault("a")).also { it.setIdentifierValue(SimpleName("p")) },
            PropertyTemplateExpressionDefault(RootExpressionDefault("b")).also { it.setIdentifierValue(SimpleName("q")) },
            PropertyTemplateExpressionDefault(RootExpressionDefault("c")).also { it.setIdentifierValue(SimpleName("r")) },
        )
        val template = CollectionTemplateDefault(false, elms).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String,Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "[3] { push EVC // Start elements for Collection | [] -> [] ^ [4, 0, 1, 2]",
            "[0] Execute expression: push EVC with p := a | [a] -> [p] ^ [4]",
            "[1] Execute expression: push EVC with q := b | [b] -> [q] ^ [4]",
            "[2] Execute expression: push EVC with r := c | [c] -> [r] ^ [4]",
            "[4] } // Finish elements for Collection | [] -> [] ^ [5]",
            "[5] Create Collection: y := List( <elements> ) | [p, q, r] -> [y] ^ []"
        )
        val expectedResult = listOf(1, 2, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_named_and_interdependent() {
        val types = typesDomain("Test", true) { }
        val elms = listOf(
            PropertyTemplateExpressionDefault(RootExpressionDefault("r")).also { it.setIdentifierValue(SimpleName("p")) },
            PropertyTemplateExpressionDefault(RootExpressionDefault("p")).also { it.setIdentifierValue(SimpleName("q")) },
            PropertyTemplateExpressionDefault(RootExpressionDefault("c")).also { it.setIdentifierValue(SimpleName("r")) },
        )
        val template = CollectionTemplateDefault(false, elms).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "[3] { push EVC // Start elements for Collection | [] -> [] ^ [4, 0, 1, 2]",
            "[2] Execute expression: push EVC with r := c | [c] -> [r] ^ [4]",
            "[0] Execute expression: push EVC with p := r | [r] -> [p] ^ [4]",
            "[1] Execute expression: push EVC with q := p | [p] -> [q] ^ [4]",
            "[4] } // Finish elements for Collection | [] -> [] ^ [5]",
            "[5] Create Collection: y := List( <elements> ) | [p, q, r] -> [y] ^ []"
        )
        val expectedResult =  listOf(1, 2, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_unnamed_empty() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(), "p1", "String") }
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val template = ObjectTemplateDefault(objType, emptyMap())
        val lhsType = objType
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            $$"start collect constructor args push new EVC | [] -> [] ^ [Create object: pop EVC; push EVC with $self := A(...) {...}]",
            $$"Create object: pop EVC; push EVC with $self := A(...) {...} | [] -> [] ^ [{ // Start set properties for A]",
            $$"{ // Start set properties for A | [] -> [] ^ [} // Finish set properties for A: pop EVC; $result := oldEvc.$self]",
            $$"} // Finish set properties for A: pop EVC; $result := oldEvc.$self | [] -> [] ^ []"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_empty() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(), "p1", "String") }
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val template = ObjectTemplateDefault(objType, emptyMap()).also { it.setIdentifierValue(SimpleName("a")) }
        val lhsType = objType
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            $$"start collect constructor args push new EVC | [] -> [a] ^ [Create object: pop EVC; push EVC with $self := A(...) {...}]",
           $$"Create object: pop EVC; push EVC with $self := A(...) {...} | [] -> [a] ^ [{ // Start set properties for A]",
            $$"{ // Start set properties for A | [] -> [] ^ [} // Finish set properties for A: pop EVC; $result := oldEvc.$self]",
            $$"} // Finish set properties for A: pop EVC; $result := oldEvc.$self | [] -> [] ^ []"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_noconstructor_with_props() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A")
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val props = listOf(
            PropertyTemplateDefault(SimpleName("p1"), PropertyTemplateExpressionDefault(RootExpressionDefault("p"))),
            PropertyTemplateDefault(SimpleName("p2"), PropertyTemplateExpressionDefault(RootExpressionDefault("q"))),
            PropertyTemplateDefault(SimpleName("p3"), PropertyTemplateExpressionDefault(RootExpressionDefault("r"))),
        ).associateBy { it.propertyName }
        val template = ObjectTemplateDefault(objType, props).also { it.setIdentifierValue(SimpleName("a")) }
        val lhsType = objType
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "Create object: 'A' [] -> [a] ^ Set properties for: 'A'",
            "Execute expression: p [p] -> [] ^ Set property 'p1'",
            "Execute expression: q [q] -> [] ^ Set property 'p2'",
            "Execute expression: r [r] -> [] ^ Set property 'p3'",
            "Set property 'p1' [] -> [] ^ Set properties for: 'A'",
            "Set property 'p2' [] -> [] ^ Set properties for: 'A'",
            "Set property 'p3' [] -> [] ^ Set properties for: 'A'",
            "Set properties for: 'A' [] -> [] ^ null"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_constructor_with_props() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(), "p1", "String") }
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val props = listOf(
            PropertyTemplateDefault(SimpleName("p1"), PropertyTemplateExpressionDefault(RootExpressionDefault("p"))),
            PropertyTemplateDefault(SimpleName("p2"), PropertyTemplateExpressionDefault(RootExpressionDefault("q"))),
            PropertyTemplateDefault(SimpleName("p3"), PropertyTemplateExpressionDefault(RootExpressionDefault("r"))),
        ).associateBy { it.propertyName }
        val template = ObjectTemplateDefault(objType, props).also { it.setIdentifierValue(SimpleName("a")) }
        val lhsType = objType
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "Execute expression: p [p] -> [] ^ Set property 'p1'",
            "Execute expression: q [q] -> [] ^ Set property 'p2'",
            "Execute expression: r [r] -> [] ^ Set property 'p3'",
            "Constructor argument 'p1' [] -> [] ^ Create object: 'A'",
            "Set property 'p2' [] -> [] ^ Set properties for: 'A'",
            "Set property 'p3' [] -> [] ^ Set properties for: 'A'",
            "Create object: 'A' [] -> [a] ^ Set properties for: 'A'",
            "Set properties for: 'A' [] -> [] ^ null"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_constructor_with_props_interdependent() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(), "p1", "String") }
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val props = listOf(
            PropertyTemplateDefault(SimpleName("p1"), PropertyTemplateExpressionDefault(RootExpressionDefault("p"))),
            PropertyTemplateDefault(SimpleName("p2"), PropertyTemplateExpressionDefault(RootExpressionDefault("q"))),
            PropertyTemplateDefault(SimpleName("p3"), PropertyTemplateExpressionDefault(RootExpressionDefault("a"))),
        ).associateBy { it.propertyName }
        val template = ObjectTemplateDefault(objType, props).also { it.setIdentifierValue(SimpleName("a")) }
        val lhsType = objType
        val input = mapOf<String,Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = listOf(
            "Execute expression: p [p] -> [] ^ Set property 'p1'",
            "Execute expression: q [q] -> [] ^ Set property 'p2'",
            "Constructor argument 'p1' [] -> [] ^ Create object: 'A'",
            "Set property 'p2' [] -> [] ^ Set properties for: 'A'",
            "Create object: 'A' [] -> [a] ^ Set properties for: 'A'",
            "Execute expression: a [a] -> [] ^ Set property 'p3'",
            "Set property 'p3' [] -> [] ^ Set properties for: 'A'",
            "Set properties for: 'A' [] -> [] ^ null"
        )
        val expectedResult = 1

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }
}