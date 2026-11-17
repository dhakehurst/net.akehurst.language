package net.akehurst.datastore.generic.jdbc

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Types

/** a column of a table, as the database metadata gives it */
data class JdbcColumn(
    val name: String,
    /** java.sql.Types */
    val sqlType: Int,
    /** the database's name for the type, e.g. VARCHAR2 */
    val sqlTypeName: String,
    val size: Int,
    val scale: Int,
    val isNullable: Boolean,
    /** the std type of the property for this column: String, Integer, Real, Boolean, Timestamp or Binary */
    val stdType: String,
)

/** a foreign key of a table: 'columns' refer to 'referencedColumns' of 'referencedTable' (same order) */
data class JdbcForeignKey(
    /** the constraint name, or a name made from the referenced table and columns if the database gives none */
    val name: String,
    val columns: List<String>,
    val referencedTable: String,
    val referencedColumns: List<String>,
)

data class JdbcTable(
    val name: String,
    val isView: Boolean,
    val columns: List<JdbcColumn>,
    /** the primary key columns, in key order (or the key columns configured by config.keys); empty if there is none */
    val primaryKey: List<String>,
    val foreignKeys: List<JdbcForeignKey>,
) {
    fun column(name: String): JdbcColumn = columns.first { it.name == name }
}

