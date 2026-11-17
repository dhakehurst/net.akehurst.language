package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.kotlinx.issues.ram.IssueHolder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreObjectTyping
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.datastore.common.withSecretsResolved
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistry
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistryDefault
import net.akehurst.kotlinx.text.toRegexFromGlob
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** where an issue of a JdbcDatastore item is: a table, and a column of it (or null for the row as a whole) */
data class JdbcIssueLocation(
    val table: String?,
    val column: String?
) : DataItemIssueLocation

/** Provenance of a DataItem read from a JdbcDatastore: a row of a table, identified by its primary key values */
data class JdbcProvenance(
    val datastoreIdentity: DatastoreIdentity,
    val schema: String?,
    val table: String,
    /** primary key column -> value; empty if the table has no primary key */
    val key: Map<String, Any?>,
) : DataItemProvenance

/**
 * A row of a table. All tables use this class; its type is the type of its table (see JdbcDatastore.qualifiedTypeNameOf).
 * 'values' holds the value of each column (by column name) as the std type of the column's property:
 * String, Long (Integer), Double (Real), Boolean, kotlin.time.Instant (Timestamp), ByteArray (Binary), or null.
 */
class JdbcRow(
    val table: JdbcTable,
    values: Map<String, Any?>,
) {
    private val _values = values.toMutableMap()

    /** column name -> value */
    val values: Map<String, Any?> get() = _values

    /** sets the value of a column, e.g. before writing the row */
    operator fun set(column: String, value: Any?) {
        require(table.columns.any { it.name == column }) { "Table '${table.name}' has no column '$column'" }
        _values[column] = value
    }

    /** the primary key values, in key order; empty if the table has no primary key */
    val key: List<Any?> get() = table.primaryKey.map { values[it] }

    override fun equals(other: Any?): Boolean = when {
        other !is JdbcRow -> false
        other.table.name != this.table.name -> false
        table.primaryKey.isEmpty() -> this === other
        else -> keyValues(this) == keyValues(other)
    }

    override fun hashCode(): Int = when {
        table.primaryKey.isEmpty() -> System.identityHashCode(this)
        else -> 31 * table.name.hashCode() + keyValues(this).hashCode()
    }

    override fun toString(): String = "${table.name}(${values.entries.joinToString { "${it.key}=${it.value}" }})"

    private companion object {
        // ByteArray has identity equality, so compare its content
        fun keyValues(row: JdbcRow): List<Any?> = row.key.map { if (it is ByteArray) it.toList() else it }
    }
}

/**
 * A datastore over the tables of one schema of a relational database, reached through JDBC.
 *
 * Its types are computed from the database metadata when they are first asked for (connecting just for that if it is not open):
 * a namespace named after the schema, with a data type per table (and view) and a property per column (typed by JdbcSchema.stdTypeOf),
 * an association for each foreign key, and a many-to-many association for each join table.
 * Association ends are navigated by querying the database, so only while the datastore is open.
 *
 * Every row is a JdbcRow; the datastore gives each row the type of its table (DatastoreObjectTyping).
 */
