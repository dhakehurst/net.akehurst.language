package net.akehurst.datastore.api

/**
 * A platform that a datastore provider can run on.
 * JS and WasmJs are split into browser and Node, because some sources can be reached in one but not the other
 * (e.g. the filesystem datastore uses the browser's File System Access API).
 */
enum class DatastorePlatform {
    JVM,
    JS_BROWSER,
    JS_NODE,
    WASM_JS_BROWSER,
    WASM_JS_NODE,
    NATIVE;

    companion object {
        /** every platform */
        val ALL: Set<DatastorePlatform> = entries.toSet()

        /** the platform this code is running on (for JS and WasmJs, found when first asked: browser if there is a window document) */
        val current: DatastorePlatform by lazy { currentDatastorePlatform() }
    }
}

internal expect fun currentDatastorePlatform(): DatastorePlatform

/**
 * The role of the application (or the half of a client/server application) that is running, given by the application,
 * because it cannot be found from the platform: a client may run on the JVM (e.g. a desktop app) and a server on Node.
 */
enum class DatastoreRole {
    CLIENT,
    SERVER
}

/** Where a datastore may be used, chosen by the user in its datastore configuration (DatastoreConfiguration.location). */
enum class DatastoreLocation {
    /** wherever its provider is supported */
    ANY,

    /** only by an application running as the client, e.g. a browser */
    CLIENT,

    /** only by an application running as the server, e.g. so that a password never reaches a browser */
    SERVER;

    /** true if a datastore with this location may be used by an application running as 'role' */
    fun allows(role: DatastoreRole): Boolean = when (this) {
        ANY -> true
        CLIENT -> role == DatastoreRole.CLIENT
        SERVER -> role == DatastoreRole.SERVER
    }
}

/**
 * Thrown by DatastoreProvider.provide (and DatastoreProviderRegistry.provide) when the provider is not supported on the current platform.
 */
class DatastorePlatformNotSupportedException(
    val providerIdentity: DatastoreProviderIdentity,
    val platform: DatastorePlatform,
    val supportedPlatforms: Set<DatastorePlatform>,
) : UnsupportedOperationException(
    "Datastore provider '${providerIdentity.value}' is not supported on $platform; it is supported on ${supportedPlatforms.joinToString().ifEmpty { "no platform" }}"
)
