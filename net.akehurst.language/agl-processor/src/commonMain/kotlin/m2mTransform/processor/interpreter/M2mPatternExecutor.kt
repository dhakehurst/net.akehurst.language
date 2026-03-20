package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.plus

class M2mPatternExecution(
    val description: String,
    val inputs: List<String>,
    val outputs: List<String>,
    val execution: (evc: EvaluationContext) -> EvaluationContext
) {
    var index = -1 //unset

    /** do 'self' execution after all of these */
    //var doMeAfter = mutableSetOf<M2mPatternExecution>()

    /** do 'self' execution before all of these */
    val doMeBefore = mutableSetOf<M2mPatternExecution>()

    val doMeBeforeAll get() = this.doMeBefore.transitveClosure { it.doMeBefore }
    //val doMeAfterAll get() = this.doMeAfter.transitveClosure { it.doMeAfter }

    fun mustComeBefore(other: M2mPatternExecution): Boolean = when {
        this.doMeBeforeAll.contains(other) -> true
        else -> false
    }

    override fun toString(): String = "[$index] ${description} | ${inputs} -> ${outputs} ^ [${doMeBefore.joinToString { it.index.toString() }}]"
}

class M2mPatternExecutor(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator,
    initialExes: List<M2mPatternExecution>
) {

    companion object {
        const val RESULT = $$"$result"
    }

    internal var _nextTempVarNum = 0
    internal val _executions = initialExes.mapIndexed { index, execution -> execution.also { it.index = index } }.toMutableList()

    fun addExecution(value: M2mPatternExecution) {
        value.index = _executions.size
        _executions.add(value)
    }

    fun executionPlan(): List<M2mPatternExecution> = _executions.topologicalSort(::compareExecutions)

    fun buildAndExecute(tgtName: String, template: PropertyTemplateRhs, lhsType: TypeInstance, evc1: EvaluationContext) {
        build(tgtName, template, lhsType)
        execute(evc1, tgtName)
    }

    fun build(tgtName: String, template: PropertyTemplateRhs, lhsType: TypeInstance) {
        constructExecutions(tgtName, emptySet(), emptySet(), template, lhsType)
    }

    fun execute(evc1: EvaluationContext, tgtName: String): TypedObject {
        val sorted = executionPlan()
        var evc = evc1 // EvaluationContext.of(variables)
        for (count in 0 until sorted.size) {
            val pe = sorted[count]
            evc = pe.execution.invoke(evc)
        }
        return evc.getOrInParent(tgtName) ?: accessorMutator.nothing()
    }

    private fun createTempVariable() = SimpleName("temp${_nextTempVarNum++}")

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    private fun constructExecutions(tgtName: String, doBeforeMe: Set<M2mPatternExecution>, doAfterMe: Set<M2mPatternExecution>, template: PropertyTemplateRhs, lhsType: TypeInstance) {
        when (template) {
            is PropertyTemplateExpression -> constructExecutionsFromPropertyTemplateExpression(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is ObjectTemplate -> constructExecutionsFromObjectTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            is CollectionTemplate -> constructExecutionsFromCollectionTemplate(tgtName, doBeforeMe, doAfterMe, template, lhsType)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    private fun constructExecutionsFromPropertyTemplateExpression(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: PropertyTemplateExpression,
        lhsType: TypeInstance
    ) {
        val inputs = when {
            template.expression is RootExpression -> listOf((template.expression as RootExpression).name)
            else -> emptyList()
        }
        val outputs = listOf(tgtName) //template.identifier?.let { listOf(it.value) } ?: emptyList()
        val exe = M2mPatternExecution($$"$$tgtName := $${template.expression} // Execute expression", inputs, outputs) { evc ->
            val v = createFromExpression(evc, lhsType, template.expression)
            evc.setNamedValue(tgtName, v)
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }
        addExecution(exe)
    }

    private fun constructExecutionsFromObjectTemplate(tgtName: String, doBeforeMe: Set<M2mPatternExecution>, doAfterMe: Set<M2mPatternExecution>, template: ObjectTemplate, lhsType: TypeInstance) {
        val decl = template.type.resolvedDeclaration

        val inputs = emptyList<String>()
        val (id, outputs) = template.identifier?.let { Pair(it, listOf(it.value)) } ?: Pair(createTempVariable(), emptyList())

        val finishSetProperties = M2mPatternExecution($$"} pop EVC; $$tgtName := oldEvc.$self // Finish set properties for $${decl.name.value}: ", emptyList(), emptyList()) { evc ->
            val self = evc.self
            evc.parent!!.also { poppedEvc ->
                self?.let { poppedEvc.setNamedValue(tgtName, self) }
            } // pop the self and other temp vars
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }

        val startSetProperties = M2mPatternExecution($$"{ push new EVC; // Start set properties for $${decl.name.value}", emptyList(), emptyList()) { evc ->
            evc.child(emptyMap())
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(finishSetProperties)
        }

        val creation = M2mPatternExecution($$"} pop EVC; push EVC with $self := $${decl.name.value}(<constructor args>) // Create object ", inputs, outputs) { evc ->
            val rhsMv = createFromRhs(evc, template.type, template)
            val newEvc = evc.parent!!.childSelf(rhsMv[RESULT]!!)
            rhsMv.entries.forEach { (k, v) ->
                if (k != RESULT) {
                    newEvc.setNamedValue(k, v)
                }
            }
            newEvc
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(startSetProperties)
        }

        val startConstructorArgs = M2mPatternExecution($$"{ push new EVC; // Collect constructor args", inputs, outputs) { evc ->
            evc.child(emptyMap())
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(creation)
        }

        addExecution(startSetProperties)
        addExecution(finishSetProperties)
        addExecution(startConstructorArgs)
        addExecution(creation)
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        template.propertyTemplate.forEach { (k, v) ->
            val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
            if (possibleConArgNames.contains(k.value)) {
                constructExecutionForConstructorArg(k.value, setOf(startConstructorArgs), setOf(creation), v, propType)
            } else {
                constructExecutionsFromPropertyTemplate(k.value, setOf(startSetProperties), setOf(finishSetProperties), v, propType)
            }
        }
    }

    private fun constructExecutionForConstructorArg(tgtName: String, doBeforeMe: Set<M2mPatternExecution>, doAfterMe: Set<M2mPatternExecution>, template: PropertyTemplate, lhsType: TypeInstance) {
//        val inputs = emptyList<String>()
//        val outputs = emptyList<String>()
//        val computeArgument = M2mPatternExecution($$"$$tgtName := oldEvc.$self // Constructor argument $$tgtName", inputs, outputs) { evc ->
//            val arg = evc.getOrInParent(tgtName)
//            arg?.let {
//                evc.setNamedValue(tgtName, arg)
//            }
//            evc
//        }.also { self ->
//            self.doAfterMe.addAll(doAfterMe)
//            doBeforeMe.forEach { it.doAfterMe.add(self) }
//        }
//        val start = M2mPatternExecution($$"{ // Constructor argument", inputs, outputs) { evc ->
//            evc
//        }.also { self ->
//            self.doAfterMe.addAll(doAfterMe)
//            self.doAfterMe.add(computeArgument)
//            doBeforeMe.forEach { it.doAfterMe.add(self) }
//        }
        //addExecution(start)
        //addExecution(computeArgument)
        val pn = template.propertyName.value
        val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
        constructExecutions(tgtName, doBeforeMe, doAfterMe, template.rhs, propType)
    }

    private fun constructExecutionsFromPropertyTemplate(tgtName: String, doBeforeMe: Set<M2mPatternExecution>, doAfterMe: Set<M2mPatternExecution>, template: PropertyTemplate, lhsType: TypeInstance) {
        val inputs = emptyList<String>()
        val outputs = emptyList<String>()
        val setProperty = M2mPatternExecution($$"$self.$$tgtName := oldEvc.$$tgtName // Finish property $$tgtName", inputs, outputs) { evc ->
            val self = evc.self!!
            val pv = evc.getOrInParent(tgtName)
            pv?.let {
                setPropertyIfNothing(self, tgtName, pv)
            }
            evc
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }
//        val start = M2mPatternExecution("// Start set property $tgtName", inputs, outputs) { evc ->
//            evc
//        }.also { self ->
//            self.doAfterMe.addAll(doAfterMe)
//            self.doAfterMe.add(setProperty)
//            doBeforeMe.forEach { it.doAfterMe.add(self) }
//        }
        //addExecution(start)
        addExecution(setProperty)
        //val pn = template.propertyName.value
        //val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
        constructExecutions(tgtName, doBeforeMe, setOf(setProperty), template.rhs, lhsType)
    }

    private fun constructExecutionsFromCollectionTemplate(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: CollectionTemplate,
        lhsType: TypeInstance
    ) {
        // TODO: probably more efficient to create each element and addit to the collection, rather than create all elements first.

        val finishElements = M2mPatternExecution($$"} // Finish elements for Collection", emptyList(), emptyList()) { evc ->
            evc
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }

        val startElements = M2mPatternExecution($$"{ push EVC; // Start elements for Collection", emptyList(), emptyList()) { evc ->
            evc.child(mapOf())
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(finishElements)
        }

        val elemIds = template.elements.map { elT ->
            val id = elT.identifier?.value ?: createTempVariable().value
            constructExecutions(id, setOf(startElements), setOf(finishElements), elT, lhsType)
            id
        }

        val inputs = elemIds
        val outputs = template.identifier?.let { listOf(it.value) } ?: emptyList()
        val createCollection = M2mPatternExecution("pop EVC; $tgtName := ${lhsType.typeName.value}( <elements> ) // Create Collection", inputs, outputs) { evc ->
            val rhsMv = createFromRhs(evc, lhsType, template)
            val poppedEvc = evc.parent!!
            poppedEvc.setNamedValue(tgtName, rhsMv[RESULT]!!)
            rhsMv.entries.forEach { (k, v) ->
                if (k != RESULT) {
                    poppedEvc.setNamedValue(k, v)
                }
            }
            poppedEvc
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            finishElements.doMeBefore.add(self)
        }
        addExecution(startElements)
        addExecution(finishElements)
        addExecution(createCollection)
    }

    /**
     * +1 if e1 comes after e2
     * -1 if e2 comes after e1
     * exe1 comes after exe2 when:
     *  exe1.input contains any of exe2.outputs
     *  exe2.doAfterMe contains exe1
     */
    private fun compareExecutions(exe1: M2mPatternExecution, exe2: M2mPatternExecution): Int {
        return when {
            exe2.doMeBefore.contains(exe1) -> 1 // do exe1 after exe2
            exe1.doMeBefore.contains(exe2) -> -1 // do exe2 after exe1
            //exe2.mustComeBefore(exe1) -> 1 // do parent after child
            //exe1.mustComeBefore(exe2) -> -1 // do parent after child
            exe2.outputs.any { exe1.inputs.contains(it) } -> 1 // do outputs before inputs
            exe1.outputs.any { exe2.inputs.contains(it) } -> -1 // do outputs before inputs
            else -> 0
        }
    }

    // --- creation ---
    private fun createFromRhs(evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs): Map<String, TypedObject> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(evc, lhsType, rhs)
        is ObjectTemplate -> createFromObjectTemplate(evc, lhsType, rhs)
        is CollectionTemplate -> createFromCollectionTemplate(evc, lhsType, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    private fun createFromPropertyTemplateExpression(evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Map<String, TypedObject> {
        val id = rhs.identifier?.value
        val existing = evc.namedValues[id]
        return when (existing) {
            null -> {
                val o = createFromExpression(evc, lhsType, rhs.expression)
                val mv = rhs.identifier?.let { mapOf(it.value to o) } ?: emptyMap()
                mv + Pair(RESULT, o)
            }

            else -> emptyMap()
        }

    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    private fun createFromExpression(evc: EvaluationContext, lhsType: TypeInstance, expression: Expression): TypedObject {
        val exprInterp = ExpressionsInterpreterOverTypedObject(accessorMutator, issues)
        return when (expression) {
            is CreateObjectExpression -> {
                exprInterp.constructObject(evc, expression)
            }

            else -> {
                val value = exprInterp.evaluateExpression(evc, expression)
                value
            }
        }
    }

    private fun createFromObjectTemplate(evc: EvaluationContext, lhsType: TypeInstance, template: ObjectTemplate): Map<String, TypedObject> {
        val id = template.identifier?.value
        val existing = evc.namedValues[id]
        return when (existing) {
            null -> {
                val decl = template.type.resolvedDeclaration
                when (decl) {
                    is DataType, is ValueType -> {
                        val matchedVars = mutableMapOf<String, TypedObject>()
                        val constructors = when (decl) {
                            is DataType -> decl.constructors
                            is ValueType -> decl.constructors
                            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                        }
                        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                        val conArgs = mutableMapOf<String, TypedObject>()
                        template.propertyTemplate.forEach { (k, v) ->
                            if (possibleConArgNames.contains(k.value)) {
                                val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                                val rhsMv = createFromRhs(evc, propType, v.rhs) //TODO: Ideally fetch from variable !
                                matchedVars.putAll(rhsMv)
                                rhsMv[RESULT]?.let { conArgs[k.value] = it }
                            }
                        }
                        //.resolveType(tgtObjectGraph.typesDomain)
                        val o = accessorMutator.createStructureValue(template.type.qualifiedTypeName, conArgs)
                        val mv = template.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                        mv + Pair(RESULT, o)
                    }

                    else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
                }
            }

            else -> mapOf(RESULT to existing)
        }
    }

    private fun createFromCollectionTemplate(evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate): Map<String, TypedObject> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.namedValues[it.value] }
        return when {
            null == existing -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject>()
                val els = evc.namedValues.values
                val col = accessorMutator.createCollection(lhsType, els)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                mv + Pair(RESULT, col)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val elements = mutableListOf<TypedObject>()
                accessorMutator.forEachIndexed(existing) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                TODO()
            }
        }
    }

    /**
     * sets the property value from the template (which should be a simple variable reference)
     * returns any new variable matches - I think none
     */
    private fun setPropertyIfNothing(obj: TypedObject, pn: String, pv: TypedObject) {
        val possiblePv = obj.getProperty(pn)
        when {
            accessorMutator.isNothing(possiblePv) -> obj.setProperty(pn, pv)
            else -> Unit
        }
    }

}