package net.akehurst.datastore.api

private fun isBrowser(): Boolean = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'")

internal actual fun currentDatastorePlatform(): DatastorePlatform =
    if (isBrowser()) DatastorePlatform.WASM_JS_BROWSER else DatastorePlatform.WASM_JS_NODE
