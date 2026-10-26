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
| P2 | A provider describes its configuration parameters with a `DatastoreProviderSchema`: one parameter set for creating a datastore, one for each access (read/write). | done |
| P3 | `Datastore.types()` returns the types of the data the datastore reads and writes. It takes no configuration. | done |
| P4 | `Datastore.accessorMutator()` returns the accessor/mutator for that data. It takes no configuration. | done |
| P5 | `types()` and `accessorMutator()` return the same instances on every call, and every object a datastore emits is typed against those types. | done (documented here, not enforced) |
| P7 | A datastore's types are never changed by a composition that uses it. | todo (see C6) |
| P6 | The namespaces of a datastore's types are created by its provider. They are not prefixed by the datastore identity: two datastores from the same provider in one composition share the same types. | done |

### Data items

| Id | Requirement | Status |
|----|-------------|--------|
| D1 | `read` and `write` take and return a `Flow<DataItem>`. | done |
| D2 | A `DataItem` has `parent: DataItem?` (the item it was derived from), `identity: DataItemIdentity`, `data: TypedObject` and `provenance: DataItemProvenance`. | done |
| D3 | Each datastore implementation defines its own provenance, by implementing `DataItemProvenance`. | done |

### Composition

| Id | Requirement | Status |
|----|-------------|--------|
| C1 | A composition is a chain of datastores (parts). Each part reads the output of the part before it. | done |
| C2 | A connection joins the output of one part to the input of the next. For each source item it creates an instance of the connection's `targetType`, which must be a type the target datastore reads. | done |
| C3 | A connection has ends: `end(name, typeName, expression)`. `expression` is evaluated with `$self` = the source item's data, and `typeName` is the type of its result. | done |
| C4 | One part can feed several connections into the next part (fan-out), e.g. a requirement's title and its description each parsed. Joining several sources is not required. | todo |
| C5 | The composed types are the merge of the parts' types, plus a namespace `composition`. | done |
| C6 | For each connection end whose type is a `DataType`, the composition generates an association in the `composition` namespace, e.g. `ParsableUnit.owner : FileHandle` and the derived `FileHandle.parsableUnit : ParsableUnit?`. The association owns these end properties; they are not added to the end types. Ends with a primitive type (e.g. `String`) are constructor arguments of the target type. | partly: generated, but the properties are currently added to the end types |
| C7 | The target object of a connection is constructed inside read/write. The derived association end returns the object that read/write constructed; it does not construct one. | done |

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
