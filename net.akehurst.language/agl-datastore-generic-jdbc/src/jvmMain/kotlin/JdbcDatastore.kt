package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.datastore.common.withSecretsResolved
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistry
import net.akehurst.kotlinx.secure.storage.SecretSourceRegistryDefault
import net.akehurst.kotlinx.text.toRegexFromGlob
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.api.TypesDomain
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
    val values: Map<String, Any?>,
) {
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
                        t.columns.forEach { col ->
                            propertyOfWithBinding<JdbcRow, Any?>(setOf(REF, VAL), col.name, col.stdType, isNullable = col.isNullable, accessor = { it.values[col.name] })
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

    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        TODO("read: waiting for the read semantics")
    }

    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        TODO("write: not yet specified")
    }

    override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = when (source) {
        is JdbcRow -> JdbcProvenance(identity, _schema?.schema, source.table.name, source.table.primaryKey.associateWith { source.values[it] })
        else -> input?.provenance ?: super.provenanceFor(input, source)
    }
}
