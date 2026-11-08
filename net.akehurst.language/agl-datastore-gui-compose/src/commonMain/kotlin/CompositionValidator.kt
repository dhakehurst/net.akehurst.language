package net.akehurst.datastore.gui.compose

import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain

/** Finds the issues of everything being edited in a CompositionEditorState. */
internal class CompositionValidator(private val state: CompositionEditorState) {

    private companion object {
        val ASSIGNABLE_PATH = Regex("(\\\$self\\.)?[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
        const val SELF = "\$self"
    }

    /** parsed expressions, by expression text (parsing on every edit of any field would be slow) */
    private val _parsed = mutableMapOf<String, Result<Expression>>()

    fun validate(): List<EditorIssue> {
        val issues = mutableListOf<EditorIssue>()
        validateParts(issues)
        state.connections.forEach { validateConnection(it, issues) }
        return issues
    }

    private fun validateParts(issues: MutableList<EditorIssue>) {
        if (state.parts.isEmpty()) issues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.Composition, "Add at least one part"))
        state.parts.forEach { p ->
            when {
                p.id.isBlank() -> issues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.PartId(p), "Give the part an id"))
                state.parts.count { it.id == p.id } > 1 -> issues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.PartId(p), "Part id '${p.id}' is used by more than one part"))
            }
            configurationIssues(p.datastore, issues) { pid -> IssueLocation.Part(p, pid) }
        }
    }

    private fun validateConnection(c: ConnectionDraft, issues: MutableList<EditorIssue>) {
        fun err(field: ConnectionField?, msg: String, end: EndDraft? = null) = issues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.Connection(c, field, end), msg))
        fun warn(field: ConnectionField?, msg: String, end: EndDraft? = null) = issues.add(EditorIssue(IssueSeverity.WARNING, IssueLocation.Connection(c, field, end), msg))

        if (state.nextPart(c.source) != c.target) err(null, "Part '${c.source.id}' is not followed by '${c.target.id}' in the chain, a connection must join neighbouring parts")
        if (c.targetType.isBlank()) err(ConnectionField.TARGET_TYPE, "Give the target type")
        if (state.connections.count { it.source == c.source && it.target == c.target && it.identity == c.identity } > 1) {
            err(ConnectionField.NAME, "Another connection from '${c.source.id}' to '${c.target.id}' has identity '${c.identity}', give the connections different names")
        }

        val sourceTypes = state.typesOrNull(c.source)
        val targetTypes = state.typesOrNull(c.target)
        if (null != targetTypes && c.targetType.isNotBlank()) {
            when (find(targetTypes, c.targetType)) {
                null -> err(ConnectionField.TARGET_TYPE, "'${c.targetType}' is not a type of part '${c.target.id}'")
                !is DataType -> err(ConnectionField.TARGET_TYPE, "'${c.targetType}' is not a data type")
                else -> Unit
            }
        }

        // the type of $self is the type of the end whose expression is $self
        val selfEnd = c.ends.firstOrNull { it.expression.trim() == SELF }
        val selfType = selfEnd?.let { e -> sourceTypes?.let { find(it, e.typeName) } }

        c.ends.forEach { e ->
            when {
                e.name.isBlank() -> err(ConnectionField.END_NAME, "Give the end a name", e)
                c.ends.count { it.name == e.name } > 1 -> err(ConnectionField.END_NAME, "End name '${e.name}' is used more than once", e)
            }
            when {
                e.typeName.isBlank() -> err(ConnectionField.END_TYPE_NAME, "Give the type of the end", e)
                null != sourceTypes && null == find(sourceTypes, e.typeName) && (null == targetTypes || null == find(targetTypes, e.typeName)) ->
                    warn(ConnectionField.END_TYPE_NAME, "'${e.typeName}' is not a type of part '${c.source.id}' or '${c.target.id}'", e)
            }
            if (e.expression.isBlank()) {
                err(ConnectionField.END_EXPRESSION, "Give the expression", e)
            } else {
                val parsed = parse(e.expression)
                val ex = parsed.exceptionOrNull()
                when {
                    null != ex -> err(ConnectionField.END_EXPRESSION, "The expression does not parse: ${ex.message}", e)
                    null != selfType && null != sourceTypes -> typeCheck(sourceTypes, selfType, parsed.getOrThrow())?.let { warn(ConnectionField.END_EXPRESSION, it, e) }
                }
            }
        }

        // ends that are not DataTypes are constructor arguments of the target type (agl-datastore-api README C6)
        val targetDataType = targetTypes?.let { find(it, c.targetType) } as? DataType
        val ctor = targetDataType?.constructors?.firstOrNull()
        if (null != targetDataType && null != ctor) {
            val paramNames = ctor.parameters.map { it.name.value }.toSet()
            c.ends.filter { it != selfEnd && it.name.isNotBlank() && it.name !in paramNames }.forEach { e ->
                val endType = sourceTypes?.let { find(it, e.typeName) } ?: find(targetTypes, e.typeName)
                if (endType !is DataType) {
                    warn(ConnectionField.END_NAME, "'${e.name}' is not a constructor parameter of '${targetDataType.qualifiedName.value}'", e)
                }
            }
        }

        // can the connection be used for write? (README C13)
        when {
            c.ends.isEmpty() -> Unit
            null == selfEnd -> warn(null, "Cannot be used for write: no end has the expression '$SELF'")
            null != sourceTypes && selfType !is DataType -> warn(null, "Cannot be used for write: the type of the '$SELF' end is not a data type")
        }
        c.ends.filter { it != selfEnd && it.expression.isNotBlank() && ASSIGNABLE_PATH.matches(it.expression.trim()).not() }.forEach { e ->
            warn(ConnectionField.END_EXPRESSION, "Cannot be used for write: the expression is not an assignable property path (a.b.c)", e)
        }
    }

    private fun find(types: TypesDomain, typeName: String): TypeDefinition? =
        try {
            types.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName.trim().asPossiblyQualifiedName)
        } catch (_: Exception) {
            null
        }

    private fun parse(expression: String): Result<Expression> = _parsed.getOrPut(expression) {
        try {
            val result = Agl.registry.agl.expressions.processor!!.process(expression)
            val errors = result.allIssues.errors
            when {
                errors.isNotEmpty() -> Result.failure(IllegalStateException(errors.joinToString("; ") { it.message }))
                null == result.asm -> Result.failure(IllegalStateException("no result"))
                else -> Result.success(result.asm!!)
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    /** a message if the expression cannot be navigated from 'selfType', otherwise null */
    private fun typeCheck(types: TypesDomain, selfType: TypeDefinition, expression: Expression): String? =
        try {
            val resolver = ExpressionTypeResolver(types, selfType.namespace, IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS))
            val t = resolver.typeFor(expression, selfType.type())
            if (t.isNothing) "The expression cannot be evaluated from '${selfType.qualifiedName.value}'" else null
        } catch (ex: Exception) {
            "The expression could not be checked: ${ex.message}"
        }
}

/** adds the issues of a configuration draft: those of each parameter, then the schema checks */
internal fun configurationIssues(draft: ConfigurationDraft, issues: MutableList<EditorIssue>, location: (String?) -> IssueLocation) {
    val paramIssues = draft.parameterIssues
    paramIssues.forEach { (pid, msg) -> issues.add(EditorIssue(IssueSeverity.ERROR, location(pid), msg)) }
    // the schema checks repeat 'Required' for missing parameters, so only show them when each parameter is fine on its own
    if (paramIssues.isEmpty()) {
        draft.schemaIssues.forEach { issues.add(EditorIssue(IssueSeverity.ERROR, location(null), it)) }
    }
}
