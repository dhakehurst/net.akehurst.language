package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.kotlinx.collections.transitveClosure
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
    val execution: (evc: EvaluationContext) -> Unit
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
        val evc = evc1.child()
        for (count in 0 until sorted.size) {
            val pe = sorted[count]
            pe.execution.invoke(evc)
        }
        return evc.getOrInParent(tgtName) ?: accessorMutator.nothing()
    }

    private fun createTempVariable() = "temp${_nextTempVarNum++}"

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    private fun constructExecutions(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: PropertyTemplateRhs,
        lhsType: TypeInstance
    ): M2mPatternExecution {
        return when (template) {
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
    ): M2mPatternExecution {
        val inputs = when {
            template.expression is RootExpression -> listOf((template.expression as RootExpression).name)
            else -> emptyList()
        }
        val outputs = listOf(tgtName)
        val exe = M2mPatternExecution($$"$$tgtName := $${template.expression} // Execute expression", inputs, outputs) { evc ->
            val v = createFromExpression(evc, lhsType, template.expression)
            evc.setNamedValue(tgtName, v)
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }
        addExecution(exe)
        return exe
    }

    private fun constructExecutionsFromObjectTemplate(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: ObjectTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution {
        val decl = template.type.resolvedDefinition

        val inputs = emptyList<String>()
        val (id, outputs) = template.identifier?.value?.let { Pair(it, listOf(it)) } ?: Pair(createTempVariable(), emptyList())

        val finishSetProperties = M2mPatternExecution("$tgtName := $id // Finish set properties for ${decl.name.value}: ", emptyList(), emptyList()) { evc ->
            val obj = evc.getOrInParent(id)
            obj?.let { evc.setNamedValue(tgtName, obj) }
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }

        val startSetProperties = M2mPatternExecution($$"// Start set properties for $${decl.name.value}", emptyList(), emptyList()) { evc ->
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(finishSetProperties)
        }

        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        val conArgNames = template.propertyTemplate.keys.mapNotNull { k ->
            if (possibleConArgNames.contains(k.value)) {
                Pair(k.value, "${id}_${k.value}")
            } else {
                null
            }
        }.associate { it }
        val creation = M2mPatternExecution("$id := ${decl.name.value}(<constructor args>) // Create object ", conArgNames.values.toList(), outputs) { evc ->
            val rhsMv = createFromObjectTemplate(evc, template.type, template, conArgNames)
            evc.setNamedValue(id, rhsMv[RESULT]!!)
            rhsMv.entries.forEach { (k, v) ->
                if (k != RESULT) {
                    evc.setNamedValue(k, v)
                }
            }
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(startSetProperties)
        }

        val startConstructorArgs = M2mPatternExecution("// Collect constructor args", inputs, outputs) { evc ->
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(creation)
        }

        addExecution(startSetProperties)
        addExecution(finishSetProperties)
        addExecution(startConstructorArgs)
        addExecution(creation)
        var prevArgs = setOf(startConstructorArgs)
        var prevProps = setOf(startSetProperties)
        template.propertyTemplate.forEach { (k, v) ->
            val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
            if (possibleConArgNames.contains(k.value)) {
                constructExecutionForConstructorArg("${id}_${k.value}", prevArgs, setOf(creation), v, propType)
            } else {
                constructExecutionsFromPropertyTemplate(id, k.value, prevProps, setOf(finishSetProperties), v, propType)
            }
        }

        return finishSetProperties
    }

    private fun constructExecutionForConstructorArg(
        argName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: PropertyTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution {
        val tgtName = template.rhs.identifier?.value
        val inputs = emptyList<String>()
        val outputs = tgtName?.let { listOf(tgtName) } ?: emptyList()
        return if (null == tgtName) {
            val pn = template.propertyName.value
            val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
            constructExecutions(argName, doBeforeMe, doAfterMe, template.rhs, propType)
        } else {
            val computeArgument = M2mPatternExecution("$tgtName := $argName // Constructor argument $$argName in template is named $tgtName", inputs, outputs) { evc ->
                val arg = evc.getOrInParent(argName)
                arg?.let {
                    evc.setNamedValue(tgtName, arg)
                }
            }.also { self ->
                self.doMeBefore.addAll(doAfterMe)
                doBeforeMe.forEach { it.doMeBefore.add(self) }
            }
            addExecution(computeArgument)
            val pn = template.propertyName.value
            val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
            constructExecutions(argName, doBeforeMe, doAfterMe + computeArgument, template.rhs, propType)
            computeArgument
        }
    }

    private fun constructExecutionsFromPropertyTemplate(
        objName: String,
        propName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: PropertyTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution {
        val tgtName = template.rhs.identifier?.value
        val inputs = listOf(objName, "${objName}_$propName")
        val outputs = tgtName?.let { listOf(tgtName) } ?: emptyList()
        return if (null == tgtName) {
            val setProperty = M2mPatternExecution("$objName.$propName := ${objName}_$propName // Finish property $propName", inputs, outputs) { evc ->
                val obj = evc.getOrInParent(objName)
                val pv = evc.getOrInParent("${objName}_$propName")
                obj?.let {
                    pv?.let {
                        setPropertyIfNothing(obj, propName, pv)
                    }
                }
            }.also { self ->
                self.doMeBefore.addAll(doAfterMe)
                doBeforeMe.forEach { it.doMeBefore.add(self) }
            }
            addExecution(setProperty)
            constructExecutions("${objName}_$propName", doBeforeMe, setOf(setProperty), template.rhs, lhsType)
            setProperty
        } else {
            val setProperty = M2mPatternExecution("$objName.$propName := ${objName}_$propName; $tgtName = ${objName}_$propName // Finish property $propName", inputs, outputs) { evc ->
                val obj = evc.getOrInParent(objName)
                val pv = evc.getOrInParent("${objName}_$propName")
                pv?.let {
                    obj?.let {
                        setPropertyIfNothing(obj, propName, pv)
                    }
                    evc.setNamedValue(tgtName, pv)
                }
            }.also { self ->
                self.doMeBefore.addAll(doAfterMe)
                doBeforeMe.forEach { it.doMeBefore.add(self) }
            }
            addExecution(setProperty)
            constructExecutions("${objName}_$propName", doBeforeMe, setOf(setProperty), template.rhs, lhsType)
            setProperty
        }
    }

    private fun constructExecutionsFromCollectionTemplate(
        tgtName: String,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        template: CollectionTemplate,
        lhsType: TypeInstance
    ): M2mPatternExecution {
        // TODO: probably more efficient to create each element and addit to the collection, rather than create all elements first.

        val colId = template.identifier?.value ?: createTempVariable()

        val finishElements = M2mPatternExecution("// Finish elements for Collection $colId", emptyList(), emptyList()) { evc ->
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }

        val startElements = M2mPatternExecution("// Start elements for Collection $colId", emptyList(), emptyList()) { evc ->
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(finishElements)
        }

        val elemIds = template.elements.map { elT ->
            val id = elT.identifier?.value ?: createTempVariable()
            // groupedExecutions(emptyList(), emptyList(), setOf(startElements), setOf(finishElements)) { startGroup, finishGroup ->
            val x = constructExecutions(id, setOf(startElements), setOf(finishElements), elT, lhsType)
            //     listOf(x)
            // }
            id
        }

        val inputs = elemIds
        val outputs = template.identifier?.let { listOf(it.value) } ?: emptyList()
        val createCollection = M2mPatternExecution("$tgtName := ${lhsType.typeName.value}( <elements> ) // Create Collection", inputs, outputs) { evc ->
            val rhsMv = createFromCollectionTemplate(evc, lhsType, template, elemIds)
            evc.setNamedValue(tgtName, rhsMv[RESULT]!!)
            rhsMv.entries.forEach { (k, v) ->
                if (k != RESULT) {
                    evc.setNamedValue(k, v)
                }
            }
        }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            finishElements.doMeBefore.add(self)
        }
        addExecution(startElements)
        addExecution(finishElements)
        addExecution(createCollection)
        return createCollection
    }

    fun groupedExecutions(
        inputs: List<String>,
        outputs: List<String>,
        doBeforeMe: Set<M2mPatternExecution>,
        doAfterMe: Set<M2mPatternExecution>,
        content: (startGroup: M2mPatternExecution, finishGroup: M2mPatternExecution) -> List<M2mPatternExecution>
    ): M2mPatternExecution {
        val finishGroup = M2mPatternExecution("}", emptyList(), outputs) { it }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
        }
        val startGroup = M2mPatternExecution("{", inputs, emptyList()) { it }.also { self ->
            self.doMeBefore.addAll(doAfterMe)
            doBeforeMe.forEach { it.doMeBefore.add(self) }
            self.doMeBefore.add(finishGroup)
        }
        addExecution(startGroup)
        addExecution(finishGroup)
        content.invoke(startGroup, finishGroup)
        return finishGroup
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
//    private fun createFromRhs(evc: EvaluationContext, lhsType: TypeInstance, rhs: PropertyTemplateRhs): Map<String, TypedObject> = when (rhs) {
//        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(evc, lhsType, rhs)
//        is ObjectTemplate -> createFromObjectTemplate(evc, lhsType, rhs)
//        is CollectionTemplate -> createFromCollectionTemplate(evc, lhsType, rhs)
//        else -> error("Unknown rhs type ${rhs::class}")
//    }

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
        val exprInterp = ExpressionsInterpreterOverTypedObject(accessorMutator)
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

    private fun createFromObjectTemplate(evc: EvaluationContext, lhsType: TypeInstance, template: ObjectTemplate, conArgNames: Map<String, String>): Map<String, TypedObject> {
        val id = template.identifier?.value
        val existing = evc.namedValues[id]
        return when (existing) {
            null -> {
                val decl = template.type.resolvedDefinition
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
                                val arg = evc.getOrInParent(conArgNames[k.value]!!)
                                arg?.let { conArgs[k.value] = arg }
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

    private fun createFromCollectionTemplate(evc: EvaluationContext, lhsType: TypeInstance, collectionTemplate: CollectionTemplate, elementIds: List<String>): Map<String, TypedObject> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.namedValues[it.value] }
        return when {
            null == existing || accessorMutator.isNothing(existing) -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject>()
                val els = elementIds.mapNotNull { evc.getOrInParent(it) }
                val col = accessorMutator.createCollection(lhsType, els)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                mv + Pair(RESULT, col)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val els = elementIds.mapNotNull { evc.getOrInParent(it) }
                val newEls = accessorMutator.createCollection(existing.type,els)
                val newColl = existing.accessor.collectionUnion(existing, newEls)
                val matchedVars = mutableMapOf<String, TypedObject>()
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, newColl) } ?: matchedVars
                mv + Pair(RESULT, newColl)
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