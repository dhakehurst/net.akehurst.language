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
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class test_M2mPatternExecutor2 {

    private companion object {
        fun doTest(types: TypesDomain, crossRefs: CrossReferenceDomain? = null, tgtType: TypeInstance, template: PropertyTemplateRhs, input: Map<String, Any>, expectedPlanExpression: String, expectedResult: Any) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val accessorMutator = ObjectGraphAccessorMutatorByReflection(
                types, issues, LocationMapDefault(),
                externalGetter = ExternalGetterAsmSimple(types, crossReferenceDomain = crossRefs, issues, LocationMapDefault())
            )
            val initVars = input.keys
            val sut = M2mPatternExecutor2(issues, accessorMutator, initVars, emptyList())

            val tgtName = M2mTransformInterpreter.RESULT
            val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v, StdLibDefault.AnyType)) }
            val evc = EvaluationContext.of(typedInput)
            sut.build(tgtName, template, tgtType)
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
            §result$el2 := r := c
            §result$el0 := p := r
            §result$el1 := q := p
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
            a$p1 := p
            a$p2 := q
            a$p3 := r
            a.p1 := a$p1
            a.p2 := a$p2
            a.p3 := a$p3
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
            a$p1 := p
            a$p2 := q
            a$p3 := r
            §result := a := A(a$p1){}
            a.p2 := a$p2
            a.p3 := a$p3
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
        /*
         a: A {
          p1 == r:b
          p2 == q:p
          p3 == p:r
        }
         */
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
            "b" to 2,
        )

        val expectedPlan = $$"""
            a$p1 := r := b
            §result := a := A(a$p1){}
            a$p3 := p := r
            a$p2 := q := p
            a.p3 := a$p3
            a.p2 := a$p2
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
        //   p2 == q
        //   p3 == r
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
            object_("a", "A") {
                property("p2") { expression(null, "q") }
                property("p3") { expression(null, "r") }
            }
        }
        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type()

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
            a$p2 := q
            a$p3 := r
            a.p2 := a$p2
            a.p3 := a$p3
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
            a2$id$rhs := 'A2-1'
            a2$b$obj$prop := s
            a2$id := a2$id$rhs
            §result := a2 := A2(a2$id){}
            a2$b := B2(){}
            a2$b$obj$owner := a2
            a2.b := a2$b
            a2$b$obj.prop := a2$b$obj$prop
            a2$b$obj.owner := a2$b$obj$owner
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
                    propertyOf(setOf(CMP, VAL), "observableStateMachine", "StateMachine")
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
            state$name := n
            pd$name := pdn := pd.name
            §result := state := State(state$name){}
            sm$name := pdn
            sm$state$col$el0 := state
            state$machine := sm := pd.observableStateMachine ?: StateMachine(sm$name){}
            state.machine := state$machine
            sm$owner := pd
            pd$observableStateMachine := sm
            Synchronize Collection sm$state
            sm.owner := sm$owner
            pd.observableStateMachine := pd$observableStateMachine
            sm.state := sm$state
        """.trimIndent()
        var stateObject: AsmStructure? = null
        val expectedResult = asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine", "StateMachine") {
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

    @Test
    fun statemachine_example2() {
        /*
        pd already has a statemachine

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
                    propertyOf(setOf(CMP, VAL), "observableStateMachine", "StateMachine")
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
        var input_pd: AsmStructure? = null
        asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            input_pd = element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine", "StateMachine") {
                    propertyString("name", "part-1")
                    reference("owner", "part-1")
                    propertyListOfElement("state") {
                        element("State") {
                            propertyString("name", "state-0")
                            reference("machine", "part-1")
                        }
                    }
                }
            }
        }

        val input = mapOf<String, Any>(
            "n" to "state-1",
            "pd" to input_pd!!,
        )
        // The plan is IDENTICAL to statemachine_example: build() sees the same template and the
        // same input variable names, so no static plan can distinguish the two cases.
        // The 'sm := pd.observableStateMachine ?: StateMachine(...)' step decides at RUNTIME:
        // here pd.observableStateMachine is populated, so sm is harvested from the model
        // instead of constructing a fresh StateMachine.
        val expectedPlan = $$"""
            state$name := n
            pd$name := pdn := pd.name
            §result := state := State(state$name){}
            sm$name := pdn
            sm$state$col$el0 := state
            state$machine := sm := pd.observableStateMachine ?: StateMachine(sm$name){}
            state.machine := state$machine
            sm$owner := pd
            pd$observableStateMachine := sm
            Synchronize Collection sm$state
            sm.owner := sm$owner
            pd.observableStateMachine := pd$observableStateMachine
            sm.state := sm$state
        """.trimIndent()
        var stateObject: AsmStructure? = null
        val expectedResult = asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine", "StateMachine") {
                    propertyString("name", "part-1")
                    reference("owner", "part-1")
                    propertyListOfElement("state") {
                        element("State") {
                            propertyString("name", "state-0")
                            reference("machine", "part-1")
                        }
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

    @Test
    fun statemachine_example3() {
        /*
        pd already has a statemachine with 2 states, try to add a transition

        trans:Transition {
          machine == sm:StateMachine {
            name == pdn
            owner == pd:PartDefinition {
              name == pdn
              observableStateMachine == sm
            }
            state == [... src:State { name == srcStateName }, tgt:State { name == tgtStateName } ]
            transition == [... trans]
          }
          source == src //State { name == srcStateName }
          target == tgt // State { name == tgtStateName }
          label == 'event' + exp
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
                    propertyOf(setOf(CMP, VAL), "observableStateMachine", "StateMachine")
                }
                data("StateMachine") {
                    constructor_ {
                        parameter(setOf(CMP, VAL), "name", "String")
                    }
                    propertyOf(setOf(REF, VAL), "owner", "PartDefinition")
                    propertyOf(setOf(CMP, VAL), "state", "List") { typeArgument("State") }
                    propertyOf(setOf(CMP, VAL), "transition", "List") { typeArgument("Transition") }
                }
                data("State") {
                    constructor_ {
                        parameter(setOf(CMP, VAL), "name", "String")
                    }
                    propertyOf(setOf(REF, VAR), "machine", "StateMachine")
                }
                data("Transition") {
                    propertyOf(setOf(REF, VAR), "machine", "StateMachine")
                    propertyOf(setOf(REF, VAR), "source", "State")
                    propertyOf(setOf(REF, VAR), "target", "State")
                    propertyOf(setOf(CMP, VAL), "label", "String")
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
                reference("Transition") {
                    property("machine", listOf("StateMachine"), null)
                    property("source", listOf("State"), null)
                    property("target", listOf("State"), null)
                }
            }
        }
        val template = patternTemplate(types) {
            object_("trans", "test.Transition") {
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
                                    object_("src", "State") {
                                        property("name") { expression(null, "srcStateName") }
                                    }
                                }
                                element {
                                    object_("tgt", "State") {
                                        property("name") { expression(null, "tgtStateName") }
                                    }
                                }
                            }
                        }
                        property("transition") {
                            collection(null, isSubset = true) {
                                element {
                                    expression(null, "trans")
                                }
                            }
                        }
                    }
                }
                property("source") {
                    expression(null, "src")
//                    object_(null, "State") {
//                        property("name") { expression(null, "srcStateName") }
//                    }
                }
                property("target") {
                    expression(null, "tgt")
//                    object_(null, "State") {
//                        property("name") { expression(null, "tgtStateName") }
//                    }
                }
                property("label") {
                    expression(null, "'event ' + exp")
                }
            }
        }

        val tgtType = types.findByQualifiedNameOrNull(QualifiedName("test.State"))!!.type()
        var input_pd: AsmStructure? = null
        asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            input_pd = element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine", "StateMachine") {
                    propertyString("name", "part-1")
                    reference("owner", "part-1")
                    propertyListOfElement("state") {
                        element("State") {
                            propertyString("name", "state-0")
                            reference("machine", "part-1")
                        }
                        element("State") {
                            propertyString("name", "state-1")
                            reference("machine", "part-1")
                        }
                    }
                }
            }
        }

        val input = mapOf<String, Any>(
            "pd" to input_pd!!,
            "srcStateName" to "state-0",
            "tgtStateName" to "state-1",
            "exp" to "<expression>"
        )
        val expectedPlan = $$"""
            §result := trans := Transition(){}
            pd$name := pdn := pd.name
            src$name := srcStateName
            tgt$name := tgtStateName
            trans$label$rhs := 'event ' + exp
            sm$transition$col$el0 := trans
            sm$name := pdn
            trans$label := trans$label$rhs
            trans$machine := sm := pd.observableStateMachine ?: StateMachine(sm$name){}
            trans.label := trans$label
            trans.machine := trans$machine
            sm$owner := pd
            pd$observableStateMachine := sm
            sm$state$col$src := src := sm.state[name==src$name] ?: State(src$name){}
            sm$state$col$tgt := tgt := sm.state[name==tgt$name] ?: State(tgt$name){}
            Synchronize Collection sm$transition
            sm.owner := sm$owner
            pd.observableStateMachine := pd$observableStateMachine
            trans$source := src
            Synchronize Collection sm$state
            trans$target := tgt
            sm.transition := sm$transition
            trans.source := trans$source
            sm.state := sm$state
            trans.target := trans$target
        """.trimIndent()
        var transObject: AsmStructure? = null
        val expectedResult = asmSimple(types, crossReferenceDomain = crossRefs, sentenceContext = contextAsmSimple()) {
            element("PartDefinition") {
                propertyString("name", "part-1")
                propertyElementExplicitType("observableStateMachine", "StateMachine") {
                    propertyString("name", "part-1")
                    reference("owner", "part-1")
                    propertyListOfElement("state") {
                        element("State") {
                            propertyString("name", "state-0")
                            reference("machine", "part-1")
                        }
                        element("State") {
                            propertyString("name", "state-1")
                            reference("machine", "part-1")
                        }
                    }
                    propertyListOfElement("transition") {
                        transObject = element("Transition") {
                            propertyString("label","event <expression>")
                            reference("machine", "part-1")
                            reference("source", "state-0")
                            reference("target", "state-1")
                        }
                    }
                }
            }
        }

        doTest(types, crossRefs, tgtType, template, input, expectedPlan, transObject!!)

        println(input_pd.asString())
        assertEquals(expectedResult.root[0].asString(), input_pd.asString())
    }

    @Test
    fun executionPlan_property_bind_conflict_throws_paradox() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    propertyOf(setOf(REF, VAR), "p1", "Integer")
                    propertyOf(setOf(REF, VAR), "p2", "Integer")
                }
            }
        }
        val template = patternTemplate(types) {
            object_("a", "test.A") {
                property("p1") { expression("v", "x") }
                property("p2") { expression("v", "y") }
            }
        }

        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val accessorMutator = ObjectGraphAccessorMutatorByReflection(
            types, issues, LocationMapDefault(),
            externalGetter = ExternalGetterAsmSimple(types, crossReferenceDomain = null, issues, LocationMapDefault())
        )
        val input = mapOf<String, Any>(
            "x" to 1,
            "y" to 2
        )
        val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v, StdLibDefault.AnyType)) }
        val evc = EvaluationContext.of(typedInput)
        val sut = M2mPatternExecutor2(issues, accessorMutator, input.keys, emptyList())
        sut.build(M2mTransformInterpreter.RESULT, template, types.findByQualifiedNameOrNull(QualifiedName("test.A"))!!.type())

        val ex = assertFailsWith<IllegalStateException> {
            sut.execute(evc, M2mTransformInterpreter.RESULT)
        }
        assertTrue(ex.message?.contains("Paradox") == true)
    }

    @Test
    fun executionPlan_named_subset_collection_unions_with_existing_variable() {
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", true) {
                element { expression(null, "a") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.Integer.asTypeArgument))
        val input = mapOf<String, Any>(
            "y" to listOf(1),
            "a" to 2
        )

        val expectedPlan = $$"""
            §result$el0 := a
            Synchronize Collection §result
        """.trimIndent()
        val expectedResult = listOf(1, 2)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_subset_collection_multiple_elements_unions_with_existing_variable() {
        // y: [... a, b]
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", true) {
                element { expression(null, "a") }
                element { expression(null, "b") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.Integer.asTypeArgument))
        val input = mapOf<String, Any>(
            "y" to listOf(1),
            "a" to 2,
            "b" to 3
        )

        val expectedPlan = $$"""
            §result$el0 := a
            §result$el1 := b
            Synchronize Collection §result
        """.trimIndent()
        val expectedResult = listOf(1, 2, 3)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_named_subset_collection_multiple_elements_without_existing_variable() {
        // y: [... a, b] with no pre-existing y
        val types = typesDomain("Test", true) { }
        val template = patternTemplate() {
            collection("y", true) {
                element { expression(null, "a") }
                element { expression(null, "b") }
            }
        }
        val tgtType = StdLibDefault.List.type(listOf(StdLibDefault.Integer.asTypeArgument))
        val input = mapOf<String, Any>(
            "a" to 2,
            "b" to 3
        )

        val expectedPlan = $$"""
            §result$el0 := a
            §result$el1 := b
            Synchronize Collection §result
        """.trimIndent()
        val expectedResult = listOf(2, 3)

        doTest(types, null, tgtType, template, input, expectedPlan, expectedResult)
    }

    @Test
    fun executionPlan_harvested_variable_is_planned_before_use() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("PartDefinition") {
                    constructor_ { parameter(setOf(CMP, VAL), "name", "String") }
                }
                data("StateMachine") {
                    constructor_ { parameter(setOf(CMP, VAL), "name", "String") }
                    propertyOf(setOf(REF, VAL), "owner", "PartDefinition")
                }
            }
        }
        val template = patternTemplate(types) {
            object_("sm", "test.StateMachine") {
                property("name") { expression(null, "pdn") }
                property("owner") {
                    object_("pd", "test.PartDefinition") {
                        property("name") { expression(null, "pdn") }
                    }
                }
            }
        }

        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val accessorMutator = ObjectGraphAccessorMutatorByReflection(
            types, issues, LocationMapDefault(),
            externalGetter = ExternalGetterAsmSimple(types, crossReferenceDomain = null, issues, LocationMapDefault())
        )
        val inputPd = AsmStructureSimple("test.PartDefinition".asQualifiedName).also {
            it.setProperty(PropertyValueName("name"), AsmPrimitiveSimple.stdString("part-1"), 0)
        }
        val input = mapOf<String, Any>("pd" to inputPd)
        val typedInput = input.entries.associate { (k, v) -> Pair(k, accessorMutator.toTypedObject(v, StdLibDefault.AnyType)) }
        val evc = EvaluationContext.of(typedInput)
        val sut = M2mPatternExecutor2(issues, accessorMutator, input.keys, emptyList())

        sut.build(M2mTransformInterpreter.RESULT, template, types.findByQualifiedNameOrNull(QualifiedName("test.StateMachine"))!!.type())
        val plan = sut.executionPlan.map { it.description }
        val producerIdx = plan.indexOfFirst { it.contains("pdn := pd.name") }
        val consumerIdx = plan.indexOfFirst { it.contains("sm\$name := pdn") }

        assertTrue(producerIdx >= 0, "Expected producer step for pdn from pd.name")
        assertTrue(consumerIdx >= 0, "Expected consumer step using pdn for sm\$name")
        assertTrue(producerIdx < consumerIdx, "Expected pdn producer step to appear before consumer step")

        // Plan-order test only: runtime in this scenario needs cross-reference setup beyond this focused case.
    }

    @Test
    fun equality_constraint_harvesting_documentation() {
        /**
         * This test documents the harvesting behavior for equality constraints in QVT Relations semantics.
         *
         * When we have an equality constraint like:
         *   observableStateMachine == sm:StateMachine { ... }
         *
         * And the property `observableStateMachine` already exists in the input with a value,
         * we HARVEST the value for `sm` from that property, rather than constructing a fresh object.
         *
         * This aligns with official QVT Relations semantics where equality constraints are bidirectional:
         * - If both sides are known, assert equality
         * - If one side is known and the other is unbound, harvest the unbound from the known
         * - If neither is known, defer/construct
         */

        // The tests `statemachine_example()` and `statemachine_example2()` demonstrate this:
        // - Both use the same template and the same input variable names (n, pd), so build()
        //   produces the SAME plan for both - the harvest-vs-construct choice cannot be made statically.
        // - The pre-scan (harvestVariables) records pd.observableStateMachine as a potential
        //   harvest source for `sm` (because `observableStateMachine == sm` appears inside the
        //   known object `pd`).
        // - The plan step `state$machine := sm := pd.observableStateMachine ?: StateMachine(sm$name){}`
        //   decides at RUNTIME:
        //   - example2: pd.observableStateMachine is populated -> sm is harvested from the model
        //   - example:  pd.observableStateMachine is empty -> a fresh StateMachine is constructed

        // A harvest source is only registered when:
        // 1. The parent object is 'known' (from the original input, or transitively harvestable)
        // 2. The parent object has an identifier (e.g., `pd`)
        // 3. The constraint binds a variable to a property of that parent (e.g., `observableStateMachine == sm`)
    }
}