open class JdbcDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration,
    private val secretSources: SecretSourceRegistry = SecretSourceRegistryDefault.GLOBAL
) : DatastoreAbstract(identity, configuration, secretSources), DatastoreObjectTyping {

    companion object {
        /** the namespace of the types if the database gives neither a schema nor a catalog */
        const val DEFAULT_NAMESPACE = "jdbc"

        private val TABLE_LINE = Regex("(\\S+)(?:\\s+[Ww][Hh][Ee][Rr][Ee]\\s+(.+))?", RegexOption.DOT_MATCHES_ALL)
    }

    private val _connectionMutex = Mutex()
    private var _connection: Connection? = null
    private var _schema: JdbcSchema? = null
    private var _associations: JdbcAssociations? = null

    /** the open connection; only while the datastore is open */
    protected val connection: Connection get() = _connection ?: error("Datastore '${identity.value}' is not open")

    /** the schema read from the database metadata; read when the types are created */
    suspend fun schema(): JdbcSchema {
        types()
        return _schema!!
    }

    /** the associations computed from the schema's foreign keys */
    suspend fun associations(): JdbcAssociations {
        types()
        return _associations!!
    }

    /** the namespace of the types: the schema name, else the catalog name, else 'jdbc' */
    val namespaceName: String get() = _schema?.let { it.schema ?: it.catalog } ?: DEFAULT_NAMESPACE

    override fun qualifiedTypeNameOf(obj: Any): QualifiedName? = when (obj) {
        is JdbcRow -> QualifiedName("$namespaceName.${obj.table.name}")
        else -> null
    }

    // --- connection ---

    /** a new connection, as configured by 'config' (whose secrets are resolved) */
    protected open fun connect(config: DatastoreConfiguration): Connection {
        val props = Properties()
        (config.valueOf(JdbcProvider.CONFIG_DRIVER_PROPERTIES) as List<*>?)
            ?.map { it.toString().trim() }?.filter { it.isNotEmpty() }
            ?.forEach { line ->
                val i = line.indexOf('=')
                require(i > 0) { "${JdbcProvider.CONFIG_DRIVER_PROPERTIES}: '$line' is not 'key=value'" }
                props.setProperty(line.substring(0, i).trim(), line.substring(i + 1).trim())
            }
        config.getAsStringOrNull(JdbcProvider.CONFIG_USER)?.let { props.setProperty("user", it) }
        config.getAsStringOrNull(JdbcProvider.CONFIG_PASSWORD)?.let { props.setProperty("password", it) }
        return DriverManager.getConnection(config.getAsString(JdbcProvider.CONFIG_URL), props)
    }

    override suspend fun onOpen() {
        _connection = withContext(Dispatchers.IO) { connect(configuration) }
    }

    override suspend fun onClose() {
        val c = _connection
        _connection = null
        if (null != c) withContext(Dispatchers.IO) { c.close() }
    }

    /**
     * runs 'block' with a connection, one block at a time: the open connection, or, if the datastore is not open,
     * a connection made for the block (e.g. to read the metadata for types()), closed afterwards.
     */
    protected suspend fun <R> withConnection(block: (Connection) -> R): R = _connectionMutex.withLock {
        withContext(Dispatchers.IO) {
            val open = _connection
            when {
                null != open -> block(open)
                else -> connect(configuration.withSecretsResolved(secretSources)).use(block)
            }
        }
    }

    // --- types ---

    override suspend fun createTypes(): TypesDomain {
        val tableGlobs = (configuration.valueOf(JdbcProvider.CONFIG_TABLES) as List<*>?)
            ?.map { it.toString().trim() }?.filter { it.isNotEmpty() }
            ?.map { Regex(it.toRegexFromGlob('/').pattern, RegexOption.IGNORE_CASE) }
            ?: emptyList()
        val includeViews = configuration.getAsBooleanOrNull(JdbcProvider.CONFIG_INCLUDE_VIEWS) ?: true
        val schemaName = configuration.getAsStringOrNull(JdbcProvider.CONFIG_SCHEMA)
        val schema = withConnection { JdbcSchema.read(it, schemaName, tableGlobs, includeViews) }
        val assocs = JdbcAssociations(schema)
        _schema = schema
        _associations = assocs
        return typesDomain("Jdbc", true) {
            namespace(namespaceName) {
                assocs.typedTables.forEach { t ->
                    data(t.name, JdbcRow::class) {
                        // a new row: every column is a parameter, in column order; each parameter is also the column's property
                        constructor_ {
                            t.columns.forEach { col ->
                                parameter<JdbcRow, Any?>(
                                    setOf(REF, VAR), col.name, col.stdType, col.isNullable,
                                    accessor = { it.values[col.name] },
                                    mutator = { row, v -> row[col.name] = v }
                                )
                            }
                            execution { args -> JdbcRow(t, t.columns.mapIndexed { i, col -> Pair(col.name, args.getOrNull(i)) }.toMap()) }
                        }
                    }
                }
                assocs.foreignKeys.forEach { a ->
                    association {
                        // a property of the table's type: the referenced row, or null if a key column is null
                        endSuspend(a.foreignKey.referencedTable, setOf(REF, VAL), a.forwardName, isNullable = true, byEvaluation = { _ ->
                            { obj -> referencedRow(obj as JdbcRow, a.foreignKey) }
                        })
                        // a property of the referenced table's type: the rows that refer to it
                        endSuspend(a.table.name, setOf(REF, VAL), a.reverseName, collectionTypeName = "List", byEvaluation = { _ ->
                            { obj -> referencingRows(obj as JdbcRow, a.table, a.foreignKey) }
                        })
                    }
                }
                assocs.manyToMany.forEach { m ->
                    association {
                        // a property of A's type: the B rows
                        endSuspend(m.foreignKeyB.referencedTable, setOf(REF, VAL), m.nameOnA, collectionTypeName = "List", byEvaluation = { _ ->
                            { obj -> joinedRows(obj as JdbcRow, m.joinTable, m.foreignKeyA, m.foreignKeyB) }
                        })
                        // a property of B's type: the A rows
                        endSuspend(m.foreignKeyA.referencedTable, setOf(REF, VAL), m.nameOnB, collectionTypeName = "List", byEvaluation = { _ ->
                            { obj -> joinedRows(obj as JdbcRow, m.joinTable, m.foreignKeyB, m.foreignKeyA) }
                        })
                    }
                }
            }
        }
    }

    // --- navigation of associations ---

    /** the row of fk.referencedTable that 'row' refers to by 'fk'; null if a key column of 'row' is null */
    protected suspend fun referencedRow(row: JdbcRow, fk: JdbcForeignKey): JdbcRow? {
        checkOpen()
        val values = fk.columns.map { row.values[it] }
        if (values.any { null == it }) return null
        val target = schema().table(fk.referencedTable)
        return selectRows(target, fk.referencedColumns, values).firstOrNull()
    }

    /** the rows of 'table' that refer to 'row' by 'fk' */
    protected suspend fun referencingRows(row: JdbcRow, table: JdbcTable, fk: JdbcForeignKey): List<JdbcRow> {
        checkOpen()
        val values = fk.referencedColumns.map { row.values[it] }
        if (values.any { null == it }) return emptyList()
        return selectRows(table, fk.columns, values)
    }

    /** the rows of 'to.referencedTable' joined to 'row' through 'joinTable' ('from' refers to row's table) */
    protected suspend fun joinedRows(row: JdbcRow, joinTable: JdbcTable, from: JdbcForeignKey, to: JdbcForeignKey): List<JdbcRow> {
        checkOpen()
        val values = from.referencedColumns.map { row.values[it] }
        if (values.any { null == it }) return emptyList()
        val schema = schema()
        val target = schema.table(to.referencedTable)
        val on = to.columns.zip(to.referencedColumns).joinToString(" AND ") { (jc, tc) -> "j.${schema.quote(jc)} = t.${schema.quote(tc)}" }
        val where = from.columns.joinToString(" AND ") { "j.${schema.quote(it)} = ?" }
        val sql = "SELECT ${target.columns.joinToString { "t." + schema.quote(it.name) }} FROM ${schema.sqlName(target)} t JOIN ${schema.sqlName(joinTable)} j ON $on WHERE $where"
        return withConnection { c -> c.prepareStatement(sql).use { ps -> bind(ps, values); ps.executeQuery().use { rs -> rowsOf(target, rs) } } }
    }

    /** the rows of 'table' whose 'columns' equal 'values' */
    protected suspend fun selectRows(table: JdbcTable, columns: List<String>, values: List<Any?>): List<JdbcRow> {
        val schema = schema()
        val where = columns.joinToString(" AND ") { "${schema.quote(it)} = ?" }
        val sql = "SELECT ${table.columns.joinToString { schema.quote(it.name) }} FROM ${schema.sqlName(table)} WHERE $where"
        return withConnection { c -> c.prepareStatement(sql).use { ps -> bind(ps, values); ps.executeQuery().use { rs -> rowsOf(table, rs) } } }
    }

    // --- values ---

    /** binds 'values' (std values, as in a JdbcRow) to the parameters of 'ps' */
    protected fun bind(ps: PreparedStatement, values: List<Any?>) {
        values.forEachIndexed { i, v ->
            when (v) {
                null -> ps.setObject(i + 1, null)
                is Instant -> ps.setTimestamp(i + 1, java.sql.Timestamp.from(v.toJavaInstant()))
                else -> ps.setObject(i + 1, v)
            }
        }
    }

    /** all rows of 'rs', whose columns are those of 'table' in order */
    protected fun rowsOf(table: JdbcTable, rs: ResultSet): List<JdbcRow> {
        val rows = mutableListOf<JdbcRow>()
        while (rs.next()) rows.add(rowOf(table, rs))
        return rows
    }

    /** the current row of 'rs', whose columns are those of 'table' in order */
    protected fun rowOf(table: JdbcTable, rs: ResultSet): JdbcRow =
        JdbcRow(table, table.columns.mapIndexed { i, col -> Pair(col.name, valueOf(rs, i + 1, col)) }.toMap())

    /**
     * the value of column 'index' of the current row of 'rs', as the std type of 'column'.
     * Dates and times without a time zone are taken as UTC (a DATE is its midnight UTC).
     */
    protected open fun valueOf(rs: ResultSet, index: Int, column: JdbcColumn): Any? {
        val v: Any? = when (column.stdType) {
            "Integer" -> rs.getLong(index)
            "Real" -> rs.getDouble(index)
            "Boolean" -> rs.getBoolean(index)
            "Binary" -> rs.getBytes(index)
            "Timestamp" -> when (column.sqlType) {
                Types.DATE -> rs.getObject(index, LocalDate::class.java)?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
                Types.TIMESTAMP_WITH_TIMEZONE -> rs.getObject(index, OffsetDateTime::class.java)?.toInstant()
                else -> rs.getObject(index, LocalDateTime::class.java)?.toInstant(ZoneOffset.UTC)
            }?.toKotlinInstant()

            else -> rs.getString(index)
        }
        return if (rs.wasNull()) null else v
    }

    // --- read / write ---

    /** a table to read, and the SQL condition its rows must meet (null: all rows) */
    data class JdbcTableAccess(val table: JdbcTable, val filter: String?)

    /**
     * the tables of access.tables, in order, each with its filter. An entry is a Pair(table name, filter or null),
     * or a String 'TABLE' or 'TABLE WHERE <condition>' (WHERE in any case).
     * A name is matched exactly, else ignoring case if that is unique.
     */
    protected suspend fun accessTables(accessConfiguration: DatastoreConfiguration): List<JdbcTableAccess> {
        val schema = schema()
        val joinTables = associations().joinTables.map { it.name }.toSet()
        val entries = (accessConfiguration.valueOf(JdbcProvider.ACCESS_TABLES) as List<*>?) ?: emptyList<Any?>()
        return entries.mapNotNull { entry ->
            val (name, filter) = when (entry) {
                null -> return@mapNotNull null
                is Pair<*, *> -> Pair(entry.first.toString().trim(), entry.second?.toString()?.trim())
                else -> {
                    val line = entry.toString().trim()
                    if (line.isEmpty()) return@mapNotNull null
                    val m = TABLE_LINE.matchEntire(line)
                        ?: error("${JdbcProvider.ACCESS_TABLES}: '$line' is not 'TABLE' or 'TABLE WHERE <condition>'")
                    Pair(m.groupValues[1], m.groups[2]?.value?.trim())
                }
            }
            val t = schema.tableOrNull(name) ?: run {
                val found = schema.tables.filter { it.name.equals(name, ignoreCase = true) }
                when (found.size) {
                    1 -> found.first()
                    0 -> error("${JdbcProvider.ACCESS_TABLES}: there is no table '$name' among the tables of this datastore")
                    else -> error("${JdbcProvider.ACCESS_TABLES}: more than one table is named '$name' ignoring case (${found.joinToString { it.name }}), give the exact name")
                }
            }
            require(t.name !in joinTables) { "${JdbcProvider.ACCESS_TABLES}: '${t.name}' is a join table, which has no type (it is a many-to-many association)" }
            JdbcTableAccess(t, filter?.takeIf { it.isNotEmpty() })
        }
    }

    /**
     * the identity of a row's item: '<TABLE>:<primary key values, ','-separated>',
     * or '<TABLE>#<index of the row in this read of the table>' if the table has no primary key
     */
    protected open fun identityOf(row: JdbcRow, index: Long): DataItemIdentity = when {
        row.table.primaryKey.isEmpty() -> DataItemIdentity("${row.table.name}#$index")
        else -> DataItemIdentity("${row.table.name}:${row.key.joinToString(",") { keyText(it) }}")
    }

    private fun keyText(v: Any?): String = when (v) {
        null -> ""
        is ByteArray -> v.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        else -> v.toString()
    }

    /**
     * input: ignored (a JdbcDatastore is a source).
     * output: for each table of access.tables, in order, one item per row (that meets the table's filter, if it has one),
     * typed as the table's type (parent null). The filter is SQL, put in the query as given.
     * Rows are fetched 'config.fetch-size' at a time; association ends can be navigated while the read is collected.
     */
    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        val tables = accessTables(accessConfiguration)
        val fetchSize = (configuration.getAsLongOrNull(JdbcProvider.CONFIG_FETCH_SIZE) ?: JdbcProvider.DEFAULT_FETCH_SIZE).toInt().coerceAtLeast(1)
        val schema = schema()
        val am = accessorMutator()
        return flow {
            for ((t, filter) in tables) {
                val where = filter?.let { " WHERE ($it)" } ?: ""
                val sql = "SELECT ${t.columns.joinToString { schema.quote(it.name) }} FROM ${schema.sqlName(t)}$where"
                val ps = withConnection { c -> c.prepareStatement(sql).also { it.fetchSize = fetchSize } }
                try {
                    val rs = withConnection { ps.executeQuery() }
                    var index = 0L
                    var more = true
                    while (more) {
                        // fetch a batch under the connection lock, emit it without the lock (navigation uses the connection)
                        val batch = withConnection {
                            val rows = mutableListOf<JdbcRow>()
                            while (rows.size < fetchSize && rs.next()) rows.add(rowOf(t, rs))
                            if (rows.size < fetchSize) more = false
                            rows
                        }
                        for (row in batch) {
                            emit(DataItemDefault(null, identityOf(row, index), am.toTypedObject(row, StdLibDefault.AnyType), provenanceFor(null, row)))
                            index++
                        }
                    }
                } finally {
                    withContext(NonCancellable) { withConnection { ps.close() } }
                }
            }
        }
    }

    // --- transaction: one database transaction ---

    override suspend fun onBeginTransaction() {
        withConnection { it.autoCommit = false }
    }

    override suspend fun onCommit() {
        withConnection { c ->
            try {
                c.commit()
            } finally {
                c.autoCommit = true
            }
        }
    }

    override suspend fun onRollback() {
        withConnection { c ->
            try {
                c.rollback()
            } finally {
                c.autoCommit = true
            }
        }
    }

    /**
     * input: items whose data is a JdbcRow (read from this datastore, or created with its type's constructor).
     * Each row is stored by its primary key as access.write-mode says (see store), in the open database transaction
     * (applied on commit, undone on rollback).
     * output: each item, with its provenance (a JdbcProvenance) and, if it could not be written, an error issue:
     * the data is not a JdbcRow, its table is a view or has no primary key, or the database refused it (the message is the database's),
     * or the write mode does not allow it (insert: the key exists; update: it does not).
     */
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        val mode = (accessConfiguration.getAsStringOrNull(JdbcProvider.ACCESS_WRITE_MODE) ?: JdbcProvider.WRITE_MODE_UPSERT).trim().lowercase()
        require(mode in setOf(JdbcProvider.WRITE_MODE_UPSERT, JdbcProvider.WRITE_MODE_INSERT, JdbcProvider.WRITE_MODE_UPDATE)) {
            "${JdbcProvider.ACCESS_WRITE_MODE}: '$mode' is not one of upsert, insert, update"
        }
        return input.map { item ->
            val row = item.data.self as? JdbcRow
            val error = when {
                null == row -> "The data of a written item must be a row of a table of this datastore"
                row.table.isView -> "'${row.table.name}' is a view; it cannot be written"
                row.table.primaryKey.isEmpty() -> "'${row.table.name}' has no primary key; it cannot be written"
                else -> try {
                    store(row, mode)
                } catch (e: java.sql.SQLException) {
                    "'${row.table.name}' row ${row.key}: ${e.message ?: e::class.simpleName}"
                }
            }
            DataItemDefault(
                item.parent, item.identity, item.data, if (null == row) item.provenance else provenanceFor(item, row),
                when (error) {
                    null -> item.issues
                    else -> IssueHolder<DataItemIssueLocation>(
                        item.issues.all.toList() + IssueDefault(IssueKind.ERROR, JdbcIssueLocation(row?.table?.name, null), error)
                    )
                }
            )
        }
    }

    /**
     * stores the row as 'mode' says: UPDATE of the row with row's primary key, and INSERT if there is none (upsert);
     * only INSERT, refused if the key exists (insert); only UPDATE, refused if it does not (update).
     * Returns null, or the reason the row was not stored.
     */
    protected open suspend fun store(row: JdbcRow, mode: String): String? {
        val schema = schema()
        val t = row.table
        val nonKey = t.columns.map { it.name }.filter { it !in t.primaryKey }
        val keyWhere = t.primaryKey.joinToString(" AND ") { "${schema.quote(it)} = ?" }
        return withConnection { c ->
            val existsSql = "SELECT 1 FROM ${schema.sqlName(t)} WHERE $keyWhere"
            fun exists(): Boolean = c.prepareStatement(existsSql).use { ps ->
                bind(ps, row.key)
                ps.executeQuery().use { it.next() }
            }
            val updated = when {
                mode == JdbcProvider.WRITE_MODE_INSERT -> if (exists()) return@withConnection "'${t.name}' already has a row with key ${row.key}; it is not written (write mode insert)" else false
                nonKey.isEmpty() -> exists()
                else -> {
                    val sql = "UPDATE ${schema.sqlName(t)} SET ${nonKey.joinToString { "${schema.quote(it)} = ?" }} WHERE $keyWhere"
                    c.prepareStatement(sql).use { ps ->
                        bind(ps, nonKey.map { row.values[it] } + row.key)
                        ps.executeUpdate() > 0
                    }
                }
            }
            when {
                updated -> null
                mode == JdbcProvider.WRITE_MODE_UPDATE -> "'${t.name}' has no row with key ${row.key}; it is not written (write mode update)"
                else -> {
                    val cols = t.columns.map { it.name }
                    val sql = "INSERT INTO ${schema.sqlName(t)} (${cols.joinToString { schema.quote(it) }}) VALUES (${cols.joinToString { "?" }})"
                    c.prepareStatement(sql).use { ps ->
                        bind(ps, cols.map { row.values[it] })
                        ps.executeUpdate()
                    }
                    null
                }
            }
        }
    }

    override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = when (source) {
        is JdbcRow -> JdbcProvenance(identity, _schema?.schema, source.table.name, source.table.primaryKey.associateWith { source.values[it] })
        else -> input?.provenance ?: super.provenanceFor(input, source)
    }
}
