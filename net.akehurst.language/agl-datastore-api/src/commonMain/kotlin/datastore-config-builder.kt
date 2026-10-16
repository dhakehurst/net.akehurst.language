package net.akehurst.datastore.api

fun datastoreConfiguration(init: DatastoreConfigurationBuilder.() -> Unit): DatastoreConfiguration {
    val b = DatastoreConfigurationBuilder()
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreConfigurationDslMarker

@DatastoreProviderConfigurationDslMarker
class DatastoreConfigurationBuilder() {

    val _argument= mutableMapOf<String, Any>()

    fun argument(key: String, value: Any) {
        _argument[key] = value
    }

    fun build() = object : DatastoreConfiguration {
        override val argument = _argument
    }
}