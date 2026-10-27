# AGL Datastore API

A datastore gives typed access to data held somewhere else: files, parsed text, spreadsheets,
requirements management (RM) tools, PLM tools, XML files and so on.
Datastores can be composed, so that the output of one is the input of the next
(e.g. files → parsed text, or RM requirements → parsed requirement text).

This module holds only the API. Default implementations and DSL builders are in `agl-datastore-common`.
`agl-datastore-filesystem` and `agl-datastore-text` are example datastores.
Other datastores are expected to be implemented in other modules or projects.

## Requirements

Status: **done** = implemented on `2.4.0-inwork`; **todo** = agreed, not yet implemented.

### Providers and datastores

| Id | Requirement | Status |
|----|-------------|--------|
| P1 | New kinds of datastore can be added in other modules, by implementing `DatastoreProvider` and `Datastore` and registering the provider in a `DatastoreProviderRegistry`. | done |
| P2 | A provider describes its configuration parameters with a `DatastoreProviderSchema` (`DatastoreProvider.schema`): one parameter set for creating a datastore, one for each access (read/write). | done |
| P2a | A configuration is checked against the provider's schema when it is built: unknown parameters, missing required parameters, values of the wrong type, and choices with no satisfied alternative are errors. Datastore configurations are checked against the datastore parameters, access configurations against the access parameters. | done |
| P2c | A parameter can be marked `sensitive` (e.g. a password or API token): `parameter(...) { sensitive() }`. `DatastoreConfiguration.isSensitive(id)` reports it, and `toString()` masks sensitive values. Values are still read with the normal getters. | done |
| P2b | `DatastoreConfiguration` has typed getters (`getAsString`, `getAsInt`, `getAsLong`, `getAsDouble`, `getAsBoolean`, each with an `OrNull` variant). | done |
| P3 | `Datastore.types()` returns the types of the data the datastore reads and writes. It takes no configuration. | done |
| P3a | A datastore's types are fixed for that datastore. They may depend on its datastore configuration (e.g. the grammar of a text datastore, or the schema of an XML file), but never on an access configuration. | done |
| P3b | An access configuration selects which objects a read or write deals with (e.g. a glob of file paths, or a query). It filters the objects retrieved, it does not change their types. | done (the filesystem datastore applies `access.glob` to paths relative to its root) |
| P4 | `Datastore.accessorMutator()` returns the accessor/mutator for that data. It takes no configuration. | done |
| P5 | `types()` and `accessorMutator()` return the same instances on every call, and every object a datastore emits is typed against those types. Implementations extend `DatastoreAbstract` (agl-datastore-common), which creates both once, from `createTypes()` and an overridable `createAccessorMutator(types)`. | done |
| P8 | A datastore has a lifecycle: `open()` prepares it (e.g. connects and logs in to a remote tool) and `close()` releases it. `read`/`write` fail unless it is open, also when a returned flow is collected after close. `open()` on an open datastore fails, `close()` on a closed one does nothing, and it can be opened again. `use { }` opens, runs the block, and closes even if the block throws. `DatastoreAbstract` keeps the state; implementations override `onOpen()`/`onClose()` (no-ops by default) and implement `doRead()`/`doWrite()`. | done |
| P9 | A datastore has transactions (`Transactional`): `beginTransaction()`, `prepare()`, `commit()`, `rollback()` and `transactionState` (NONE, ACTIVE, PREPARED). `write` must be called and collected inside a transaction; it stages changes, and `commit()` applies them. `prepare()` checks that commit will succeed and changes nothing; `commit()` prepares first if needed. `close()` rolls back an active transaction. `transaction { }` begins, runs the block, prepares and commits, and rolls back if anything fails. `DatastoreAbstract` keeps the state; implementations override `onBeginTransaction()`, `onPrepare()`, `onCommit()`, `onRollback()` (no-ops by default). | done |
| P7 | A datastore's types are never changed by a composition that uses it. | done |
| P6 | The namespaces of a datastore's types are created by its provider. They are not prefixed by the datastore identity: two datastores from the same provider in one composition share the same types. | done |

### Data items

