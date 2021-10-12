package net.akehurst.language.agl.agl.grammar.scopes

class ScopeModel {
    val scopes = mutableListOf<Scope>()
    val references = mutableListOf<ReferenceDefinition>()

    fun isScope(scopeFor: String): Boolean {
        return scopes.any { it.scopeFor == scopeFor }
    }

    fun isReference(typeName: String, propertyName: String): Boolean {
        return references.any {
            it.inTypeName == typeName
                    && it.referringPropertyName == propertyName
        }
    }

    fun getReferablePropertyNameFor(scopeFor: String, typeName: String): String? {
        val scope = scopes.firstOrNull { it.scopeFor == scopeFor }
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.propertyName
    }
    fun getReferredToTypeNameFor(inTypeName: String, referringPropertyName: String): List<String> {
        val def = references.firstOrNull { it.inTypeName == inTypeName && it.referringPropertyName==referringPropertyName }
        return def?.refersToTypeName ?: emptyList()
    }
}

data class Scope(
    val scopeFor: String
) {
    val identifiables = mutableListOf<Identifiable>()
}

data class Identifiable(
    val typeName: String,
    val propertyName: String
)

data class ReferenceDefinition(
    val inTypeName: String,
    val referringPropertyName: String,
    val refersToTypeName: List<String>
)