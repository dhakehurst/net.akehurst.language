# AGL Datastore GUI (Compose)

A reusable Compose Multiplatform UI for editing datastore compositions, to be embedded in an application's own GUI.
It is UI only: all code is in `commonMain`, and the host application supplies the behaviour (tests, types,
saving and loading) through callbacks. It never creates, opens or reads datastores itself.

## Use

```kotlin
val state = remember {
    CompositionEditorState(
        providers = listOf(FilesystemProvider(), AglTextProvider()),
        secretSourceIds = listOf("keychain", "env"),
        initial = loadedConfiguration,           // optional, loading is done by the application
    )
}
val callbacks = EditorCallbacks(
    testConnection = { partId, provider, cfg -> /* e.g. provide and open the datastore */ TestResult.Success() },
    testAccess = { partId, provider, cfg, access -> /* e.g. read with 'access' */ TestResult.Success() },
    typesFor = { partId, provider, cfg -> registry.provide(provider, partId, cfg).types() },
)
DatastoreCompositionEditor(state, callbacks = callbacks, customisation = EditorCustomisation(
    toolbarActions = { Button(onClick = { save(state.build()) }, enabled = !state.hasErrors) { Text("Save") } }
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
| G8 | The types of a part come from the application's `typesFor` lambda; with them the target type and end type fields offer the types, and target types and expressions are checked. Without them the fields are plain text. | done |
| G9 | Issues are recomputed on every edit and shown next to their field and in an issue list; schema checks are those of the configuration builders. Connections that cannot be used for write (README C13) give a warning. | done |
| G10 | End expressions are plain text fields, parsed with AGL's expression processor. A code-editor field (highlighting, completion) is for later, through a custom parameter renderer. | done (code editor: todo) |
| G11 | `EditorCustomisation`: parameter renderers, provider presentation, strings (labels), toolbar actions; the host's `MaterialTheme` is used. | done |
| G12 | Saving and loading are done by the application, for now. | done |
| G13 | The list of providers is passed in by the application, e.g. `registry.providers` from a `DatastoreProviderRegistry`. | done |
