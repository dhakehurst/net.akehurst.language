package net.akehurst.datastore.generic.jdbc

import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DatastoreConfigurationBuilder
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.TypesDomain
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** an H2 in-memory database for a test, kept until the JVM ends, created with 'ddl' */
internal object TestDb {
    private val counter = AtomicInteger()

    fun create(vararg ddl: String): String {
        val url = "jdbc:h2:mem:jdbctest${counter.incrementAndGet()};DB_CLOSE_DELAY=-1"
        DriverManager.getConnection(url).use { c -> c.createStatement().use { st -> ddl.forEach { st.execute(it) } } }
        return url
    }

    val HR = arrayOf(
        "CREATE TABLE DEPARTMENT (ID INTEGER PRIMARY KEY, NAME VARCHAR(100) NOT NULL, BUDGET DECIMAL(12,2))",
        "CREATE TABLE EMPLOYEE (ID INTEGER PRIMARY KEY, NAME VARCHAR(100) NOT NULL, DEPT_ID INTEGER, MANAGER_ID INTEGER, " +
            "HIRED DATE, ACTIVE BOOLEAN, PHOTO BLOB, " +
            "CONSTRAINT FK_EMP_DEPT FOREIGN KEY (DEPT_ID) REFERENCES DEPARTMENT(ID), " +
            "CONSTRAINT FK_EMP_MANAGER FOREIGN KEY (MANAGER_ID) REFERENCES EMPLOYEE(ID))",
        "CREATE TABLE PROJECT (ID INTEGER PRIMARY KEY, TITLE VARCHAR(100))",
        "CREATE TABLE EMPLOYEE_PROJECT (EMP_ID INTEGER, PROJ_ID INTEGER, PRIMARY KEY (EMP_ID, PROJ_ID), " +
            "CONSTRAINT FK_EP_EMP FOREIGN KEY (EMP_ID) REFERENCES EMPLOYEE(ID), " +
            "CONSTRAINT FK_EP_PROJ FOREIGN KEY (PROJ_ID) REFERENCES PROJECT(ID))",
        "CREATE VIEW ACTIVE_EMPLOYEE AS SELECT ID, NAME FROM EMPLOYEE WHERE ACTIVE",
        "INSERT INTO DEPARTMENT VALUES (1, 'Engineering', 1000.50), (2, 'Sales', NULL)",
        "INSERT INTO EMPLOYEE VALUES (10, 'Alice', 1, NULL, DATE '2020-01-15', TRUE, NULL), (11, 'Bob', 1, 10, NULL, FALSE, X'0102'), (12, 'Carol', 2, 10, NULL, TRUE, NULL)",
        "INSERT INTO PROJECT VALUES (100, 'Parser'), (101, 'Datastore')",
        "INSERT INTO EMPLOYEE_PROJECT VALUES (10, 100), (10, 101), (11, 101)",
    )
}

class test_JdbcDatastore_types {

    private fun datastore(url: String, config: DatastoreConfigurationBuilder.() -> Unit = {}): Datastore {
        val cfg = datastoreConfiguration(JdbcProvider.IDENTITY) {
            argument(JdbcProvider.CONFIG_URL, url)
            config()
        }
        return JdbcProvider().provide(DatastoreIdentity("db"), cfg)
    }

    private fun TypesDomain.dataType(qn: String): DataType? = findFirstDefinitionByPossiblyQualifiedNameOrNull(qn.asPossiblyQualifiedName) as? DataType

    private fun DataType.propertyTypes(): Map<String, String> =
        allProperty.values.associate { p -> Pair(p.name.value, p.typeInstance.signature(namespace, 0)) }

