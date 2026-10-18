package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreProviderConfiguration
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

fun datastoreProviderConfiguration(availableTypes:TypesDomain = typesDomain("Default", true){}, init: DatastoreProviderConfigurationBuilder.() -> Unit): DatastoreProviderConfiguration {
    val b = DatastoreProviderConfigurationBuilder(availableTypes)
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreProviderConfigurationDslMarker

@DatastoreProviderConfigurationDslMarker
class DatastoreProviderConfigurationBuilder(
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

    fun build() = object : DatastoreProviderConfiguration {
        override val datastore = _datastore
        override val access = _access
    }
}

@DatastoreProviderConfigurationDslMarker
class DsParameterSetBuilder(
    val _availableTypes: TypesDomain,
) {

    val _parameterInfo = mutableListOf<DsConfigParameterInfo>()

    fun group(name:String, init: DsParameterSetBuilder.() -> Unit) {
        val b = DsParameterSetBuilder(_availableTypes)
        b.init()
        val paramSet = b.build()
        _parameterInfo.add(object : DsConfigParameterGroup {
            override val name = name
            override val parameterInfo = paramSet.parameterInfo
        })
    }

    fun choice(init: DsParameterSetChoiceBuilder.() -> Unit) {
        val b = DsParameterSetChoiceBuilder(_availableTypes)
        b.init()
        val x = b.build()
    }

    fun parameter(id:String, name: String, optional:Boolean, init: DsParameterBuilder.() -> Unit) {
        val b = DsParameterBuilder(_availableTypes,id, name, optional)
        b.init()
        val pd = b.build()
        _parameterInfo.add(pd)
    }

    fun build() = object : DsParameterSet {
        override val parameterInfo = _parameterInfo
    }
}

@DatastoreProviderConfigurationDslMarker
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

@DatastoreProviderConfigurationDslMarker
class DsParameterBuilder(
    val _availableTypes: TypesDomain,
    val _identity: String,
    val _name: String,
    val _optional: Boolean
) {
    var _description: String = ""
    lateinit var _type: TypeDefinition

    fun description(value: String) {
        _description = value
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
    }

}