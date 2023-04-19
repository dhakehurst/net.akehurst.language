package net.akehurst.language.agl.agl.typemodel

import net.akehurst.language.api.typemodel.ElementType
import net.akehurst.language.api.typemodel.RuleType
import net.akehurst.language.api.typemodel.TypeModel

class TypeModelSimple(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace, name) {

    fun addTypeFor(grammarRuleName: String, type: RuleType) {
        super.allTypesByRuleName[grammarRuleName] = type
        super.allTypesByName[type.name] = type
    }

    fun findOrCreateTypeFor(grammarRuleName: String, typeName: String): ElementType {
        val existing = findOrCreateTypeNamed(typeName)
        super.allTypesByRuleName[grammarRuleName] = existing
        return existing
    }

    fun findOrCreateTypeNamed(typeName: String): ElementType {
        val existing = findTypeNamed(typeName)
        return if (null == existing) {
            val t = ElementType(this, typeName)
            super.allTypesByName[typeName] = t
            t
        } else {
            existing as ElementType
        }
    }
}

abstract class TypeModelAbstract(
    override val namespace: String,
    override val name: String
) : TypeModel {

    val qualifiedName get() = "$namespace.$name"

    /**
     * RuleType.name --> RuleType
     */
    override val allTypesByName = mutableMapOf<String, RuleType>()

    /**
     * GrammarRule.name --> RuleType
     */
    override var allTypesByRuleName = mutableMapOf<String, RuleType>()

    override fun findTypeForRule(ruleName: String): RuleType? = allTypesByRuleName[ruleName]
    override fun findTypeNamed(typeName: String): RuleType? = allTypesByName[typeName]
}