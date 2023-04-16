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

    override var types = mutableMapOf<String, RuleType>()

    override fun findTypeForRule(ruleName: String): RuleType?  = types[ruleName]
}