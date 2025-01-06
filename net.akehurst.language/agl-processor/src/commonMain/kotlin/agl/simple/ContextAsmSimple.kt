package net.akehurst.language.agl.simple

import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.asm.api.AsmPath
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm

class ContextAsmSimple() : SentenceContext<AsmPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<AsmPath>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    val isEmpty: Boolean get() = rootScope.isEmpty

    var createScopedItem: CreateScopedItem<Asm, AsmPath> = { asm, ref, item -> asm.addToIndex(item); item.path }
    var resolveScopedItem: ResolveScopedItem<Asm, AsmPath> = { asm, ref -> asm.elementIndex[ref] }

    fun asString(): String = "context scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextAsmSimple -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextAsmSimple"
}

