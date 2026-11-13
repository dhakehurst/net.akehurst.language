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
        suggestedIdentity = "doors-reqs",        // for a new composition (no 'initial')
    )
}
// the host shows the identity, name and description fields itself, e.g.
// OutlinedTextField(state.identity, { state.identity = it }, readOnly = !state.identityEditable)
// OutlinedTextField(state.name, { state.name = it }); OutlinedTextField(state.description, { state.description = it })
val callbacks = EditorCallbacks(
    testConnection = { partId, provider, cfg -> /* e.g. provide and open the datastore */ TestResult.Success() },
    testAccess = { partId, provider, cfg, access -> /* e.g. read with 'access' */ TestResult.Success() },
    // an exception thrown is shown as 'not available' with its message
    typesFor = { partId, provider, cfg -> TypesResult.Found(registry.provide(provider, partId, cfg).types()) },
    // read the first 'limit' items through the chain up to part 'upTo'
    preview = { composition, access, upTo, limit -> PreviewResult.Items(readUpTo(composition, access, upTo, limit)) },
)
DatastoreCompositionEditor(state, callbacks = callbacks, customisation = EditorCustomisation(
    toolbarActions = { Button(onClick = { save(state.build()) }, enabled = !state.hasErrors) { Text("Save") } },
    partActions = { part -> TextButton(onClick = { showTypes(part) }) { Text("Show types") } },
))

val access = remember(state) { AccessEditorState(state) }
AccessConfigurationEditor(access, callbacks = callbacks)

// for someone who may only look: nothing can be changed, tests, preview and selection still work
DatastoreCompositionEditor(state, callbacks = callbacks, readOnly = true)
AccessConfigurationEditor(access, callbacks = callbacks, readOnly = true)
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
| G16 | Pre-fill of a connection's ends from the types, when its target type is set: `owner = $self`, typed by the source part's item type (`itemTypes`, else its data types, not counting the std and composition namespaces) if there is exactly one, otherwise left to be chosen; then an end per parameter of the target type's first constructor, with an empty expression. Ends made by pre-fill are marked pre-filled; editing an end's name, type or expression clears the mark. When the target type changes (and the target's types are known), the still-marked ends except `owner` are removed and the ends are pre-filled again for the new type: no second `owner` is added (a still-marked owner whose type now differs is replaced), and no end is added with the name of an end that is kept. While the target's types are not known the ends are kept. An end that is not a data type of the source (so a constructor argument, see agl-datastore-api README C6) and whose name is not a parameter of the target type's constructor gives a warning. | done |
| G17 | An end's expression field suggests the property paths of the `$self` end's type, one segment at a time (e.g. `content`, then `content.asBytes`), those whose type conforms to the end's type first. Free typing still works; parsing and type checks are as before. | done |
| G18 | Moving a part removes the connections that no longer join neighbouring parts; the chain shows "N connection(s) removed" with Undo, which restores the order and the connections. | done |
| G19 | The chain shows each connection's ends in one small line, e.g. `owner = $self, bytes = bytes`. | done |
| G20 | A group declared `advanced` (`group("Advanced", advanced = true) { ... }`, also inside an alternative of a choice) is a section folded by default, with an expand/collapse arrow. It opens when one of its active parameters is set to something other than its default value, or has an error (`ConfigurationDraft.advancedGroupMustBeOpen`); the user can fold it again. | done |
| G21 | A parameter's default value (agl-datastore-api P2f) is shown as the placeholder of an empty field (not for sensitive parameters), and a Boolean switch shows it while not set. A parameter with a default is not required. The default is not written into the configuration; it is used when the field is left empty. | done |
| G20 | Preview: each part in the access editor has a 'Preview' button that reads through the chain from the first part up to that part, with the host's `preview` callback (composition and access configurations, `upTo`, `limit` = `EditorCustomisation.previewLimit`, default 10), returning `PreviewResult.Items(items, more)` or `Failure`; an exception becomes `Failure`. Disabled (not hidden) with no callback, or while the composition or access configuration has errors. Each item shows its qualified identity (joined with '/'), its issues, and a property tree that is read one level at a time when opened (text for primitives and null, a count for collections, the message for a property that cannot be read). A result is dropped when a configuration changes. | done |
| G22 | `CompositionEditorState` holds the composition's `identity`, `name` and `description` (agl-datastore-api C18) as state, loaded from `initial` (or `load`) and returned by `build()`. `identityEditable` is true only for a new composition (no `initial`, nothing loaded); then the identity starts as the constructor's `suggestedIdentity` (default ""), e.g. suggested by the host. The identity of a loaded composition is read-only (setting it fails). A blank or invalid identity (`DatastoreCompositionIdentity.isValid`) is an error issue at `IssueLocation.CompositionIdentity`, so `hasErrors` is true and `build()` (and the access editor's `build()`) gives null. Uniqueness among compositions is checked by the host. The editor does not show these fields: the host application draws them. | done |
| G23 | `readOnly` (default false) on `DatastoreCompositionEditor` and `AccessConfigurationEditor`, for people who may look but not change: the buttons that change the state are hidden (add, remove and move part, add and remove connection and end), and fields, secret pickers, choices and switches cannot be changed. Text fields keep their normal text colour (Material `readOnly`, not disabled) and can still be selected. Selecting parts and connections, folding groups, scrolling, 'Test connection', 'Test access', preview, Retry of types and the host's `partActions` / `connectionActions` still work. A custom parameter renderer is told through `ParameterFieldContext.readOnly`. `ParameterForm` has `readOnly` too. | done |
| G24 | A parameter of the std `List` type (`ParameterKind.LIST`) is a multi-line text field, one element per line. A `List` value is loaded as its elements joined by newlines; the text is saved as a `List<String>`, one element per non-blank line, trimmed. An empty text is not set. A `List` default is shown as the placeholder, one element per line. `readOnly` and `enabled` work as for the other kinds. | done |
