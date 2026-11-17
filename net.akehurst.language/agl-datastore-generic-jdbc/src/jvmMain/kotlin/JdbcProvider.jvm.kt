package net.akehurst.datastore.generic.jdbc

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity

internal actual fun createJdbcDatastore(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
    JdbcDatastore(datastoreIdentity, configuration)
