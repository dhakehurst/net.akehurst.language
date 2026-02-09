package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.CollectionTemplate
import net.akehurst.language.m2mTransform.api.ObjectTemplate
import net.akehurst.language.m2mTransform.api.PropertyTemplate
import net.akehurst.language.m2mTransform.api.PropertyTemplateExpression
import net.akehurst.language.m2mTransform.api.PropertyTemplateRhs
import net.akehurst.language.m2mTransform.asm.CollectionTemplateDefault
import net.akehurst.language.m2mTransform.asm.ObjectTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateDefault
import net.akehurst.language.m2mTransform.asm.PropertyTemplateExpressionDefault
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get

class M2mPatternExecution<OT : Any>(
    val description: String,
    /** do 'self' execution before argument 'doMeBeforeThis' execution */
    val doMeBeforeThis: M2mPatternExecution<OT>?,
    val inputs: List<String>,
    val outputs: List<String>,
    val execution: (variables: Map<String, TypedObject<OT>>, src: TypedObject<OT>) -> Map<String, TypedObject<OT>>
) {
    override fun toString(): String = "${description} ${inputs} -> ${outputs} ^ ${doMeBeforeThis?.description}"
}

class M2mPatternExecutor<OT : Any>(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator<OT>
) {

    internal var _nextTempVarNum = 0
    internal val _executions = mutableListOf<M2mPatternExecution<OT>>()
    internal lateinit var _simplifiedTemplate: PropertyTemplateRhs

    fun executionPlan(): List<M2mPatternExecution<OT>> = _executions.topologicalSort(::compareExecutions)

    fun buildAndExecute(template: PropertyTemplateRhs, lhsType: TypeInstance, variables: Map<String, TypedObject<OT>>, src: TypedObject<OT>) {
        build(template, lhsType)
        execute(variables, src)
    }

    fun build(template: PropertyTemplateRhs, lhsType: TypeInstance) {
        _simplifiedTemplate = constructExecutions(null, template, lhsType)
    }

    fun execute(variables: Map<String, TypedObject<OT>>, src: TypedObject<OT>) {
        val sorted = executionPlan()
        val matchedVars = variables.toMutableMap()
        for (pe in sorted) {
            val mv = pe.execution.invoke(matchedVars, src)
            matchedVars.putAll(mv)
        }
    }

    internal fun createTempVariable() = SimpleName("temp${_nextTempVarNum++}")

    /**
     * returns a simplified template where all property assignments are from variables
     * any non-identified template is given an artificial id.
     */
    internal fun constructExecutions(parent: M2mPatternExecution<OT>?, template: PropertyTemplateRhs, lhsType: TypeInstance): PropertyTemplateRhs {
        return when (template) {
            is PropertyTemplateExpression -> constructExecutionsFromPropertyTemplateExpression(parent, template, lhsType)
            is ObjectTemplate -> constructExecutionsFromObjectTemplate(parent, template, lhsType)
            is CollectionTemplate -> constructExecutionsFromCollectionTemplate(parent, template, lhsType)
            else -> error("Unknown rhs type ${template::class}")
        }
    }

    internal fun constructExecutionsFromPropertyTemplateExpression(parent: M2mPatternExecution<OT>?, template: PropertyTemplateExpression, lhsType: TypeInstance): PropertyTemplateExpression {
        val inputs = when {
            template.expression is RootExpression -> listOf((template.expression as RootExpression).name)
            else -> emptyList()
        }
        val (id, outputs) = template.identifier?.let { Pair(it, listOf(it.value)) } ?: Pair(createTempVariable(), emptyList())
        val exe = M2mPatternExecution("Execute expression: ${template.expression}", parent, inputs, outputs) { vars, src ->
            val lhsType = StdLibDefault.AnyType // maybe not used!
            val (o, mv) = createFromRhs(vars, lhsType, template)
            mv + Pair(id.value, o)
        }
        _executions.add(exe)
        return PropertyTemplateExpressionDefault(RootExpressionDefault(id.value))
    }

    internal fun constructExecutionsFromObjectTemplate(parent: M2mPatternExecution<OT>?, template: ObjectTemplate, lhsType: TypeInstance): ObjectTemplate {
        val decl = template.type.resolvedDeclaration

        val inputs = emptyList<String>()
        val (id, outputs) = template.identifier?.let { Pair(it, listOf(it.value)) } ?: Pair(createTempVariable(), emptyList())

        val setProperties = M2mPatternExecution("Set properties for: '${decl.name.value}'", parent, emptyList(), emptyList()) { variables, src ->
            setPropertiesFromRhs(src, variables, template)
        }
        val creation = M2mPatternExecution("Create object: '${decl.name.value}'", setProperties, inputs, outputs) { vars, src ->
            val (value, mv) = createFromRhs(vars, template.type, template)
            mv + Pair(id.value, value)
        }
        _executions.add(setProperties)
        _executions.add(creation)
        val constructors = when (decl) {
            is DataType -> decl.constructors
            is ValueType -> decl.constructors
            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
        }
        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
        val simpleProps = template.propertyTemplate.entries.associate { (k, v) ->
            val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
            val pt = if (possibleConArgNames.contains(k.value)) {
                constructExecutionsFromPropertyTemplate(creation, v, propType)
            } else {
                constructExecutionsFromPropertyTemplate(setProperties, v, propType)
            }
            Pair(k, pt)
        }

        return ObjectTemplateDefault(template.type, simpleProps).also {
            it.setIdentifierValue(id)
        }
    }

    internal fun constructExecutionsFromPropertyTemplate(parent: M2mPatternExecution<OT>, template: PropertyTemplate, lhsType: TypeInstance): PropertyTemplate {
        val inputs = emptyList<String>()
        val outputs = emptyList<String>()
        val setProperty = M2mPatternExecution("Set property '${template.propertyName.value}'", parent, inputs, outputs) { vars, src ->
            setPropertyIfNothing(src, vars, template)
        }
        _executions.add(setProperty)
        val pn = template.propertyName.value
        val propType = lhsType.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
        val simpleRhs = constructExecutions(setProperty, template.rhs, propType)
        return PropertyTemplateDefault(template.propertyName, simpleRhs)
    }

    internal fun constructExecutionsFromCollectionTemplate(parent: M2mPatternExecution<OT>?, template: CollectionTemplate, lhsType: TypeInstance): CollectionTemplate {
        val inputs = emptyList<String>()
        val (id, outputs) = template.identifier?.let { Pair(it, listOf(it.value)) } ?: Pair(createTempVariable(), emptyList())
        val createCollection = M2mPatternExecution("Create Collection '${lhsType.typeName.value}'", parent, inputs, outputs) { vars, src ->
            val (value, mv) = createFromRhs(vars, lhsType, template)
            mv + Pair(id.value, value)
        }
        val simpleEls = template.elements.map { elT ->
            constructExecutions(createCollection, elT, lhsType)
        }
        val simpleTemplate = CollectionTemplateDefault(template.isSubset, simpleEls)
        _executions.add(createCollection)
        return simpleTemplate.also {
            it.setIdentifierValue(id)
        }
    }

    /**
     * +1 if e1 depends on e2
     * -1 if e2 depends on e1
     * exe1 depends on exe2 when:
     *  exe1.input contains any of exe2.outputs
     *  exe2.parent == exe1
     */
    internal fun compareExecutions(exe1: M2mPatternExecution<OT>, exe2: M2mPatternExecution<OT>): Int {
        return when {
            exe2.doMeBeforeThis == exe1 -> 1 // do parent after child
            exe1.doMeBeforeThis == exe2 -> -1 // do parent after child
            exe2.outputs.any { exe1.inputs.contains(it) } -> 1 // do outputs before inputs
            exe1.outputs.any { exe2.inputs.contains(it) } -> -1 // do outputs before inputs
            else -> 0
        }
    }

    // --- creation ---
    fun createFromRhs(
        variables: Map<String, TypedObject<OT>>,
        lhsType: TypeInstance,
        rhs: PropertyTemplateRhs
    ): Pair<TypedObject<OT>, Map<String, TypedObject<OT>>> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(variables, lhsType, rhs)
        is ObjectTemplate -> createFromObjectTemplate(variables, lhsType, rhs)
        is CollectionTemplate -> createFromCollectionTemplate(variables, lhsType, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    fun createFromPropertyTemplateExpression(variables: Map<String, TypedObject<OT>>, lhsType: TypeInstance, rhs: PropertyTemplateExpression): Pair<TypedObject<OT>, Map<String, TypedObject<OT>>> {
        val id = rhs.identifier?.value
        val existing = variables[id]
        return when (existing) {
            null -> {
                val o = createFromExpression(variables, lhsType, rhs.expression)
                val mv = rhs.identifier?.let { mapOf(it.value to o) } ?: emptyMap()
                Pair(o, mv)
            }

            else -> Pair(existing, emptyMap())
        }

    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromExpression(variables: Map<String, TypedObject<OT>>, lhsType: TypeInstance, expression: Expression): TypedObject<OT> {
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(accessorMutator, issues)
        return when (expression) {
            is CreateObjectExpression -> {
                val evc = EvaluationContext.of(variables)
                exprInterp.constructObject(evc, expression)
            }

            else -> {
                val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expression)
                value
            }
        }
    }

    fun createFromObjectTemplate(
        variables: Map<String, TypedObject<OT>>,
        lhsType: TypeInstance,
        template: ObjectTemplate
    ): Pair<TypedObject<OT>, Map<String, TypedObject<OT>>> {
        val id = template.identifier?.value
        val existing = variables[id]
        return when (existing) {
            null -> {
                val decl = template.type.resolvedDeclaration
                when (decl) {
                    is DataType, is ValueType -> {
                        val matchedVars = mutableMapOf<String, TypedObject<OT>>()
                        val constructors = when (decl) {
                            is DataType -> decl.constructors
                            is ValueType -> decl.constructors
                            else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                        }
                        val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                        val conArgs = mutableMapOf<String, TypedObject<OT>>()
                        template.propertyTemplate.forEach { (k, v) ->
                            if (possibleConArgNames.contains(k.value)) {
                                val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                                val (value, mv) = createFromRhs(variables, propType, v.rhs) //TODO: Ideally fetch from variable !
                                matchedVars.putAll(mv)
                                conArgs[k.value] = value
                            }
                        }
                        //.resolveType(tgtObjectGraph.typesDomain)
                        val o = accessorMutator.createStructureValue(template.type.qualifiedTypeName, conArgs)
                        val mv = template.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                        Pair(o, mv)
                    }

                    else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
                }
            }

            else -> Pair(existing, variables)
        }
    }

    fun createFromCollectionTemplate(
        variables: Map<String, TypedObject<OT>>,
        lhsType: TypeInstance,
        collectionTemplate: CollectionTemplate
    ): Pair<TypedObject<OT>, Map<String, TypedObject<OT>>> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { variables[it.value] }
        return when {
            null == existing -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject<OT>>()
                val elements = collectionTemplate.elements.map {
                    val (o, mv) = createFromRhs(variables, lhsType.typeArguments[0].type, it)
                    matchedVars.putAll(mv)
                    o
                }
                val col = accessorMutator.createCollection(lhsType.qualifiedTypeName, elements)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                Pair(col, mv)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val elements = mutableListOf<TypedObject<OT>>()
                accessorMutator.forEachIndexed(existing) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                TODO()
            }
        }
    }

    //
    fun setPropertiesFromRhs(obj: TypedObject<OT>, variables: Map<String, TypedObject<OT>>, rhs: PropertyTemplateRhs) = when (rhs) {
        is PropertyTemplateExpression -> variables
        is ObjectTemplate -> setPropertiesFromObjectTemplate(obj, variables, rhs)
        is CollectionTemplate -> variables
        else -> error("Unknown rhs type ${rhs::class}")
    }

    fun setPropertiesFromObjectTemplate(obj: TypedObject<OT>, variables: Map<String, TypedObject<OT>>, template: ObjectTemplate): Map<String, TypedObject<OT>> {
        val decl = template.type.resolvedDeclaration
        return when (decl) {
            // only DataTypes have properties that can be set
            is DataType -> {
                template.propertyTemplate.map { (k, v) ->
                    val propType = obj.type.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                    // TODO: if property is a constructor arg, it is already set, else it will have no value yet
                    // can we deduce this rather than getting all properties and just checking for nothing
                    val possiblePv = accessorMutator.getProperty(obj, k.value)
                    when {
                        accessorMutator.isNothing(possiblePv) -> {
                            val (o, vars) = createFromRhs(variables, propType, v.rhs) //TODO: really just want to set to a variable !
                            accessorMutator.setProperty(obj, k.value, o)
                            vars
                        }

                        else -> variables
                    }
                }.foldRight(emptyMap()) { it, acc -> acc + it }
            }

            else -> variables
        }
    }

    /**
     * sets the property value from the template (which should be a simple variable reference)
     * returns any new variable matches - I think none
     */
    fun setPropertyIfNothing(obj: TypedObject<OT>, variables: Map<String, TypedObject<OT>>, template: PropertyTemplate): Map<String, TypedObject<OT>> {
        val pn = template.propertyName.value
        val possiblePv = accessorMutator.getProperty(obj, pn)
        return when {
            accessorMutator.isNothing(possiblePv) -> {
                val propType = obj.type.allResolvedProperty[PropertyName(pn)]?.typeInstance ?: StdLibDefault.AnyType
                val (pv, vars) = createFromRhs(variables, propType, template.rhs) //TODO: really just want to set to a variable !
                accessorMutator.setProperty(obj, pn, pv)
                vars
            }

            else -> emptyMap()
        }
    }

}