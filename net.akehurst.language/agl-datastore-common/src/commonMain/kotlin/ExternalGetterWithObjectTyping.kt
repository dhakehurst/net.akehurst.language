package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreObjectTyping
import net.akehurst.language.objectgraph.api.ExternalGetter
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain

/**
 * An ExternalGetter that asks each of 'typings' for the type of an object, before 'delegate' (which finds it by its class).
 * The name a typing returns is looked up in 'types'; if it is not found there, the next typing (then 'delegate') is asked.
 * Everything else, including the storage of association links, is done by 'delegate'.
 */
class ExternalGetterWithObjectTyping(
    private val types: TypesDomain,
    private val typings: List<DatastoreObjectTyping>,
    private val delegate: ExternalGetter
) : ExternalGetter by delegate {
    override fun typeFor(obj: Any, ifNotFound: TypeInstance): TypeInstance =
        typings.firstNotNullOfOrNull { t -> t.qualifiedTypeNameOf(obj)?.let { types.findByQualifiedNameOrNull(it)?.type() } }
            ?: delegate.typeFor(obj, ifNotFound)
}
