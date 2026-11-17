package net.akehurst.datastore.api

private fun isBrowser(): Boolean = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean

internal actual fun currentDatastorePlatform(): DatastorePlatform =
    if (isBrowser()) DatastorePlatform.JS_BROWSER else DatastorePlatform.JS_NODE
