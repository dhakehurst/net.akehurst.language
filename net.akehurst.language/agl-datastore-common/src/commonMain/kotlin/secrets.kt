package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.SecretReference
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistry
import net.akehurst.language.types.asm.StdLibDefault

/**
 * This configuration with each SecretReference argument replaced by the secret it refers to, read from 'registry'.
 * A secret is a String; for a parameter of type Integer, Real or Boolean it is converted.
 * Fails if a source is not registered, or a secret is not found or cannot be converted; the message never contains a secret.
 * Returns this configuration if it has no references.
 */
suspend fun DatastoreConfiguration.withSecretsResolved(registry: SecretSourceRegistry): DatastoreConfiguration {
    if (argument.values.none { it is SecretReference }) return this
    val resolved = argument.mapValues { (id, value) ->
        when (value) {
            is SecretReference -> {
                val source = registry.getOrNull(value.source)
                    ?: error("Parameter '$id': secret source '${value.source}' is not registered")
                val secret = source.read(value.key)
                    ?: error("Parameter '$id': secret '${value.key}' not found in source '${value.source}'")
                convertSecret(id, secret)
            }

            else -> value
        }
    }
    return when (this) {
        is DatastoreConfigurationDefault -> DatastoreConfigurationDefault(providerIdentity, resolved, parameterSet)
        else -> {
            val original = this
            object : DatastoreConfiguration {
                override val providerIdentity = original.providerIdentity
                override val argument = resolved
                override fun isSensitive(parameterIdentity: String) = original.isSensitive(parameterIdentity)
                override fun toString(): String = original.toString()
            }
        }
    }
}

private fun DatastoreConfiguration.convertSecret(id: String, secret: String): Any {
    val type = (this as? DatastoreConfigurationDefault)?.parameterSet?.getParameterOrNull(id)?.type?.qualifiedName
    return when (type) {
        StdLibDefault.Integer.qualifiedTypeName -> secret.toLongOrNull() ?: error("Parameter '$id': the secret is not an Integer")
        StdLibDefault.Real.qualifiedTypeName -> secret.toDoubleOrNull() ?: error("Parameter '$id': the secret is not a Real")
        StdLibDefault.Boolean.qualifiedTypeName -> secret.toBooleanStrictOrNull() ?: error("Parameter '$id': the secret is not a Boolean")
        else -> secret
    }
}
