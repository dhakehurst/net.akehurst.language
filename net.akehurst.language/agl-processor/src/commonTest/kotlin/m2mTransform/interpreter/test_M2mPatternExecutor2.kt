package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.AsmStructureSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.ExternalGetterAsmSimple
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.PropertyTemplateRhs
import net.akehurst.language.m2mTransform.asm.CollectionTemplateDefault
import net.akehurst.language.m2mTransform.asm.ObjectTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateExpressionDefault
import net.akehurst.language.m2mTransform.builder.patternTemplate
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_M2mPatternExecutor2 {

    private companion object {
        fun doTest(types: TypesDomain, crossRefs: CrossReferenceDomain? = null, tgtType: TypeInstance, template: PropertyTemplateRhs, input: Map<String, Any>, expectedPlanExpression: String, expectedResult: Any) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val accessorMutator = ObjectGraphAccessorMutatorByReflection(
                types, issues, LocationMapDefault(),
                externalGetter = ExternalGetterAsmSimple(types, crossReferenceDomain = crossRefs, issues, LocationMapDefault())
            )
            val sut = M2mPatternExecutor2(issues, accessorMutator, emptyList())

            val tgtName = M2mPatternExecutor.RESULT
            val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v, StdLibDefault.AnyType)) }
            val evc = EvaluationContext.of(typedInput)
            sut.build(evc, tgtName, template, tgtType)
            val actualPlanStr = sut.executionPlan.joinToString("\n") { it.description }
            println(actualPlanStr)
            assertEquals(expectedPlanExpression, actualPlanStr)

            val res = sut.execute(evc, tgtName)
            val actualResult = res
            val expectedTypedResult = accessorMutator.toTypedObject(expectedResult, StdLibDefault.AnyType)
            assertEquals(expectedTypedResult.self.asString(), actualResult.self.asString())
        }
    }

    @Test
    fun executionPlan_unnamed_x() {
        // x
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            expression(null, "x")
        }
        val tgtType = StdLibDefault.Integer
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = $$"""
            §result := x
        """.trimIndent()
        val expectedResult = 1
        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_x() {
        // y : x
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            expression("y", "x")
        }
        val tgtType = StdLibDefault.Integer
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = $$"""
            §result := y := x
        """.trimIndent()
        val expectedResult = 1

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_unnamed_empty_nonSusbset_collection() {
        // []
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection(null, false) { }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf(
            "x" to 1
        )

        val expectedPlan = $$"""
            §result := List()
        """.trimIndent()
        val expectedResult = listOf<String>()

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_empty_nonSusbset_collection() {
        // y: []
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", false) { }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>()

        val expectedPlan = $$"""
            §result := y := List()
        """.trimIndent()
        val expectedResult = listOf<String>()

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_unamed() {
        // y: [ a, b, c]
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", false) {
                element { expression(null, "a") }
                element { expression(null, "b") }
                element { expression(null, "c") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            §result$el0 := a
            §result$el1 := b
            §result$el2 := c
            §result := y := List(§result$el0, §result$el1, §result$el2)
        """.trimIndent()
        val expectedResult = listOf(1, 2, 3)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_nonSusbset_collection_of_named() {
        // y: [ p:a, q:b, r:c ]
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", false) {
                element { expression("p", "a") }
                element { expression("q", "b") }
                element { expression("r", "c") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            §result$el0 := p := a
            §result$el1 := q := b
            §result$el2 := r := c
            §result := y := List(§result$el0, §result$el1, §result$el2)
        """.trimIndent()
        val expectedResult = listOf(1, 2, 3)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
        val template = patternTemplate(types) {
            collection("y", false) {
                element { object_(null, "A") {} }
                element { object_(null, "B") {} }
                element { object_(null, "C") {} }
            }
        }

        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>(
        )

        val expectedPlan = $$"""
            §result$el0 := A(){}
            §result$el1 := B(){}
            §result$el2 := C(){}
            §result := y := List(§result$el0, §result$el1, §result$el2)
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            list {
                element("A") {}
                element("B") {}
                element("C") {}
            }
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
        val template = patternTemplate() {
            collection("y", false) {
                element { expression("p", "r") }
                element { expression("q", "p") }
                element { expression("r", "c") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument))
        val input = mapOf<String, Any>(
            "c" to 3
        )

        val expectedPlan = $$"""
            §result$el0 := p := r := §result.el0
            §result$el1 := q := p := §result.el1
            §result$el2 := r := c
            §result := y := List(§result$el0, §result$el1, §result$el2)
        """.trimIndent()
        val expectedResult = listOf(3, 3, 3)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
        val template = patternTemplate(types) {
            object_(null, "test.A") {
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val input = mapOf<String, Any>(
        )

        val expectedPlan = $$"""
            §result := A(){}
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {}
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
        val template = patternTemplate(types) {
            object_("a", "test.A") {}
        }
        //val template = ObjectTemplateDefault(objType, emptyMap()).also { it.setIdentifierValue(SimpleName("a")) }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val input = mapOf<String, Any>()

        val expectedPlan = $$"""
            §result := a := A(){}
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {}
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_named_noconstructor_with_props() {
        // a: A {
        //   p1 == p
        //   p2 == q
        //   p3 == r
        // }
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    propertyOf(setOf(REF, VAR), "p1", "String")
                    propertyOf(setOf(REF, VAR), "p2", "String")
                    propertyOf(setOf(REF, VAR), "p3", "String")
                }
            }
        }
        val template = patternTemplate(types) {
            object_("a", "test.A") {
                property("p1") { expression(null, "p") }
                property("p2") { expression(null, "q") }
                property("p3") { expression(null, "r") }
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val input = mapOf<String, Any>(
            "p" to 1,
            "q" to 2,
            "r" to 3
        )

        val expectedPlan = $$"""
            §result := a := A(){}
            §result$p1 := p
            §result$p2 := q
            §result$p3 := r
            §result.p1 := §result$p1
            §result.p2 := §result$p2
            §result.p3 := §result$p3
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyInteger("p1", 1)
                propertyInteger("p2", 2)
                propertyInteger("p3", 3)
            }
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
                    constructor_ { parameter(setOf(REF, VAL), "p1", "Integer") }
                    propertyOf(setOf(REF, VAR), "p2", "Integer")
                    propertyOf(setOf(REF, VAR), "p3", "Integer")
                }
            }
        }
        val template = patternTemplate(types) {
            object_("a", "test.A") {
                property("p1") { expression(null, "p") }
                property("p2") { expression(null, "q") }
                property("p3") { expression(null, "r") }
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val input = mapOf<String, Any>(
            "p" to 1,
            "q" to 2,
            "r" to 3
        )

        val expectedPlan = $$"""
            §result$p1 := p
            §result$p2 := q
            §result$p3 := r
            §result := a := A(§result$p1){}
            §result.p2 := §result$p2
            §result.p3 := §result$p3
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyInteger("p1", 1)
                propertyInteger("p2", 2)
                propertyInteger("p3", 3)
            }
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
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
                    constructor_ { parameter(setOf(REF, VAL), "p1", "Integer") }
                    propertyOf(setOf(REF, VAR), "p2", "Integer")
                    propertyOf(setOf(REF, VAR), "p3", "Integer")
                }
            }
        }
        val template = patternTemplate(types) {
            object_("a", "test.A") {
                property("p1") { expression("r", "b") }
                property("p2") { expression("q", "p") }
                property("p3") { expression("p", "r") }
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()
        val input = mapOf<String, Any>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )

        val expectedPlan = $$"""
            // Find 'a' or construct test.A(...)
            {
              a := when {
                $nothing == a -> test.A(
                  p1 := when {
                    $nothing == p1 -> b
                    else -> p1
                  }
                ) { }
                else -> a
              }
              with(a) {
                p2 := when {
                  $nothing == p2 -> p
                  else -> p2
                }
                p3 := when {
                  $nothing == p3 -> r
                  else -> p3
                }
                a
              }
            }
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyInteger("p1", 2)
                propertyInteger("p3", 2)
                propertyInteger("p2", 2)
            }
        }.root[0]


        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_object_already_created_set_props() {
        // a: A {
        //   p2 := q
        //   p3 := r
        // }
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ { parameter(setOf(REF, VAL), "p1", "Integer") }
                    propertyOf(setOf(REF, VAR), "p2", "Integer")
                    propertyOf(setOf(REF, VAR), "p3", "Integer")
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
        val tgtType = objType

        val a = AsmStructureSimple(QualifiedName("test.A")).also {
            it.setProperty(PropertyValueName("p1"), AsmPrimitiveSimple.stdInteger(1), 0)
        }
        val input = mapOf<String, Any>(
            "a" to a,
            "q" to 2,
            "r" to 3
        )

        val expectedPlan = $$"""
            §result := a
            §result$p2 := q
            §result$p3 := r
            §result$p1 := p := §result.p1
            §result.p2 := §result$p2
            §result.p3 := §result$p3
        """.trimIndent()
        val expectedResult = asmSimple(types) {
            element("A") {
                propertyInteger("p1", 1)
                propertyInteger("p2", 2)
                propertyInteger("p3", 3)
            }
        }.root[0]

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun setting_the_owner_by_reference() {
        /*
            a2:A2 {
              id == 'A2-1'
              b == B2 {
                owner == a2
                prop == s
              }
            }
         */
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A2") {
                    constructor_ { parameter(setOf(REF, VAL), "id", "String") }
                    propertyOf(setOf(CMP, VAR), "b", "B2")
                }
                data("B2") {
                    propertyOf(setOf(REF, VAR), "owner", "A2")
                    propertyOf(setOf(CMP, VAR), "prop", "String")
                }
            }
        }
        val crossRefs = crossReferenceDomain("Test") {
            declarationsFor("test") {
                identify("A2", "id")
                reference("B2") {
                    property("owner", listOf("A2"), null)
                }
            }
        }
        val template = patternTemplate(types) {
            object_("a2", "test.A2") {
                property("id") { expression(null, "'A2-1'") }
                property("b") {
                    object_(null, "test.B2") {
                        property("owner") { expression(null, "a2") }
                        property("prop") { expression(null, "s") }
                    }
                }
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A2"))!!.type()
        val input = mapOf<String, Any>(
            "s" to "strValue",
        )
        val expectedPlan = $$"""
            §result$id$rhs := 'A2-1'
            §result$b := B2(){}
            §result$b$prop := s
            §result$id := §result$id$rhs
            §result$b.prop := §result$b$prop
            §result := a2 := A2(§result$id){}
            §result.b := §result$b
            §result$b$owner := a2
            §result$b.owner := §result$b$owner
         """.trimIndent()
        val expectedResult = asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            element("A2") {
                propertyString("id", "A2-1")
                propertyElementExplicitType("b", "B2") {
                    propertyString("prop", "strValue")
                    reference("owner", "A2-1")
                }
            }
        }.root[0]
        doTest(types, crossRefs, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun statemachine_example() {
        /*
        state:State {
          name == n
          machine == sm:StateMachine {
            name == pdn
            owner == pd:PartDefinition {
              name == pdn
              observableStateMachine == sm
            }
            state == [... state]
          }
        }
        */
        val types = typesDomain("Test", true) { //TODO:use full sysmlModel from net.akehusrt.omg
            namespace("test") {
                data("PartDefinition") {
                    constructor_ {
                        parameter(setOf(CMP, VAL), "name", "String")
                    }
                    propertyOf(setOf(REF, VAR), "metaData", "MetaData")
                    propertyOf(setOf(CMP, VAL), "documentation", "String")
                    propertyOf(setOf(CMP, VAL), "observableStatemachine", "StateMachine")
                }
                data("StateMachine") {
                    constructor_ {
                        parameter(setOf(CMP, VAL), "name", "String")
                    }
                    propertyOf(setOf(REF, VAL), "owner", "PartDefinition")
                    propertyOf(setOf(CMP, VAL), "state", "List") { typeArgument("State") }
                }
                data("State") {
                    constructor_ {
                        parameter(setOf(CMP, VAL), "name", "String")
                    }
                    propertyOf(setOf(REF, VAR), "machine", "StateMachine")
                }
            }
        }
        val crossRefs = crossReferenceDomain("Test") {
            declarationsFor("test") {
                identify("PartDefinition", "name")
                identify("StateMachine", "name")
                identify("State", "name")
                reference("StateMachine") {
                    property("owner", listOf("PartDefinition"), null)
                }
                reference("State") {
                    property("machine", listOf("StateMachine"), null)
                }
            }
        }
        val template = patternTemplate(types) {
            object_("state", "test.State") {
                property("name") { expression(null, "n") }
                property("machine") {
                    object_("sm", "test.StateMachine") {
                        property("name") { expression(null, "pdn") }
                        property("owner") {
                            object_("pd", "test.PartDefinition") {
                                property("name") { expression(null, "pdn") }
                                property("observableStateMachine") { expression(null, "sm") }
                            }
                        }
                        property("state") {
                            collection(null, isSubset = true) {
                                element {
                                    expression(null, "state")
                                }
                            }
                        }
                    }
                }
            }
        }

        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.State"))!!.type()
        val input_pd = AsmStructureSimple("test.PartDefinition".asQualifiedName).also {
            it.setSemanticQualifiedPath(listOf("part-1"))
            it.setProperty(PropertyValueName("name"), AsmPrimitiveSimple.stdString("part-1"), 0)
        }
        val input = mapOf<String, Any>(
            "n" to "state-1",
            "pd" to input_pd,
        )
        val expectedPlan = $$"""
            §result$name := n
            §result$machine$owner := pd
            §result := state := State(§result$name){}
            §result$machine$owner$name := pdn := §result$machine$owner.name
            §result$machine$state$el0 := state
            §result$machine$name := pdn
            §result$machine$state := List(§result$machine$state$el0)
            §result$machine := sm := StateMachine(§result$machine$name){}
            §result.machine := §result$machine
            §result$machine.owner := §result$machine$owner
            §result$machine$owner$observableStateMachine := sm
            §result$machine.state := §result$machine$state
            §result$machine$owner.observableStateMachine := §result$machine$owner$observableStateMachine
        """.trimIndent()
        var stateObject: AsmStructure? = null
        val expectedResult = asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine","StateMachine") {
                    propertyString("name", "part-1")
                    reference("owner", "part-1")
                    propertyListOfElement("state") {
                        stateObject = element("State") {
                            propertyString("name", "state-1")
                            reference("machine", "part-1")
                        }
                    }
                }
            }
        }

        doTest(types, crossRefs, tgtType, template, input, expectedPlan, stateObject!!)

        println(input_pd.asString())
        assertEquals(expectedResult.root[0].asString(), input_pd.asString())
    }
}