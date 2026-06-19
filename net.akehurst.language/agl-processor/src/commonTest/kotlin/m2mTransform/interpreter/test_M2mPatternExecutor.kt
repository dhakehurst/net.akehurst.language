package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.asm.builder.asmSimple
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
            val accessorMutator = ObjectGraphAccessorMutatorAsmSimple(types, issues, LocationMapDefault())
            val sut = M2mPatternExecutor(issues, accessorMutator, emptyList())

            val tgtName = template.identifier?.value ?: M2mPatternExecutor.RESULT
            sut.build(tgtName, template, lhsType)
            val actualPlan = sut.executionPlan().map { it.toString() }
            println(actualPlan.joinToString("\n"))
            assertEquals(expectedPlan, actualPlan)

            val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v.toAsmSimple, StdLibDefault.AnyType)) }
            val res = sut.execute(EvaluationContext.of(typedInput), tgtName)
            val actualResult = res
            val expectedTypedResult = accessorMutator.toTypedObject(expectedResult.toAsmSimple, StdLibDefault.AnyType)
            assertEquals(expectedTypedResult.self.toAsmSimple.asString(), actualResult.self.toAsmSimple.asString())
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
            $$"[0] $result := x // Execute expression | [x] -> [$result] ^ []"
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
            "[0] y := x // Execute expression | [x] -> [y] ^ []"
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

        val expectedPlan = $$"""
            [0] // Start elements for Collection temp0 | [] -> [] ^ [1]
            [1] // Finish elements for Collection temp0 | [] -> [] ^ [2]
            [2] $result := List( <elements> ) // Create Collection | [] -> [] ^ []
        """.trimIndent().lines()
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
        val input = mapOf<String, Any>()

        val expectedPlan = $$"""
            [0] // Start elements for Collection y | [] -> [] ^ [1]
            [1] // Finish elements for Collection y | [] -> [] ^ [2]
            [2] y := List( <elements> ) // Create Collection | [] -> [y] ^ []
        """.trimIndent().lines()
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
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [3] // Start elements for Collection y | [] -> [] ^ [4, 0, 1, 2]
            [0] temp0 := a // Execute expression | [a] -> [temp0] ^ [4]
            [1] temp1 := b // Execute expression | [b] -> [temp1] ^ [4]
            [2] temp2 := c // Execute expression | [c] -> [temp2] ^ [4]
            [4] // Finish elements for Collection y | [] -> [] ^ [5]
            [5] y := List( <elements> ) // Create Collection | [temp0, temp1, temp2] -> [y] ^ []
        """.trimIndent().lines()
        val expectedResult = listOf(1, 2, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_named() {
        // y: [ p:a, q:b, r:c ]
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
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [3] // Start elements for Collection y | [] -> [] ^ [4, 0, 1, 2]
            [0] p := a // Execute expression | [a] -> [p] ^ [4]
            [1] q := b // Execute expression | [b] -> [q] ^ [4]
            [2] r := c // Execute expression | [c] -> [r] ^ [4]
            [4] // Finish elements for Collection y | [] -> [] ^ [5]
            [5] y := List( <elements> ) // Create Collection | [p, q, r] -> [y] ^ []
        """.trimIndent().lines()
        val expectedResult = listOf(1, 2, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_unnamed_nonSusbset_collection_of_unnamed_object() {
        // [ A(){}, B(){}, C(){} ]
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {}
                data("B") {}
                data("C") {}
            }
        }
        val objTypeA = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val objTypeB = types.findByQualifiedNameOrNull(QualifiedName("test.B"))!!.type()
        val objTypeC = types.findByQualifiedNameOrNull(QualifiedName("test.C"))!!.type()
        val elms = listOf(
            ObjectTemplateDefault(objTypeA, emptyMap()),
            ObjectTemplateDefault(objTypeB, emptyMap()),
            ObjectTemplateDefault(objTypeC, emptyMap()),
        )
        val template = CollectionTemplateDefault(false, elms).also {
            it.setIdentifierValue(SimpleName("y"))
        }
        val lhsType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$$"""
            [12] // Start elements for Collection y | [] -> [] ^ [13, 1, 0, 3, 2, 5, 4, 7, 6, 9, 8, 11, 10]
            [2] // Collect constructor args | [] -> [] ^ [13, 3]
            [6] // Collect constructor args | [] -> [] ^ [13, 7]
            [10] // Collect constructor args | [] -> [] ^ [13, 11]
            [3] temp1 := A(<constructor args>) // Create object  | [] -> [] ^ [13, 0]
            [7] temp3 := B(<constructor args>) // Create object  | [] -> [] ^ [13, 4]
            [11] temp5 := C(<constructor args>) // Create object  | [] -> [] ^ [13, 8]
            [0] // Start set properties for A | [] -> [] ^ [13, 1]
            [4] // Start set properties for B | [] -> [] ^ [13, 5]
            [8] // Start set properties for C | [] -> [] ^ [13, 9]
            [1] temp0 := temp1 // Finish set properties for A:  | [] -> [] ^ [13]
            [5] temp2 := temp3 // Finish set properties for B:  | [] -> [] ^ [13]
            [9] temp4 := temp5 // Finish set properties for C:  | [] -> [] ^ [13]
            [13] // Finish elements for Collection y | [] -> [] ^ [14]
            [14] y := List( <elements> ) // Create Collection | [temp0, temp2, temp4] -> [y] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            list {
                element("A") {}
                element("B") {}
                element("C") {}
            }
        }.root[0]

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_named_and_interdependent() {
        // y: [ p:r, q:p, r:c ]
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
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [3] // Start elements for Collection y | [] -> [] ^ [4, 0, 1, 2]
            [2] r := c // Execute expression | [c] -> [r] ^ [4]
            [0] p := r // Execute expression | [r] -> [p] ^ [4]
            [1] q := p // Execute expression | [p] -> [q] ^ [4]
            [4] // Finish elements for Collection y | [] -> [] ^ [5]
            [5] y := List( <elements> ) // Create Collection | [p, q, r] -> [y] ^ []
        """.trimIndent().lines()
        val expectedResult = listOf(3, 3, 3)

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_unnamed_empty() {
        // A {}
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val template = ObjectTemplateDefault(objType, emptyMap())
        val lhsType = objType
        val input = mapOf<String, Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [2] // Collect constructor args | [] -> [] ^ [3]
            [3] temp0 := A(<constructor args>) // Create object  | [] -> [] ^ [0]
            [0] // Start set properties for A | [] -> [] ^ [1]
            [1] $result := temp0 // Finish set properties for A:  | [] -> [] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            element("A") {}
        }.root[0]

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_empty() {
        // a: A {}
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
        val input = mapOf<String, Any>(
            "r" to 1,
            "p" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [2] // Collect constructor args | [] -> [a] ^ [3]
            [3] a := A(<constructor args>) // Create object  | [] -> [a] ^ [0]
            [0] // Start set properties for A | [] -> [] ^ [1]
            [1] a := a // Finish set properties for A:  | [] -> [] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            element("A") {}
        }.root[0]

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_noconstructor_with_props() {
        // a: A() {
        //   p1 := p
        //   p2 := q
        //   p3 := r
        // }
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    propertyOf(setOf(REF, VAR),"p1","String")
                    propertyOf(setOf(REF, VAR),"p2","String")
                    propertyOf(setOf(REF, VAR),"p3","String")
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
        val input = mapOf<String, Any>(
            "p" to 1,
            "q" to 2,
            "r" to 3
        )

        val expectedPlan = $$"""
            [2] // Collect constructor args | [] -> [a] ^ [3]
            [3] a := A(<constructor args>) // Create object  | [] -> [a] ^ [0]
            [0] // Start set properties for A | [] -> [] ^ [1, 4, 5, 6, 7, 8, 9]
            [5] a_p1 := p // Execute expression | [p] -> [a_p1] ^ [4]
            [7] a_p2 := q // Execute expression | [q] -> [a_p2] ^ [6]
            [9] a_p3 := r // Execute expression | [r] -> [a_p3] ^ [8]
            [4] a.p1 := a_p1 // Finish property p1 | [a, a_p1] -> [] ^ [1]
            [6] a.p2 := a_p2 // Finish property p2 | [a, a_p2] -> [] ^ [1]
            [8] a.p3 := a_p3 // Finish property p3 | [a, a_p3] -> [] ^ [1]
            [1] a := a // Finish set properties for A:  | [] -> [] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyString("p1","1")
                propertyString("p2","2")
                propertyString("p3","3")
            }
        }.root[0]

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_constructor_with_props() {
        // a: A(p1 := p) {
        //   p2 := q
        //   p3 := r
        // }
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(REF, VAL), "p1", "String") }
                    propertyOf(setOf(REF, VAR),"p2","String")
                    propertyOf(setOf(REF, VAR),"p3","String")
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
        val input = mapOf<String, Any>(
            "p" to 1,
            "q" to 2,
            "r" to 3
        )

        val expectedPlan = $$"""
            [2] // Collect constructor args | [] -> [a] ^ [3, 4]
            [4] a_p1 := p // Execute expression | [p] -> [a_p1] ^ [3]
            [3] a := A(<constructor args>) // Create object  | [a_p1] -> [a] ^ [0]
            [0] // Start set properties for A | [] -> [] ^ [1, 5, 6, 7, 8]
            [6] a_p2 := q // Execute expression | [q] -> [a_p2] ^ [5]
            [8] a_p3 := r // Execute expression | [r] -> [a_p3] ^ [7]
            [5] a.p2 := a_p2 // Finish property p2 | [a, a_p2] -> [] ^ [1]
            [7] a.p3 := a_p3 // Finish property p3 | [a, a_p3] -> [] ^ [1]
            [1] a := a // Finish set properties for A:  | [] -> [] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyString("p1","1")
                propertyString("p2","2")
                propertyString("p3","3")
            }
        }.root[0]

        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_constructor_with_props_interdependent() {
        // a: A(p1 := r:b) {
        //   p2 := q:p
        //   p3 := p:r
        // }
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(), "p1", "String") }
                }
            }
        }
        val objType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()

        val props = listOf(
            PropertyTemplateDefault(SimpleName("p1"), PropertyTemplateExpressionDefault(RootExpressionDefault("b")).also { it.setIdentifierValue(SimpleName("r")) }),
            PropertyTemplateDefault(SimpleName("p2"), PropertyTemplateExpressionDefault(RootExpressionDefault("p")).also { it.setIdentifierValue(SimpleName("q")) }),
            PropertyTemplateDefault(SimpleName("p3"), PropertyTemplateExpressionDefault(RootExpressionDefault("r")).also { it.setIdentifierValue(SimpleName("p")) }),
        ).associateBy { it.propertyName }
        val template = ObjectTemplateDefault(objType, props).also { it.setIdentifierValue(SimpleName("a")) }
        val lhsType = objType
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            [2] // Collect constructor args | [] -> [a] ^ [3, 4, 5]
            [5] a_p1 := b // Execute expression | [b] -> [a_p1] ^ [3, 4]
            [4] r := a_p1 // Constructor argument $a_p1 in template is named r | [] -> [r] ^ [3]
            [3] a := A(<constructor args>) // Create object  | [a_p1] -> [a] ^ [0]
            [0] // Start set properties for A | [] -> [] ^ [1, 6, 7, 8, 9]
            [9] a_p3 := r // Execute expression | [r] -> [a_p3] ^ [8]
            [8] a.p3 := a_p3; p = a_p3 // Finish property p3 | [a, a_p3] -> [p] ^ [1]
            [7] a_p2 := p // Execute expression | [p] -> [a_p2] ^ [6]
            [6] a.p2 := a_p2; q = a_p2 // Finish property p2 | [a, a_p2] -> [q] ^ [1]
            [1] a := a // Finish set properties for A:  | [] -> [] ^ []
        """.trimIndent().lines()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyString("p1","2")
                propertyString("p3","2")
                propertyString("p2","2")
            }
        }.root[0]


        doTest(types, lhsType, template, input, expectedPlan, expectedResult)
    }
}