/** the tables (and views) of one database schema that a JdbcDatastore gives types for */
data class JdbcSchema(
    /** the schema name, or null if the database has no schemas (then 'catalog' is used) */
    val schema: String?,
    val catalog: String?,
    val tables: List<JdbcTable>,
    /** the quote for identifiers in SQL, from the metadata; "" if quoting is not supported */
    val identifierQuote: String,
) {
    private val _byName = tables.associateBy { it.name }
    fun tableOrNull(name: String): JdbcTable? = _byName[name]
    fun table(name: String): JdbcTable = _byName[name] ?: error("No table '$name' in schema '${schema ?: catalog}'")

    fun quote(identifier: String): String = when {
        identifierQuote.isEmpty() -> identifier
        else -> identifierQuote + identifier.replace(identifierQuote, identifierQuote + identifierQuote) + identifierQuote
    }

    /** the qualified, quoted name of a table for SQL */
    fun sqlName(table: JdbcTable): String = when {
        null != schema -> "${quote(schema)}.${quote(table.name)}"
        null != catalog -> "${quote(catalog)}.${quote(table.name)}"
        else -> quote(table.name)
    }

    companion object {

        /**
         * Reads the tables (and, if 'includeViews', the views) of 'schema' (null: the connection's current schema)
         * whose names match one of 'tableNames' (globs, matched ignoring case; empty: all).
         */
        fun read(connection: Connection, schema: String?, tableNames: List<Regex>, includeViews: Boolean, keys: Map<String, List<String>> = emptyMap()): JdbcSchema {
            val md = connection.metaData
            val catalog = connection.catalog
            val sch = schema?.takeIf { it.isNotBlank() } ?: connection.schema
            val types = if (includeViews) arrayOf("TABLE", "VIEW") else arrayOf("TABLE")
            val found = mutableListOf<Pair<String, Boolean>>()
            md.getTables(if (null == sch) catalog else null, sch, "%", types).use { rs ->
                while (rs.next()) {
                    val name = rs.getString("TABLE_NAME")
                    val isView = rs.getString("TABLE_TYPE").equals("VIEW", ignoreCase = true)
                    if (tableNames.isEmpty() || tableNames.any { it.matches(name) }) found.add(Pair(name, isView))
                }
            }
            val tables = withKeys(found.sortedBy { it.first }.map { (name, isView) -> readTable(md, catalog, sch, name, isView) }, keys)
            val selected = tables.map { it.name }.toSet()
            // foreign keys to tables that are not read give no associations
            val result = tables.map { t -> t.copy(foreignKeys = t.foreignKeys.filter { it.referencedTable in selected }) }
            val quote = md.identifierQuoteString?.trim()?.takeIf { it.isNotEmpty() } ?: ""
            return JdbcSchema(sch, catalog, result, quote)
        }

        /**
         * the tables, each without a primary key given the key columns of 'keys' (table name -> column names; names matched exactly,
         * else ignoring case if unique). Fails for a name that is not a selected table, a table that has a primary key, or a column it does not have.
         */
        private fun withKeys(tables: List<JdbcTable>, keys: Map<String, List<String>>): List<JdbcTable> {
            if (keys.isEmpty()) return tables
            fun <T> find(what: String, name: String, items: List<T>, nameOf: (T) -> String): T =
                items.firstOrNull { nameOf(it) == name } ?: items.filter { nameOf(it).equals(name, ignoreCase = true) }.let { found ->
                    when (found.size) {
                        1 -> found.first()
                        0 -> error("config.keys: there is no $what '$name'")
                        else -> error("config.keys: more than one $what is named '$name' ignoring case, give the exact name")
                    }
                }

            val byTable = keys.entries.associate { (tn, cols) ->
                val t = find("selected table or view", tn, tables) { it.name }
                check(t.primaryKey.isEmpty()) { "config.keys: '${t.name}' has a primary key (${t.primaryKey.joinToString()})" }
                Pair(t.name, cols.map { c -> find("column of '${t.name}'", c, t.columns) { it.name }.name })
            }
            return tables.map { t -> byTable[t.name]?.let { t.copy(primaryKey = it) } ?: t }
        }

        private fun readTable(md: DatabaseMetaData, catalog: String?, schema: String?, name: String, isView: Boolean): JdbcTable {
            val cat = if (null == schema) catalog else null
            val columns = mutableListOf<Pair<Int, JdbcColumn>>()
            md.getColumns(cat, schema, escape(md, name), "%").use { rs ->
                while (rs.next()) {
                    if (rs.getString("TABLE_NAME") != name) continue
                    val sqlType = rs.getInt("DATA_TYPE")
                    val size = rs.getInt("COLUMN_SIZE")
                    val scale = rs.getInt("DECIMAL_DIGITS")
                    columns.add(
                        Pair(
                            rs.getInt("ORDINAL_POSITION"),
                            JdbcColumn(
                                name = rs.getString("COLUMN_NAME"),
                                sqlType = sqlType,
                                sqlTypeName = rs.getString("TYPE_NAME") ?: "",
                                size = size,
                                scale = scale,
                                isNullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                                stdType = stdTypeOf(sqlType, size, scale),
                            )
                        )
                    )
                }
            }
            val pk = mutableListOf<Pair<Int, String>>()
            if (isView.not()) {
                md.getPrimaryKeys(cat, schema, name).use { rs ->
                    while (rs.next()) pk.add(Pair(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME")))
                }
            }
            // rows of one foreign key, by key sequence
            data class FkCol(val fkName: String?, val refTable: String, val seq: Int, val column: String, val refColumn: String)

            val fkCols = mutableListOf<FkCol>()
            if (isView.not()) {
                md.getImportedKeys(cat, schema, name).use { rs ->
                    while (rs.next()) {
                        fkCols.add(
                            FkCol(
                                rs.getString("FK_NAME"), rs.getString("PKTABLE_NAME"), rs.getInt("KEY_SEQ"),
                                rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME")
                            )
                        )
                    }
                }
            }
            // a foreign key starts again at KEY_SEQ 1, which separates keys without a name
            val groups = mutableListOf<MutableList<FkCol>>()
            fkCols.forEach { c ->
                val last = groups.lastOrNull()
                if (null == last || c.seq == 1 || last.first().fkName != c.fkName || last.first().refTable != c.refTable) groups.add(mutableListOf(c)) else last.add(c)
            }
            val fks = groups.map { g ->
                val cols = g.sortedBy { it.seq }
                JdbcForeignKey(
                    name = cols.first().fkName ?: "${cols.first().refTable}_${cols.joinToString("_") { it.column }}",
                    columns = cols.map { it.column },
                    referencedTable = cols.first().refTable,
                    referencedColumns = cols.map { it.refColumn },
                )
            }.sortedBy { it.name }
            return JdbcTable(name, isView, columns.sortedBy { it.first }.map { it.second }, pk.sortedBy { it.first }.map { it.second }, fks)
        }

        /** escapes the search pattern characters of a name, for metadata calls that take a pattern */
        private fun escape(md: DatabaseMetaData, name: String): String {
            val esc = md.searchStringEscape ?: return name
            return name.replace(esc, esc + esc).replace("_", esc + "_").replace("%", esc + "%")
        }

        /**
         * The std type for a column of the given java.sql.Types type.
         * NUMERIC/DECIMAL with scale 0 and at most 18 digits are Integer (e.g. Oracle NUMBER(10)), others Real.
         * DATE and TIMESTAMP (with or without time zone) are Timestamp. Anything not listed (e.g. TIME, ROWID, XML, arrays) is String.
         */
        fun stdTypeOf(sqlType: Int, size: Int, scale: Int): String = when (sqlType) {
            Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> "String"
            Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> "Integer"
            Types.NUMERIC, Types.DECIMAL -> if (scale == 0 && size in 1..18) "Integer" else "Real"
            Types.REAL, Types.FLOAT, Types.DOUBLE -> "Real"
            Types.BIT, Types.BOOLEAN -> "Boolean"
            Types.DATE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "Timestamp"
            Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "Binary"
            else -> "String"
        }
    }
}
