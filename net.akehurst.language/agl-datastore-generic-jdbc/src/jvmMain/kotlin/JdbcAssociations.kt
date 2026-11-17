package net.akehurst.datastore.generic.jdbc

/**
 * A foreign key of 'table' as an association:
 * 'forwardName' is a property of the table's type, giving the referenced row (or null);
 * 'reverseName' is a property of the referenced table's type, giving the List of rows that refer to it.
 */
data class JdbcForeignKeyAssociation(
    val table: JdbcTable,
    val foreignKey: JdbcForeignKey,
    val forwardName: String,
    val reverseName: String,
)

/**
 * A join table (exactly two foreign keys, whose columns are all of its columns and together its primary key)
 * as a many-to-many association between the two referenced tables. The join table has no type of its own.
 * 'nameOnA' is a property of A's type giving the List of B rows, 'nameOnB' a property of B's type giving the List of A rows.
 */
data class JdbcManyToManyAssociation(
    val joinTable: JdbcTable,
    /** the foreign key to A */
    val foreignKeyA: JdbcForeignKey,
    /** the foreign key to B */
    val foreignKeyB: JdbcForeignKey,
    val nameOnA: String,
    val nameOnB: String,
)

/**
 * The associations of a schema, with the names of their properties.
 * Property names never clash on a type: columns keep their names, and each association end gets
 * its preferred name if that is free on the type, else a longer one (see the README, J5 and J6).
 */
class JdbcAssociations(val schema: JdbcSchema) {

    val joinTables: List<JdbcTable>
    val manyToMany: List<JdbcManyToManyAssociation>
    val foreignKeys: List<JdbcForeignKeyAssociation>

    /** the tables that have a type: all but the join tables */
    val typedTables: List<JdbcTable>

    init {
        joinTables = schema.tables.filter { isJoinTable(it) }
        typedTables = schema.tables - joinTables.toSet()
        // property names used so far, per table
        val used = typedTables.associate { t -> Pair(t.name, t.columns.map { it.name }.toMutableSet()) }

        fun claim(table: String, vararg candidates: String): String {
            val u = used.getValue(table)
            val name = candidates.firstOrNull { it !in u } ?: run {
                val base = candidates.last()
                generateSequence(2) { it + 1 }.map { "${base}_$it" }.first { it !in u }
            }
            u.add(name)
            return name
        }

        // a foreign key to a join table gives no association
        fun fksOf(t: JdbcTable) = t.foreignKeys.filter { it.referencedTable in used }
        foreignKeys = typedTables.flatMap { t ->
            val byRef = fksOf(t).groupBy { it.referencedTable }
            fksOf(t).map { fk ->
                // a self reference (e.g. EMPLOYEE.MANAGER_ID) is named by its key
                val one = byRef.getValue(fk.referencedTable).size == 1 && fk.referencedTable != t.name
                val forward = if (one) claim(t.name, fk.referencedTable, fk.name) else claim(t.name, fk.name)
                Pair(t, Pair(fk, forward))
            }
        }.map { (t, p) ->
            val (fk, forward) = p
            val one = fksOf(t).count { it.referencedTable == fk.referencedTable } == 1 && fk.referencedTable != t.name
            val reverse = if (one) claim(fk.referencedTable, t.name, "${t.name}_${fk.name}") else claim(fk.referencedTable, "${t.name}_${fk.name}")
            JdbcForeignKeyAssociation(t, fk, forward, reverse)
        }

        manyToMany = joinTables.map { j ->
            val (fkA, fkB) = j.foreignKeys
            val a = fkA.referencedTable
            val b = fkB.referencedTable
            val nameOnA = if (a == b) claim(a, fkB.name) else claim(a, b, j.name)
            val nameOnB = if (a == b) claim(b, fkA.name) else claim(b, a, j.name)
            JdbcManyToManyAssociation(j, fkA, fkB, nameOnA, nameOnB)
        }
    }

    private fun isJoinTable(t: JdbcTable): Boolean {
        if (t.isView || t.foreignKeys.size != 2) return false
        val fkCols = t.foreignKeys.flatMap { it.columns }
        if (fkCols.size != fkCols.toSet().size) return false // the two keys share a column
        val all = t.columns.map { it.name }.toSet()
        return fkCols.toSet() == all && t.primaryKey.toSet() == all
    }
}
