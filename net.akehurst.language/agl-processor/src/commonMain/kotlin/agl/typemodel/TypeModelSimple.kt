package net.akehurst.language.agl.agl.typemodel

import net.akehurst.language.api.typemodel.RuleType
import net.akehurst.language.api.typemodel.TypeModel

class TypeModelSimple(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace,name) {


}

abstract class TypeModelAbstract(
    override val namespace: String,
    override val name: String
) : TypeModel {

    val qualifiedName get() = "$namespace.$name"

    override var allTypes = mutableMapOf<String, RuleType>()

    override fun findTypeForRule(ruleName: String): RuleType?  = allTypes[ruleName]
}