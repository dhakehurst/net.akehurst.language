package net.akehurst.language.agl.default

import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.scope.ScopeSimple
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class ContextAsmDefault() : SentenceContext<AsmPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<AsmPath>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    val isEmpty: Boolean get() = rootScope.isEmpty

    fun asString(): String = "context scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextAsmDefault -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextAsmDefault"
}