| Id | Requirement | Status |
|----|-------------|--------|
| D1 | `read` and `write` take and return a `Flow<DataItem>`. | done |
| D2 | A `DataItem` has `parent: DataItem?` (the item it was derived from), `identity: DataItemIdentity`, `data: TypedObject` and `provenance: DataItemProvenance`. | done |
| D3 | Each datastore implementation defines its own provenance, by implementing `DataItemProvenance`. | done |
| D4 | A `DataItem` has `issues: IssueCollection<DataItemIssueLocation>`, using the generic issues of `net.akehurst.kotlinx.issues`, not AGL's `LanguageIssue`. Each datastore defines its own `DataItemIssueLocation` (e.g. `AglTextIssueLocation` wraps an `InputLocation`). A failure to read or write one item (e.g. a parse error) gives an item with the issues, and the rest of the flow carries on. | done (text datastore) |
| D6 | A derived item's identity is unique among the items derived from the same parent: a connection item uses the connection identity (e.g. `[/a/test.txt, parsableUnit]`). A datastore that returns the object it was given (e.g. the text datastore returns the same `ParsableUnit`) keeps the input item's parent, identity and data. | done |
| D5 | A `DataItem` has `qualifiedIdentity: List<DataItemIdentity>`, which is the parent's `qualifiedIdentity` followed by its own `identity`. | done |

### Composition

