# agl-datastore-generic-jdbc

A datastore over the tables of one schema of a relational database (Oracle, PostgreSQL, SQL Server, MySQL, H2, ...),
reached through JDBC. Package `net.akehurst.datastore.generic.jdbc`, `JdbcProvider` / `JdbcDatastore`. JVM only (JDBC is a Java API).
No driver is a dependency: the application adds the driver for its database, e.g. `com.oracle.database.jdbc:ojdbc11`.

## Use

```kotlin
val config = datastoreConfiguration(JdbcProvider.IDENTITY) {
    argument(JdbcProvider.CONFIG_URL, "jdbc:oracle:thin:@//host:1521/SERVICE")
    argument(JdbcProvider.CONFIG_USER, "hr")
    argument(JdbcProvider.CONFIG_PASSWORD, secret("keychain", "hr-db"))
    argument(JdbcProvider.CONFIG_SCHEMA, "HR")
    argument(JdbcProvider.CONFIG_TABLES, listOf("EMP*", "DEPARTMENT"))
}
val access = accessConfiguration(JdbcProvider.IDENTITY) {
    argument(JdbcProvider.ACCESS_TABLES, listOf("EMPLOYEE WHERE ACTIVE = 1", "DEPARTMENT"))
}
```

## Requirements

| id | requirement | state |
|----|-------------|-------|
| J1 | JDBC only (no ODBC), JVM only. Module `agl-datastore-generic-jdbc`. | done |
| J2 | Datastore config: `config.url` (required), `config.user`, `config.password` (sensitive, give a `secret(source,key)` ref), `config.schema` (default: the connection's current schema), `config.tables` (List of names or globs, matched ignoring case; default all). Advanced: `config.include-views` (true), `config.driver-properties` (List of `key=value`), `config.fetch-size` (500). | done |
| J3 | Types are computed from the database metadata, the first time they are asked for (connecting just for that if the datastore is not open). Namespace = schema name (else catalog name, else `jdbc`); one data type per selected table and view, named as the table; one property per column, named as the column, nullable if the column is. | done |
| J4 | Column types: CHAR/VARCHAR/CLOB/... → `String`; TINYINT..BIGINT → `Integer`; NUMERIC/DECIMAL with scale 0 and at most 18 digits → `Integer`, other NUMERIC/DECIMAL, REAL, FLOAT, DOUBLE → `Real` (a Double, so very precise decimals lose digits); BIT/BOOLEAN → `Boolean`; DATE, TIMESTAMP (with or without time zone) → `Timestamp` (without a zone taken as UTC; a DATE is its midnight UTC); BINARY/VARBINARY/BLOB → `Binary`; anything else (TIME, ROWID, XML, ...) → `String`. | done |
| J5 | Each foreign key (to a selected table) is an association; the column stays a property too. On the referencing type: the referenced row (nullable), named after the referenced table, or after the key's constraint name if there is more than one key to that table, it is a self reference, or the name is taken by a column. On the referenced type: the List of referencing rows, named after the referencing table, else `<TABLE>_<KEY NAME>`. A name still taken gets `_2`, `_3`, .... | done |
| J6 | A join table (exactly two foreign keys to selected tables, whose columns are all its columns and together its primary key) has no type: it is a many-to-many association, a List on each side, named after the other table (else the join table's name; for a self join, the key names). | done |
| J7 | Association ends are navigated by querying the database, so only while the datastore is open (also while a read is being collected). | done |
| J8 | Access: `access.tables`, a List of (table, filter) entries, read in order. An entry is a `Pair(table, filter?)`, or a line `TABLE` or `TABLE WHERE <condition>`. The filter is SQL, put in the query as given. A table is matched exactly, else ignoring case if unique; an unknown or join table fails the read. | done |
| J9 | Read gives one item per row, typed as its table's type (all rows are `JdbcRow`; the datastore types them, `DatastoreObjectTyping`). Identity `<TABLE>:<primary key values, ','-separated>`, or `<TABLE>#<index>` for a table without a primary key (e.g. a view). Provenance `JdbcProvenance(datastoreIdentity, schema, table, key)`. Rows are fetched `config.fetch-size` at a time. The read input is ignored. | done |
| J10 | Write: not yet; each item gets an error issue. Delete: not supported. | open |