    @Test
    fun types_without_open_connect_for_the_metadata() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR))
        val types = sut.types()
        assertTrue(sut.isOpen.not())
        assertNotNull(types.dataType("PUBLIC.EMPLOYEE"))
    }

    @Test
    fun one_type_per_table_and_view_but_not_join_tables() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR))
        val types = sut.types()
        val ns = types.findNamespaceOrNull("PUBLIC".asQualifiedName)
        assertNotNull(ns)
        assertEquals(setOf("ACTIVE_EMPLOYEE", "DEPARTMENT", "EMPLOYEE", "PROJECT"), ns.definition.map { it.name.value }.toSet())
    }

    @Test
    fun column_properties_have_std_types() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR))
        val emp = sut.types().dataType("PUBLIC.EMPLOYEE")!!
        val props = emp.propertyTypes()
        assertEquals("Integer", props["ID"])
        assertEquals("String", props["NAME"])
        assertEquals("Integer?", props["DEPT_ID"])
        assertEquals("Timestamp?", props["HIRED"])
        assertEquals("Boolean?", props["ACTIVE"])
        assertEquals("Binary?", props["PHOTO"])
        val dept = sut.types().dataType("PUBLIC.DEPARTMENT")!!
        assertEquals("Real?", dept.propertyTypes()["BUDGET"])
    }

    @Test
    fun foreign_keys_are_associations() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR))
        val types = sut.types()
        val emp = types.dataType("PUBLIC.EMPLOYEE")!!.propertyTypes()
        val dept = types.dataType("PUBLIC.DEPARTMENT")!!.propertyTypes()
        // one key to DEPARTMENT: named after the table
        assertEquals("DEPARTMENT?", emp["DEPARTMENT"])
        assertEquals("List<EMPLOYEE>", dept["EMPLOYEE"])
        // a self reference: named after the key
        assertEquals("EMPLOYEE?", emp["FK_EMP_MANAGER"])
        assertEquals("List<EMPLOYEE>", emp["EMPLOYEE_FK_EMP_MANAGER"])
    }

    @Test
    fun join_table_is_many_to_many() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR))
        val types = sut.types()
        assertEquals("List<PROJECT>", types.dataType("PUBLIC.EMPLOYEE")!!.propertyTypes()["PROJECT"])
        assertEquals("List<EMPLOYEE>", types.dataType("PUBLIC.PROJECT")!!.propertyTypes()["EMPLOYEE"])
    }

    @Test
    fun tables_filter_with_globs_ignoring_case() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR)) {
            argument(JdbcProvider.CONFIG_TABLES, listOf("emp*"))
        }
        val types = sut.types()
        assertNotNull(types.dataType("PUBLIC.EMPLOYEE"))
        assertNull(types.dataType("PUBLIC.DEPARTMENT"))
        // EMPLOYEE_PROJECT is selected, but PROJECT is not, so it is not a join table and has a type
        assertNotNull(types.dataType("PUBLIC.EMPLOYEE_PROJECT"))
        // no association to an unselected table; the column stays
        assertNull(types.dataType("PUBLIC.EMPLOYEE")!!.propertyTypes()["DEPARTMENT"])
        assertEquals("Integer?", types.dataType("PUBLIC.EMPLOYEE")!!.propertyTypes()["DEPT_ID"])
    }

    @Test
    fun views_can_be_left_out() = runTest {
        val sut = datastore(TestDb.create(*TestDb.HR)) {
            argument(JdbcProvider.CONFIG_INCLUDE_VIEWS, false)
        }
        assertNull(sut.types().dataType("PUBLIC.ACTIVE_EMPLOYEE"))
    }

    @Test
    fun column_named_like_a_table_does_not_clash() = runTest {
        val url = TestDb.create(
            "CREATE TABLE A (ID INTEGER PRIMARY KEY)",
            "CREATE TABLE B (ID INTEGER PRIMARY KEY, A INTEGER, CONSTRAINT FK_B_A FOREIGN KEY (A) REFERENCES A(ID))",
        )
        val b = datastore(url).types().dataType("PUBLIC.B")!!.propertyTypes()
        assertEquals("Integer?", b["A"])
        assertEquals("A?", b["FK_B_A"])
    }

    @Test
    fun schema_selects_the_namespace() = runTest {
        val url = TestDb.create(
            "CREATE SCHEMA HR",
            "CREATE TABLE HR.PERSON (ID INTEGER PRIMARY KEY)",
            "CREATE TABLE OTHER_TABLE (ID INTEGER PRIMARY KEY)",
        )
        val types = datastore(url) { argument(JdbcProvider.CONFIG_SCHEMA, "HR") }.types()
        assertNotNull(types.dataType("HR.PERSON"))
        assertNull(types.dataType("PUBLIC.OTHER_TABLE"))
    }
}