| Id | Requirement | Status |
|----|-------------|--------|
| C1 | A composition is a chain of datastores (parts). Each part reads the output of the part before it. | done |
| C2 | A connection joins the output of one part to the input of the next. For each source item it creates an instance of the connection's `targetType`, which must be a type the target datastore reads. | done |
| C3 | A connection has ends: `end(name, typeName, expression)`. `expression` is evaluated with `$self` = the source item's data, and `typeName` is the type of its result. | done |
| C4 | One part can feed several connections into the next part (fan-out), e.g. a requirement's title and its description each parsed. Joining several sources is not required. | done |
| C5 | The composed types are the merge of the parts' types, plus a namespace `composition`. | done |
| C6 | For each connection end whose type is a `DataType`, the composition generates an association in the `composition` namespace, e.g. `ParsableUnit.owner : FileHandle` and `FileHandle.parsableUnit : ParsableUnit?` (named by the connection identity). The association owns both end properties; they are not added to the end types, and the objects are not changed. Ends with a primitive type (e.g. `String`) are constructor arguments of the target type. | done |
| C8 | A connection has an optional `name`. Its `identity` is the name, or else the target type's simple name with a lower-case first letter (e.g. `parsableUnit`). Connections from the same source to the same target must have different identities. The identity is used for the created items and for the derived association end on the source type. | done |
| C9 | A composition's `open()` opens its parts in chain order; if one fails, the parts already opened are closed again. `close()` closes them in reverse order, closing every part even if one fails. `read` fails unless the composition is open, and it has `use { }` too. | done |
| C10 | A composition's transaction commits all or nothing (two-phase commit): it begins a transaction on every part, prepares every part (if one fails, every part is rolled back), and commits the parts only once all are prepared. If a part still fails to commit, it and the parts after it are rolled back; if parts before it had committed, a `PartialCommitException` names which committed and which did not. | done |
| C11 | Each composition `read` has its own accessor-mutator, which holds the links that read creates. Every item it emits, and each item's parents, is typed by that accessor-mutator, so navigation from an item's data (e.g. `item.data.getProperty("parsableUnit")`) follows that read's links. Two reads never share links, and links are freed with their items. `read` still returns `Flow<DataItem>`. `composition.accessorMutator()` is for the composed types (e.g. building expressions) and holds no links. | done |
| C7 | The target object of a connection is constructed inside read/write, which links it to the end objects. Both association ends then navigate through that link. | done |
| C12 | Each part's write mirrors its read. A composition `write(access, items)` must be called and collected inside the composition's transaction; `commit()` applies the changes. It writes the parts in reverse chain order. Between two parts, each item written by the later part is mapped back through the connections: the source object is the one linked by the end of a `DataType` whose expression is `$self` (e.g. `owner`), and the value of each other end is assigned to the source along its expression (e.g. `content.asString := text`). The source item (the item's parent, when it holds that object) is then written by the earlier part. | done |
| C13 | The end expressions of a connection used for write must be assignable property paths (`a.b.c`, optionally starting with `$self.`), and the connection needs a `$self` end of a `DataType`; otherwise write fails before writing anything. | done |
| C14 | An item to be written must be linked to its source object (a new object is linked by whoever creates it). An item that no connection links to a source is not written: it is emitted after the written items, with an error issue. An item with errors (e.g. it could not be formatted) is not assigned to its source; its issues are passed on with the source item. | done |
| C15 | Composition `write` returns `Flow<DataItem>`, like `Datastore.write`: the items written by the first part, then the items that could not be written. | done (was `Unit`; changed so issues can be returned) |
| C16 | A part's write returns the item data it was given (same `TypedObject`), so the links of the read stay navigable for the next step of the write. | done (text, filesystem) |

### Text datastore

| Id | Requirement | Status |
|----|-------------|--------|
| X1 | Read takes a `ParsableUnit` (or a `String`, for which a unit is created) and outputs the same unit, with `asm` set to the root of parsing its `text` (null if parsing fails, with the issues on the item). One parse gives one root. | done |
| X2 | Write takes a `ParsableUnit` and sets its `text` to the result of formatting its `asm`. If there is no `asm`, or formatting fails, `text` is unchanged and the item has the issues. | done |

### Filesystem datastore

| Id | Requirement | Status |
|----|-------------|--------|
| F1 | Setting `FileHandle.content.asString` stages the new content in the datastore's active transaction (it fails outside a transaction). Reading it gives the staged content, if any. `commit()` writes the staged contents to the files, `rollback()` discards them. | done |
| F2 | Write takes `FileHandle` items (whose content has been set) and returns them; any other item gets an error issue. | done |
| F3 | A new file is created through the `FileHandle` constructor, `FileHandle(path)`, with a path relative to the filesystem root (no leading `/`, no `.` or `..` segments), e.g. from an m2m via the accessor-mutator's `createStructureValue`. It returns a pending handle; nothing is on disk until commit. Its content is set like any file's, and on commit the file and any missing directories are created (an existing file at that path is overwritten). Rollback leaves nothing. `prepare()` fails if a directory is in the way. The constructor does not add a `path` property to the types. | done |

### Types (agl-processor)

| Id | Requirement | Status |
|----|-------------|--------|
| T1 | An association end has a flag `ownedByAssociation`. When false (the default, existing behaviour) the end's property is added to the type at the other end. When true the property is owned by the association and the type is not changed. | done |
| T2 | Association-owned properties are found through the `TypesDomain` that contains the association, so they are navigable with an accessor-mutator for that domain. | done |
| T3 | `ExternalGetterByReflection.typeFor` matches a Kotlin object to a type by its declared `implementation` class first, and only then by simple class name, because several datastores may define types with the same simple name. | done |
| T4 | The value of an association-owned end is a link, not held by the objects: the accessor-mutator gets and sets it through `ExternalGetter.getLinkEnd`/`setLinkEnd`. The default getters keep the links in memory (`AssociationLinkStore`), keyed by the end objects using `equals`/`hashCode`, so every object must have a unique identity by that contract. Links are navigable in both directions (so expressions and m2m transformations over a read result can follow them), and keep the multiplicity of both ends. Links belong to the accessor-mutator that set them (see C11). |  done |
| T6 | `TypedObject.setPropertySuspend` uses the accessor-mutator's suspend setter, which calls a property's `mutatorSuspend` (or its `mutator` if it has none). | done |
| T5 | Navigation from an object, by property or link, is by a unique name: an association-owned end may not have the same name as another property or association-owned end of the same type (a subtype may redefine a name). | done |

### Example uses

- Filesystem → text: parse the content of each file.

  ```kotlin
  connect("fs", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
      end("owner", "filesystem.FileHandle", "\$self")
      end("text", "String", "content.asString")
  }
  ```

- RM tool → text: parse the text of each requirement (the RM datastore is not part of this project).

  ```kotlin
  connect("rm", "txt", AglTextProvider.PARSABLE_UNIT_TYPE) {
      end("owner", "rm.Requirement", "\$self")
      end("text", "String", "description")
  }
  ```
