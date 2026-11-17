package net.akehurst.datastore.generic.jdbc

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastorePlatformNotSupportedException

internal actual fun createJdbcDatastore(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore =
    throw DatastorePlatformNotSupportedException(JdbcProvider.IDENTITY, net.akehurst.datastore.api.DatastorePlatform.current, JdbcProvider().supportedPlatforms)
