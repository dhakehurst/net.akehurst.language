package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.flow.asFlow
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
import kotlin.test.assertFailsWith
import net.akehurst.language.objectgraph.api.TypedObject
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

    private fun query(url: String, sql: String): List<String> = DriverManager.getConnection(url).use { c ->
        c.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> generateSequence { if (rs.next()) rs.getString(1) else null }.toList() }
        }
    }

    @Test
    fun delete_rows_by_identity() = runTest {
        val url = TestDb.create(*TestDb.HR, "INSERT INTO EMPLOYEE VALUES (13, 'Dave', 2, NULL, NULL, TRUE, NULL)")
        val sut = open(url)
        assertTrue(sut.supportsDelete)
        sut.beginTransaction()
        val ids = listOf("EMPLOYEE:13", "EMPLOYEE:99", "NO_TABLE:1", "EMPLOYEE:x").map { DataItemIdentity(it) }
        val deleted = sut.delete(noAccess, ids.asFlow()).toList()
        assertEquals(listOf(DataItemIdentity("EMPLOYEE:13")), deleted)
        sut.commit()
        assertEquals(listOf("Alice", "Bob", "Carol"), names(url))
        sut.close()
    }

    @Test
    fun delete_of_a_referenced_row_fails() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        sut.beginTransaction()
        assertFailsWith<java.sql.SQLException> { sut.delete(noAccess, flowOf(DataItemIdentity("DEPARTMENT:1"))).toList() }
        sut.rollback()
        sut.close()
    }

    @Test
    fun key_text_is_escaped_in_identity_and_delete() = runTest {
        val url = TestDb.create("CREATE TABLE K (A VARCHAR(20), B VARCHAR(20), PRIMARY KEY (A, B))", "INSERT INTO K VALUES ('x,y', 'z\\w')")
        val sut = open(url)
        val item = read(sut, "K").single()
        assertEquals("K:x\\,y,z\\\\w", item.identity.value)
        sut.beginTransaction()
        assertEquals(listOf(item.identity), sut.delete(noAccess, flowOf(item.identity)).toList())
        sut.commit()
        assertEquals(emptyList(), query(url, "SELECT A FROM K"))
        sut.close()
    }

    @Test
    fun set_foreign_key_end_sets_the_key_columns() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val am = sut.accessorMutator()
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        val sales = read(sut, "DEPARTMENT WHERE ID = 2").single()
        am.setPropertySuspend(bob.data, "DEPARTMENT", sales.data)
        assertEquals(2L, (bob.data.self as JdbcRow).values["DEPT_ID"])
        am.setPropertySuspend(bob.data, "FK_EMP_MANAGER", am.nothing())
        assertEquals(null, (bob.data.self as JdbcRow).values["MANAGER_ID"])
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(bob)).toList()
        assertTrue(out.single().issues.isEmpty(), out.single().issues.toString())
        sut.commit()
        assertEquals(listOf("2"), query(url, "SELECT DEPT_ID FROM EMPLOYEE WHERE ID = 11"))
        assertEquals(listOf("1"), query(url, "SELECT COUNT(*) FROM EMPLOYEE WHERE ID = 11 AND MANAGER_ID IS NULL"))
        sut.close()
    }

    @Test
    fun set_many_to_many_end_replaces_the_join_rows() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val am = sut.accessorMutator()
        val alice = read(sut, "EMPLOYEE WHERE ID = 10").single()
        val parser = read(sut, "PROJECT WHERE ID = 100").single()
        am.setPropertySuspend(alice.data, "PROJECT", am.toTypedObject(listOf(parser.data.self), StdLibDefault.List.type(listOf(parser.data.type.asTypeArgument))))
        // the set value is seen before it is written
        assertEquals(listOf(parser.data.self), (alice.data.getPropertySuspend("PROJECT").self as List<*>).map { if (it is TypedObject) it.self else it })
        sut.beginTransaction()
        val out = sut.write(noAccess, flowOf(alice)).toList()
        assertTrue(out.single().issues.isEmpty(), out.single().issues.toString())
        sut.commit()
        assertEquals(listOf("100"), query(url, "SELECT PROJ_ID FROM EMPLOYEE_PROJECT WHERE EMP_ID = 10"))
        assertEquals(listOf("101"), query(url, "SELECT PROJ_ID FROM EMPLOYEE_PROJECT WHERE EMP_ID = 11"))
        sut.close()
    }

    @Test
    fun set_foreign_key_end_to_a_row_of_another_table_fails() = runTest {
        val url = TestDb.create(*TestDb.HR)
        val sut = open(url)
        val bob = read(sut, "EMPLOYEE WHERE ID = 11").single()
        val project = read(sut, "PROJECT").first()
        assertFailsWith<IllegalArgumentException> { sut.accessorMutator().setPropertySuspend(bob.data, "DEPARTMENT", project.data) }
        sut.close()
    }

    private suspend fun openWithKeys(url: String, vararg keys: String): Datastore {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) {
            argument(JdbcProvider.CONFIG_URL, url)
            argument(JdbcProvider.CONFIG_KEYS, keys.toList())
        }
        return JdbcProvider().provide(DatastoreIdentity("db"), cfg).also { it.open() }
    }

    @Test
    fun configured_key_gives_view_rows_a_stable_identity() = runTest {
        val sut = openWithKeys(TestDb.create(*TestDb.HR), "active_employee: id")
        assertEquals(listOf("ACTIVE_EMPLOYEE:10", "ACTIVE_EMPLOYEE:12"), read(sut, "ACTIVE_EMPLOYEE").map { it.identity.value })
        // a view is still not written
        val item = read(sut, "ACTIVE_EMPLOYEE").first()
        sut.beginTransaction()
        assertEquals(1, sut.write(noAccess, flowOf(item)).toList().single().issues.errors.size)
        assertEquals(emptyList(), sut.delete(noAccess, flowOf(item.identity)).toList())
        sut.rollback()
        sut.close()
    }

    @Test
    fun configured_key_lets_a_table_without_primary_key_be_written_and_deleted() = runTest {
        val url = TestDb.create("CREATE TABLE LOG (CODE VARCHAR(10), MSG VARCHAR(100))", "INSERT INTO LOG VALUES ('a', 'one'), ('b', 'two')")
        val sut = openWithKeys(url, "LOG: CODE")
        val items = read(sut, "LOG")
        assertEquals(listOf("LOG:a", "LOG:b"), items.map { it.identity.value })
        (items[0].data.self as JdbcRow)["MSG"] = "uno"
        sut.beginTransaction()
        assertTrue(sut.write(noAccess, flowOf(items[0])).toList().single().issues.isEmpty())
        assertEquals(listOf(DataItemIdentity("LOG:b")), sut.delete(noAccess, flowOf(DataItemIdentity("LOG:b"))).toList())
        sut.commit()
        assertEquals(listOf("uno"), query(url, "SELECT MSG FROM LOG"))
        sut.close()
    }

    @Test
    fun configured_key_for_a_table_with_a_primary_key_fails() = runTest {
        val sut = openWithKeys(TestDb.create(*TestDb.HR), "EMPLOYEE: NAME")
        assertFailsWith<IllegalStateException> { sut.types() }
        sut.close()
    }
}
