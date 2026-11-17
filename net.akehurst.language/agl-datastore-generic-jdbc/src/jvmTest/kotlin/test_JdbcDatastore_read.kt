package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DatastoreConfigurationBuilder
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import kotlin.test.BeforeTest
import net.akehurst.language.objectgraph.api.TypedObject
import kotlin.test.Test
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class test_JdbcDatastore_read {

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(JdbcProvider())
    }

    private suspend fun open(url: String = TestDb.create(*TestDb.HR), config: DatastoreConfigurationBuilder.() -> Unit = {}): Datastore {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) {
            argument(JdbcProvider.CONFIG_URL, url)
            config()
        }
        return JdbcProvider().provide(DatastoreIdentity("db"), cfg).also { it.open() }
    }

    private suspend fun read(sut: Datastore, vararg tables: Any): List<DataItem> =
        sut.read(accessConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.ACCESS_TABLES, tables.toList()) }, emptyFlow()).toList()

    /** a List property's elements are TypedObjects */
    private fun row(element: Any?): JdbcRow = when (element) {
        is TypedObject -> element.self as JdbcRow
        else -> element as JdbcRow
    }

    private suspend fun DataItem.prop(name: String): Any? = data.getPropertySuspend(name).let { if (it.isNothing) null else it.self }

    @Test
    fun one_item_per_row_with_key_identity() = runTest {
        val sut = open()
        val items = read(sut, "DEPARTMENT", "EMPLOYEE")
        assertEquals(listOf("DEPARTMENT:1", "DEPARTMENT:2", "EMPLOYEE:10", "EMPLOYEE:11", "EMPLOYEE:12"), items.map { it.identity.value })
        items.forEach { assertTrue(it.issues.isEmpty(), it.issues.toString()) }
        assertEquals("PUBLIC.EMPLOYEE", items[2].data.type.resolvedDefinition.qualifiedName.value)
        val prov = items[2].provenance
        assertIs<JdbcProvenance>(prov)
        assertEquals("EMPLOYEE", prov.table)
        assertEquals(mapOf<String, Any?>("ID" to 10L), prov.key)
        sut.close()
    }

    @Test
    fun column_values() = runTest {
        val sut = open()
        val alice = read(sut, "EMPLOYEE").first()
        assertEquals(10L, alice.prop("ID"))
        assertEquals("Alice", alice.prop("NAME"))
        assertEquals(1L, alice.prop("DEPT_ID"))
        assertNull(alice.prop("MANAGER_ID"))
        assertEquals(Instant.parse("2020-01-15T00:00:00Z"), alice.prop("HIRED"))
        assertEquals(true, alice.prop("ACTIVE"))
        val bob = read(sut, "EMPLOYEE")[1]
        assertContentEquals(byteArrayOf(1, 2), bob.prop("PHOTO") as ByteArray)
        val eng = read(sut, "DEPARTMENT").first()
        assertEquals(1000.5, eng.prop("BUDGET"))
        sut.close()
    }

    @Test
    fun filter_per_table_as_line() = runTest {
        val sut = open()
        val items = read(sut, "EMPLOYEE where ACTIVE = TRUE", "DEPARTMENT WHERE NAME = 'Sales'", "PROJECT")
        assertEquals(listOf("EMPLOYEE:10", "EMPLOYEE:12", "DEPARTMENT:2", "PROJECT:100", "PROJECT:101"), items.map { it.identity.value })
        sut.close()
    }

    @Test
    fun filter_per_table_as_pair() = runTest {
        val sut = open()
        val items = read(sut, Pair("EMPLOYEE", "DEPT_ID = 1"), Pair("DEPARTMENT", null))
        assertEquals(listOf("EMPLOYEE:10", "EMPLOYEE:11", "DEPARTMENT:1", "DEPARTMENT:2"), items.map { it.identity.value })
        sut.close()
    }

    @Test
    fun line_that_is_not_a_table_or_where_fails() = runTest {
        val sut = open()
        assertFailsWith<IllegalStateException> { read(sut, "EMPLOYEE ACTIVE = TRUE") }
        sut.close()
    }

    @Test
    fun table_names_ignore_case_and_views_are_read() = runTest {
        val sut = open()
        val items = read(sut, "active_employee")
        assertEquals(listOf("ACTIVE_EMPLOYEE#0", "ACTIVE_EMPLOYEE#1"), items.map { it.identity.value })
        assertEquals(listOf("Alice", "Carol"), items.map { it.prop("NAME") })
        sut.close()
    }

    @Test
    fun unknown_table_fails() = runTest {
        val sut = open()
        assertFailsWith<IllegalStateException> { read(sut, "NO_SUCH_TABLE") }
        sut.close()
    }

    @Test
    fun join_table_cannot_be_read() = runTest {
        val sut = open()
        assertFailsWith<IllegalArgumentException> { read(sut, "EMPLOYEE_PROJECT") }
        sut.close()
    }

    @Test
    fun navigate_foreign_key_both_ways() = runTest {
        val sut = open()
        val bob = read(sut, "EMPLOYEE")[1]
        val dept = bob.data.getPropertySuspend("DEPARTMENT")
        assertEquals("Engineering", dept.getPropertySuspend("NAME").self)
        val manager = bob.data.getPropertySuspend("FK_EMP_MANAGER")
        assertEquals("Alice", manager.getPropertySuspend("NAME").self)
        // Alice has no manager
        val alice = read(sut, "EMPLOYEE")[0]
        assertTrue(alice.data.getPropertySuspend("FK_EMP_MANAGER").isNothing)

        val eng = read(sut, "DEPARTMENT")[0]
        val emps = eng.data.getPropertySuspend("EMPLOYEE").self as List<*>
        assertEquals(setOf("Alice", "Bob"), emps.map { row(it).values["NAME"] }.toSet())
        val reports = alice.data.getPropertySuspend("EMPLOYEE_FK_EMP_MANAGER").self as List<*>
        assertEquals(setOf("Bob", "Carol"), reports.map { row(it).values["NAME"] }.toSet())
        sut.close()
    }

    @Test
    fun navigate_many_to_many() = runTest {
        val sut = open()
        val alice = read(sut, "EMPLOYEE")[0]
        val projects = alice.data.getPropertySuspend("PROJECT").self as List<*>
        assertEquals(setOf("Parser", "Datastore"), projects.map { row(it).values["TITLE"] }.toSet())
        val datastore = read(sut, "PROJECT")[1]
        val people = datastore.data.getPropertySuspend("EMPLOYEE").self as List<*>
        assertEquals(setOf("Alice", "Bob"), people.map { row(it).values["NAME"] }.toSet())
        sut.close()
    }

    @Test
    fun rows_are_fetched_in_batches() = runTest {
        val url = TestDb.create(
            "CREATE TABLE T (ID INTEGER PRIMARY KEY)",
            "INSERT INTO T SELECT X FROM SYSTEM_RANGE(1, 25)",
        )
        val sut = open(url) { argument(JdbcProvider.CONFIG_FETCH_SIZE, 10L) }
        assertEquals((1..25).map { "T:$it" }, read(sut, "T").map { it.identity.value })
        sut.close()
    }

    @Test
    fun read_needs_open() = runTest {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.CONFIG_URL, TestDb.create(*TestDb.HR)) }
        val sut = JdbcProvider().provide(DatastoreIdentity("db"), cfg)
        assertFailsWith<IllegalStateException> { read(sut, "EMPLOYEE") }
    }

    @Test
    fun testConnection_and_testAccess() = runTest {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.CONFIG_URL, TestDb.create(*TestDb.HR)) }
        val sut = JdbcProvider().provide(DatastoreIdentity("db"), cfg)
        assertTrue(sut.testConnection().success)
        val r = sut.testAccess(accessConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.ACCESS_TABLES, listOf("EMPLOYEE")) })
        assertTrue(r.success, r.message)
        assertEquals(1, r.itemCount)
        val bad = JdbcProvider().provide(DatastoreIdentity("bad"), datastoreConfiguration(JdbcProvider.IDENTITY) { argument(JdbcProvider.CONFIG_URL, "jdbc:nosuchdriver:x") })
        assertTrue(bad.testConnection().success.not())
    }
}
