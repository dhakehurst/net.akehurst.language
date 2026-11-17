package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.DatastoreProvenance
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.asm.StdLibDefault
import java.sql.DriverManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_JdbcDatastore_write {

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(JdbcProvider())
    }

    private val noAccess: DatastoreConfiguration get() = accessConfiguration(JdbcProvider.IDENTITY) {}

    private suspend fun open(url: String): Datastore {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.CONFIG_URL, url) }
        return JdbcProvider().provide(DatastoreIdentity("db"), cfg).also { it.open() }
    }

    private suspend fun read(sut: Datastore, vararg tables: Any): List<DataItem> =
        sut.read(accessConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.ACCESS_TABLES, tables.toList()) }, emptyFlow()).toList()

    private fun names(url: String): List<String> = DriverManager.getConnection(url).use { c ->
        c.createStatement().use { st ->
            st.executeQuery("SELECT NAME FROM EMPLOYEE ORDER BY ID").use { rs -> generateSequence { if (rs.next()) rs.getString(1) else null }.toList() }
        }
    }

    @Test
    fun update_existing_row() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        sut.accessorMutator().setPropertySuspend(bob.data, "NAME", sut.accessorMutator().toTypedObject("Robert", StdLibDefault.String))
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(bob)).toList()
        assertTrue(out.single().issues.isEmpty(), out.single().issues.toString())
        // not visible to another connection before commit
        assertEquals(listOf("Alice", "Bob", "Carol"), names(url))
        sut.commit()
        assertEquals(listOf("Alice", "Robert", "Carol"), names(url))
        sut.close()
    }

    @Test
    fun insert_new_row_made_with_the_constructor() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val am = sut.accessorMutator()
        val args = mapOf(
            "ID" to am.toTypedObject(13L, StdLibDefault.Integer),
            "NAME" to am.toTypedObject("Dave", StdLibDefault.String),
            "DEPT_ID" to am.toTypedObject(2L, StdLibDefault.Integer),
            "MANAGER_ID" to am.nothing(),
            "HIRED" to am.nothing(),
            "ACTIVE" to am.toTypedObject(true, StdLibDefault.Boolean),
            "PHOTO" to am.nothing(),
        )
        val dave = am.createStructureValueSuspend("PUBLIC.EMPLOYEE".asPossiblyQualifiedName, args)
        val item = DataItemDefault(null, DataItemIdentity("new"), dave, DatastoreProvenance(DatastoreIdentity("test")))
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(item)).toList()
        assertTrue(out.single().issues.isEmpty(), out.single().issues.toString())
        sut.commit()
        assertEquals(listOf("Alice", "Bob", "Carol", "Dave"), names(url))
        sut.close()
    }

    private fun mode(m: String) = accessConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.ACCESS_WRITE_MODE, m) }

    @Test
    fun write_mode_insert_refuses_an_existing_key() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        (bob.data.self as JdbcRow)["NAME"] = "Robert"
        sut.beginTransaction()
        val out = sut.write(mode(JdbcProvider.WRITE_MODE_INSERT), flowOf(bob)).toList()
        assertEquals(1, out.single().issues.errors.size)
        sut.commit()
        assertEquals(listOf("Alice", "Bob", "Carol"), names(url))
        sut.close()
    }

    @Test
    fun write_mode_update_refuses_a_missing_key() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        val row = bob.data.self as JdbcRow
        val copy = JdbcRow(row.table, row.values + mapOf("ID" to 99L, "NAME" to "Ghost"))
        val item = DataItemDefault(null, DataItemIdentity("ghost"), sut.accessorMutator().toTypedObject(copy, StdLibDefault.AnyType), DatastoreProvenance(DatastoreIdentity("test")))
        sut.beginTransaction()
        val out = sut.write(mode(JdbcProvider.WRITE_MODE_UPDATE), flowOf(item, bob)).toList()
        assertEquals(1, out[0].issues.errors.size)
        assertTrue(out[1].issues.isEmpty(), out[1].issues.toString())
        sut.commit()
        assertEquals(listOf("Alice", "Bob", "Carol"), names(url))
        sut.close()
    }

    @Test
    fun rollback_undoes_the_writes() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        (bob.data.self as JdbcRow)["NAME"] = "Robert"
        sut.beginTransaction()
        sut.write(noAccess, flowOf(bob)).toList()
        sut.rollback()
        assertEquals(listOf("Alice", "Bob", "Carol"), names(url))
        sut.close()
    }

    @Test
    fun database_error_is_an_item_issue() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        (bob.data.self as JdbcRow)["DEPT_ID"] = 99L // no such department
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(bob)).toList()
        assertEquals(1, out.single().issues.errors.size)
        sut.rollback()
        sut.close()
    }

    @Test
    fun view_and_non_row_items_are_not_written() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val view = read(sut, "ACTIVE_EMPLOYEE").first()
        val other = DataItemDefault(null, DataItemIdentity("x"), sut.accessorMutator().toTypedObject("text", StdLibDefault.String), DatastoreProvenance(DatastoreIdentity("test")))
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(view, other)).toList()
        assertTrue(out.all { it.issues.errors.size == 1 }, out.map { it.issues }.toString())
        sut.rollback()
        sut.close()
    }
}
