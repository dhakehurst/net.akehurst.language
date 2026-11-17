package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.PropertyDeclarationAbstract
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

    /** many-to-many ends set on this row (end name -> rows), stored in the join table when the row is written */
    internal val pendingLinks = mutableMapOf<String, List<JdbcRow>>()

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
                        // setting it sets the key columns (stored when the row is written); null sets them to null
                        endSuspend(a.foreignKey.referencedTable, setOf(REF, VAR), a.forwardName, isNullable = true, byEvaluation = { pd ->
                            (pd as PropertyDeclarationAbstract).mutator = { obj, value -> setReference(obj as JdbcRow, a.foreignKey, a.forwardName, value) }
                            ; { obj -> referencedRow(obj as JdbcRow, a.foreignKey) }
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
                        // setting it replaces the links of the row (stored in the join table when the row is written)
                        endSuspend(m.foreignKeyB.referencedTable, setOf(REF, VAR), m.nameOnA, collectionTypeName = "List", byEvaluation = { pd ->
                            (pd as PropertyDeclarationAbstract).mutator = { obj, value -> setLinks(obj as JdbcRow, m.nameOnA, m.foreignKeyB.referencedTable, value) }
                            ; { obj -> (obj as JdbcRow).pendingLinks[m.nameOnA] ?: joinedRows(obj, m.joinTable, m.foreignKeyA, m.foreignKeyB) }
                        })
                        // a property of B's type: the A rows
                        endSuspend(m.foreignKeyA.referencedTable, setOf(REF, VAR), m.nameOnB, collectionTypeName = "List", byEvaluation = { pd ->
                            (pd as PropertyDeclarationAbstract).mutator = { obj, value -> setLinks(obj as JdbcRow, m.nameOnB, m.foreignKeyA.referencedTable, value) }
                            ; { obj -> (obj as JdbcRow).pendingLinks[m.nameOnB] ?: joinedRows(obj, m.joinTable, m.foreignKeyB, m.foreignKeyA) }
                        })
                    }
                }
            }
        }
    }

    // --- setting association ends ---

    /** a value given to an association end: a row (possibly a TypedObject of one), or null / nothing */
    private fun rowOrNull(value: Any?): JdbcRow? = when (value) {
        null -> null
        is JdbcRow -> value
        is TypedObject -> rowOrNull(value.self)
        else -> null
    }

    /** sets the key columns of 'row' to those of 'value' (a row of fk.referencedTable), or to null if 'value' is null or nothing */
    protected fun setReference(row: JdbcRow, fk: JdbcForeignKey, endName: String, value: Any?) {
        val target = rowOrNull(value)
        require(null == target || target.table.name == fk.referencedTable) {
            "'${row.table.name}.$endName' must be set to a row of '${fk.referencedTable}', not of '${target?.table?.name}'"
        }
        fk.columns.forEachIndexed { i, c -> row[c] = target?.values?.get(fk.referencedColumns[i]) }
    }

    /** sets the many-to-many end 'endName' of 'row' to the rows of 'value' (a List of rows of 'targetTable'; null or nothing: none) */
    protected fun setLinks(row: JdbcRow, endName: String, targetTable: String, value: Any?) {
        val list = when (value) {
            null -> emptyList()
            is TypedObject -> if (value.isNothing) emptyList() else (value.self as? List<*> ?: listOf(value.self))
            is List<*> -> value
            else -> listOf(value)
        }
        val rows = list.map { e ->
            val r = rowOrNull(e) ?: throw IllegalArgumentException("'${row.table.name}.$endName' must be set to rows of '$targetTable'")
            require(r.table.name == targetTable) { "'${row.table.name}.$endName' must be set to rows of '$targetTable', not of '${r.table.name}'" }
            r
        }
        row.pendingLinks[endName] = rows
    }

    /** stores the many-to-many ends set on 'row' (see setLinks): its rows in each join table are replaced */
    protected suspend fun storeLinks(row: JdbcRow) {
        if (row.pendingLinks.isEmpty()) return
        val schema = schema()
        val ends = associations().manyToMany.flatMap { m ->
            listOf(
                Triple(m.foreignKeyA.referencedTable to m.nameOnA, m, Pair(m.foreignKeyA, m.foreignKeyB)),
                Triple(m.foreignKeyB.referencedTable to m.nameOnB, m, Pair(m.foreignKeyB, m.foreignKeyA)),
            )
        }.associate { Pair(it.first, Pair(it.second, it.third)) }
        for ((endName, targets) in row.pendingLinks) {
            val (m, keys) = ends[row.table.name to endName] ?: continue
            val (from, to) = keys
            val fromValues = from.referencedColumns.map { row.values[it] }
            withConnection { c ->
                val where = from.columns.joinToString(" AND ") { "${schema.quote(it)} = ?" }
                c.prepareStatement("DELETE FROM ${schema.sqlName(m.joinTable)} WHERE $where").use { ps -> bind(ps, fromValues); ps.executeUpdate() }
                val cols = from.columns + to.columns
                val sql = "INSERT INTO ${schema.sqlName(m.joinTable)} (${cols.joinToString { schema.quote(it) }}) VALUES (${cols.joinToString { "?" }})"
                c.prepareStatement(sql).use { ps ->
                    targets.distinct().forEach { t ->
                        bind(ps, fromValues + to.referencedColumns.map { t.values[it] })
                        ps.executeUpdate()
                    }
                }
            }
        }
        row.pendingLinks.clear()
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

    /** a key value as text: '\\' and ',' are escaped with '\\'; a Binary is hex; null is empty */
    private fun keyText(v: Any?): String = when (v) {
        null -> ""
        is ByteArray -> v.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        else -> v.toString().replace("\\", "\\\\").replace(",", "\\,")
    }

    /** the key texts of an identity's key part (see keyText) */
    private fun splitKeyText(text: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\\' && i + 1 < text.length -> { sb.append(text[i + 1]); i++ }
                ch == ',' -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    /** a key value from its text, as the std type of 'column'; null if it cannot be converted */
    private fun keyValueOf(text: String, column: JdbcColumn): Any? = when (column.stdType) {
        "Integer" -> text.toLongOrNull()
        "Real" -> text.toDoubleOrNull()
        "Boolean" -> text.toBooleanStrictOrNull()
        "Timestamp" -> try { Instant.parse(text) } catch (e: IllegalArgumentException) { null }
        "Binary" -> if (text.length % 2 == 0) text.chunked(2).map { it.toIntOrNull(16)?.toByte() ?: return null }.toByteArray() else null
        else -> text
    }

    /** the table and primary key values of a row's item identity (see identityOf); null if it is not one of a table with a primary key */
    protected suspend fun rowKeyOf(identity: DataItemIdentity): Pair<JdbcTable, List<Any?>>? {
        val text = identity.value
        val i = text.indexOf(':')
        if (i <= 0) return null
        val table = schema().tableOrNull(text.substring(0, i)) ?: return null
        if (table.primaryKey.isEmpty() || table.isView) return null
        val parts = splitKeyText(text.substring(i + 1))
        if (parts.size != table.primaryKey.size) return null
        val values = table.primaryKey.zip(parts).map { (col, part) -> keyValueOf(part, table.column(col)) ?: return null }
        return Pair(table, values)
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
                    store(row, mode) ?: run { storeLinks(row); null }
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

    override val supportsDelete: Boolean get() = true

    /**
     * input: identities of row items ('<TABLE>:<primary key values>', as read gives them).
     * Each row is deleted in the open database transaction (applied on commit, undone on rollback).
     * output: the identities of the rows deleted. An identity that is not one of a row of a table of this datastore
     * (with a primary key), or whose row does not exist, is not output. A row the database refuses to delete
     * (e.g. another row refers to it) fails the delete with the database's message.
     * The access configuration is not used.
     */
    override suspend fun doDelete(accessConfiguration: DatastoreConfiguration, input: Flow<DataItemIdentity>): Flow<DataItemIdentity> {
        val schema = schema()
        return input.transform { id ->
            val (table, key) = rowKeyOf(id) ?: return@transform
            val where = table.primaryKey.joinToString(" AND ") { "${schema.quote(it)} = ?" }
            val n = withConnection { c ->
                c.prepareStatement("DELETE FROM ${schema.sqlName(table)} WHERE $where").use { ps -> bind(ps, key); ps.executeUpdate() }
            }
            if (n > 0) emit(id)
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
