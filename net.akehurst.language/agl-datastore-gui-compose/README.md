# AGL Datastore GUI (Compose)

A reusable Compose Multiplatform UI for editing datastore compositions, to be embedded in an application's own GUI.
It is UI only: all code is in `commonMain`, and the host application supplies the behaviour (tests, types,
saving and loading) through callbacks. It never creates, opens or reads datastores itself.

## Use

```kotlin
val state = remember {
    CompositionEditorState(
        registry = registry,                     // a DatastoreProviderRegistry; default DatastoreProviderRegistryDefault.GLOBAL
        secretSourceIds = listOf("keychain", "env"),
        initial = loadedConfiguration,           // optional, loading is done by the application
    )
}
val callbacks = EditorCallbacks(
    testConnection = { partId, provider, cfg -> /* e.g. provide and open the datastore */ TestResult.Success() },
    testAccess = { partId, provider, cfg, access -> /* e.g. read with 'access' */ TestResult.Success() },
    // an exception thrown is shown as 'not available' with its message
    typesFor = { partId, provider, cfg -> TypesResult.Found(registry.provide(provider, partId, cfg).types()) },
)
DatastoreCompositionEditor(state, callbacks = callbacks, customisation = EditorCustomisation(
    toolbarActions = { Button(onClick = { save(state.build()) }, enabled = !state.hasErrors) { Text("Save") } },
    partActions = { part -> TextButton(onClick = { showTypes(part) }) { Text("Show types") } },
))

val access = remember(state) { AccessEditorState(state) }
AccessConfigurationEditor(access, callbacks = callbacks)
```

`state.build()` and `access.build()` return the configurations, or null while there are errors (`state.issues`).

## Requirements

Agreed with David on 2026-11-06 (design doc: https://claude.ai/artifact/FnMWtm8dezjxPge7BMhyF7).

| Id | Requirement | Status |
|----|-------------|--------|
| G1 | All GUI code is in commonMain (target independent). The GUI is UI only, not application function. | done |
| G2 | The state (`CompositionEditorState`, `AccessEditorState`) is held by the host; the composables only show and edit it. Values are kept as typed, so an unfinished value can stay in a form, and are converted when built. | done |
| G3 | Parts are added (provider and id), removed and reordered. Connections join neighbouring parts; several per pair (fan-out). | done |
| G4 | Forms are generated from the provider's `DsParameterSet`: a field per parameter by type (String, Integer, Real, Boolean, other as text), a titled section per group, radio buttons per choice. | done |
| G5 | A parameter's description is behind a help icon ('?' in the primary colour), shown as a tooltip on hover, or on click/tap. | done |
| G6 | A sensitive parameter is given as a masked value or as `secret(source, key)`; the source ids are passed in by the application. | done |
| G7 | 'Test connection' (per part) and 'Test access' (per part's access) call lambdas supplied by the application. With no lambda, or while the configuration has errors, the button is disabled. There is no default test. | done |
| G8 | The types of a part come from the application's `typesFor` lambda (see G14); with them the target type and end type fields offer the types, and target types and expressions are checked. Without them the fields are plain text. | done |
| G9 | Issues are recomputed on every edit and shown next to their field and in an issue list; schema checks are those of the configuration builders. Connections that cannot be used for write (README C13) give a warning. | done |
| G10 | End expressions are plain text fields, parsed with AGL's expression processor. A code-editor field (highlighting, completion) is for later, through a custom parameter renderer. | done (code editor: todo) |
| G11 | `EditorCustomisation`: parameter renderers, provider presentation, strings (labels), toolbar actions; the host's `MaterialTheme` is used. | done |
| G12 | Saving and loading are done by the application, for now. | done |
| G13 | The providers come from a `DatastoreProviderRegistry` (`registry.providers`), by default `DatastoreProviderRegistryDefault.GLOBAL`. A second constructor takes a list of providers. | done |
| G14 | `typesFor` returns a `TypesResult`: `Found(types, itemTypes)` or `NotAvailable(reason)`; an exception becomes `NotAvailable(message)`. The state keeps a `TypesStatus` per part (loading, found, not available), shown in the part's detail as "Types: loading", "Types: N types" (N = type definitions not in the std namespace) or "Types: not available: reason", with a Retry button. The types are asked for again whenever the part's configuration changes. | done |
| G15 | `EditorCustomisation.partActions` and `connectionActions`: the host's buttons, shown in the header of a part's and a connection's detail (e.g. "Show types"). | done |
| G16 | Choosing a target type from the suggestions fills the ends of a connection that has none: `owner = $self`, typed by the source part's item type (`itemTypes`, else its data types, not counting the std and composition namespaces) if there is exactly one, otherwise left to be chosen; then an end per parameter of the target type's first constructor, with an empty expression. Ends the user has already given are never changed. | done |
| G17 | An end's expression field suggests the property paths of the `$self` end's type, one segment at a time (e.g. `content`, then `content.asBytes`), those whose type conforms to the end's type first. Free typing still works; parsing and type checks are as before. | done |
| G18 | Moving a part removes the connections that no longer join neighbouring parts; the chain shows "N connection(s) removed" with Undo, which restores the order and the connections. | done |
| G19 | The chain shows each connection's ends in one small line, e.g. `owner = $self, bytes = bytes`. | done |
| G20 | A group declared `advanced` (`group("Advanced", advanced = true) { ... }`, also inside an alternative of a choice) is a section folded by default, with an expand/collapse arrow. It opens when one of its active parameters is set to something other than its default value, or has an error (`ConfigurationDraft.advancedGroupMustBeOpen`); the user can fold it again. | done |
| G21 | A parameter's default value (agl-datastore-api P2f) is shown as the placeholder of an empty field (not for sensitive parameters), and a Boolean switch shows it while not set. A parameter with a default is not required. The default is not written into the configuration; it is used when the field is left empty. | done |
