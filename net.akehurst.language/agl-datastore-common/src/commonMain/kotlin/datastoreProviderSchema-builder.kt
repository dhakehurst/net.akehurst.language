package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreProviderSchema
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

fun datastoreProviderSchema(availableTypes:TypesDomain = typesDomain("Default", true){}, init: DatastoreProviderSchemaBuilder.() -> Unit): DatastoreProviderSchema {
    val b = DatastoreProviderSchemaBuilder(availableTypes)
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreProviderSchemaDslMarker

@DatastoreProviderSchemaDslMarker
class DatastoreProviderSchemaBuilder(
    val _availableTypes: TypesDomain,
) {

    lateinit var _datastore: DsParameterSet
    lateinit var _access: DsParameterSet

    fun datastore(init: DsParameterSetBuilder.() -> Unit) {
        val b = DsParameterSetBuilder(_availableTypes)
        b.init()
        _datastore = b.build()
    }

    fun access(init: DsParameterSetBuilder.() -> Unit) {
        val b = DsParameterSetBuilder(_availableTypes)
        b.init()
        _access = b.build()
    }

    fun build() = DatastoreProviderSchemaDefault(_datastore, _access)
}

@DatastoreProviderSchemaDslMarker
class DsParameterSetBuilder(
    val _availableTypes: TypesDomain,
) {

    val _parameterInfo = mutableListOf<DsConfigParameterInfo>()

    fun group(name:String, init: DsParameterSetBuilder.() -> Unit) {
        val b = DsParameterSetBuilder(_availableTypes)
        b.init()
        val ps = b.build()
        _parameterInfo.add(object : DsConfigParameterGroup {
            override val name = name
            override val parameterInfo = ps.parameterInfo
        })
    }

    fun choice(init: DsParameterSetChoiceBuilder.() -> Unit) {
        val b = DsParameterSetChoiceBuilder(_availableTypes)
        b.init()
        val ch = b.build()
        _parameterInfo.add(ch)
    }

    fun parameter(id:String, name: String, optional:Boolean, init: DsParameterBuilder.() -> Unit) {
        val b = DsParameterBuilder(_availableTypes,id, name, optional)
        b.init()
        val pd = b.build()
        _parameterInfo.add(pd)
    }

    fun build() = DsParameterSetDefault(_parameterInfo)

}

@DatastoreProviderSchemaDslMarker
class DsParameterSetChoiceBuilder(
    val _availableTypes: TypesDomain,
) {

    val _alternative = mutableMapOf<String, DsParameterSet>()
    fun alternative(name:String, init: DsParameterSetBuilder.() -> Unit) {
        val b = DsParameterSetBuilder(_availableTypes)
        b.init()
        val paramSet = b.build()
        _alternative[name] = paramSet
    }

    fun build() = object : DsConfigParameterChoice {
        override val alternative = _alternative
    }
}

@DatastoreProviderSchemaDslMarker
class DsParameterBuilder(
    val _availableTypes: TypesDomain,
    val _identity: String,
    val _name: String,
    val _optional: Boolean
) {
    var _description: String = ""
    var _sensitive: Boolean = false
    lateinit var _type: TypeDefinition

    fun description(value: String) {
        _description = value
    }

    /** marks the parameter as sensitive (e.g. a password or token), so its value is masked when shown */
    fun sensitive(value: Boolean = true) {
        _sensitive = value
    }

    fun type(typeName: String) {
        _type = _availableTypes.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName.asPossiblyQualifiedName)
            ?: error("Type $typeName not found in available types")
    }

    fun build() = object : DsConfigParameterDefinition {
        override val identity = _identity
        override val name = _name
        override val optional = _optional
        override val description = _description
        override val type = _type
        override val sensitive = _sensitive
    }

}