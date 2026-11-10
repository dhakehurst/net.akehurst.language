package net.akehurst.datastore.common

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionIdentity
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.SecretReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class test_DatastoreConfigurationJson {

    private companion object {
        val SRC = DatastoreProviderIdentity("test.Source")
        val TGT = DatastoreProviderIdentity("test.Target")

        val source = object : DatastoreProvider {
            override val identity = SRC
            override val schema = datastoreProviderSchema {
                datastore {
                    parameter("url", "URL", false) { type("String") }
                    parameter("port", "Port", true) { type("Integer") }
                    parameter("timeout", "Timeout", true) { type("Real") }
                    parameter("readOnly", "Read only", true) { type("Boolean") }
                    parameter("password", "Password", true) {
                        type("String")
                        sensitive()
                    }
                }
                access {
                    parameter("filter", "Filter", true) { type("String") }
                    parameter("token", "API token", true) {
                        type("String")
                        sensitive()
                    }
                }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val target = object : DatastoreProvider {
            override val identity = TGT
            override val schema = datastoreProviderSchema {
                datastore {
                    parameter("language", "Language", false) { type("String") }
                }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val registry = DatastoreProviderRegistryDefault().also {
            it.register(source)
            it.register(target)
        }

        fun composition(password: Any = SecretReference("env", "PW")): DatastoreCompositionConfiguration =
            DatastoreCompositionBuilder("doors-reqs", registry).apply {
                name = "DOORS requirements"
                description = "Requirements read from DOORS"
                datastore("src", SRC) {
                    argument("url", "http://example.com")
                    argument("port", 8080)
                    argument("timeout", 2.0)
                    argument("readOnly", true)
                    argument("password", password)
                }
                datastore("txt", TGT) {
                    argument("language", "json")
                }
                connect("src", "txt", "ParsableUnit", "unit") {
                    end("text", "String", "\$self.content")
                    end("owner", "FileHandle", "\$self")
                }
            }.build().configuration
    }

    @Test
    fun composition_round_trip() {
        val cfg = composition()
        val text = DatastoreConfigurationJson.encode(cfg)
        val actual = DatastoreConfigurationJson.decode(text, registry)

        assertEquals(DatastoreCompositionIdentity("doors-reqs"), actual.identity)
        assertEquals("DOORS requirements", actual.name)
        assertEquals("Requirements read from DOORS", actual.description)
        assertEquals(listOf("src", "txt"), actual.part.keys.map { it.second.value })
        assertEquals(listOf(SRC, TGT), actual.part.keys.map { it.first })
        val src = actual.part.values.first()
        assertEquals("http://example.com", src.argument["url"])
        assertEquals(8080L, src.argument["port"])
        assertEquals(2.0, src.argument["timeout"])
        assertEquals(true, src.argument["readOnly"])
        assertEquals(SecretReference("env", "PW"), src.argument["password"])
        // the schema is re-attached from the registry
        assertIs<DatastoreConfigurationDefault>(src)
        assertSame(source.schema.datastore, src.parameterSet)
        assertTrue(src.isSensitive("password"))

        val conn = actual.connection.single()
        assertEquals("src", conn.source.value)
        assertEquals("txt", conn.target.value)
        assertEquals("ParsableUnit", conn.targetType)
        assertEquals("unit", conn.name)
        assertEquals(listOf("text", "owner"), conn.end.map { it.name })
        assertEquals("\$self.content", conn.end[0].expression)
        assertEquals("FileHandle", conn.end[1].typeName)

        // encoding again gives the same document
        assertEquals(text, DatastoreConfigurationJson.encode(actual))
    }

    @Test
    fun numbers_use_the_schema_type() {
        // a Real written without a fraction, and an Integer, both read as JSON numbers
        val text = """
            { "composition": { "id": "c1", "part": [
                { "provider": "test.Source", "id": "src", "configuration": { "url": "u", "port": 1, "timeout": 3 } }
            ] } }
        """.trimIndent()
        val src = DatastoreConfigurationJson.decode(text, registry).part.values.single()
        assertEquals(1L, src.argument["port"])
        assertEquals(3.0, src.argument["timeout"])
    }

    @Test
    fun integer_with_fraction_fails() {
        val text = """
            { "composition": { "id": "c1", "part": [
                { "provider": "test.Source", "id": "src", "configuration": { "url": "u", "port": 1.5 } }
            ] } }
        """.trimIndent()
        val e = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decode(text, registry) }
        assertTrue(e.message!!.contains("'port' must be an Integer"), e.message)
    }

    @Test
    fun plain_sensitive_value_is_refused() {
        val cfg = composition(password = "secret123")
        val e = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.encode(cfg) }
        assertTrue(e.message!!.contains("sensitive parameter 'password'"), e.message)
        assertFalse(e.message!!.contains("secret123"), e.message)
    }

    @Test
    fun unknown_provider_fails() {
        val text = DatastoreConfigurationJson.encode(composition())
        val other = DatastoreProviderRegistryDefault().also { it.register(target) }
        val e = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decode(text, other) }
        assertTrue(e.message!!.contains("provider 'test.Source' is not registered"), e.message)
    }

    @Test
    fun parameter_not_in_schema_fails() {
        val text = """
            { "composition": { "id": "c1", "part": [
                { "provider": "test.Source", "id": "src", "configuration": { "url": "u", "colour": "red" } }
            ] } }
        """.trimIndent()
        val e = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decode(text, registry) }
        assertTrue(e.message!!.contains("'colour' is not a datastore parameter of provider 'test.Source'"), e.message)
    }

    @Test
    fun access_round_trip() {
        val comp = composition()
        val acc = accessCompositeConfiguration(comp, registry) {
            access("src") {
                argument("filter", "*.json")
                argument("token", secret("keepass", "api"))
            }
        }
        val text = DatastoreConfigurationJson.encode(acc)
        val actual = DatastoreConfigurationJson.decodeAccess(text, registry)

        assertEquals(listOf("src", "txt"), actual.datastoreConfiguration.part.keys.map { it.second.value })
        val a = actual.part[DatastoreIdentity("src")]!!
        assertEquals("*.json", a.argument["filter"])
        assertEquals(SecretReference("keepass", "api"), a.argument["token"])
        assertIs<DatastoreConfigurationDefault>(a)
        assertSame(source.schema.access, a.parameterSet)

        // the composition can be read from the same document
        assertEquals(2, DatastoreConfigurationJson.decode(text, registry).part.size)
    }

    @Test
    fun access_plain_sensitive_value_is_refused() {
        val acc = accessCompositeConfiguration(composition(), registry) {
            access("src") { argument("token", "abc") }
        }
        val e = assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.encode(acc) }
        assertTrue(e.message!!.contains("sensitive parameter 'token'"), e.message)
    }

    @Test
    fun decodeAccess_without_access_section_fails() {
        val text = DatastoreConfigurationJson.encode(composition())
        assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decodeAccess(text, registry) }
    }

    @Test
    fun id_name_and_description_are_written_first() {
        val text = DatastoreConfigurationJson.encode(composition())
        val keys = Regex("\"(id|name|description|part|connection)\"\\s*:").findAll(text.substringAfter("\"composition\"")).map { it.groupValues[1] }.toList()
        assertEquals(listOf("id", "name", "description", "part"), keys.take(4), text)
        assertTrue(text.contains("\"id\": \"doors-reqs\""), text)
        assertTrue(text.contains("\"name\": \"DOORS requirements\""), text)
    }

    @Test
    fun name_and_description_default_to_empty() {
        val text = """
            { "composition": { "id": "c1", "part": [
                { "provider": "test.Target", "id": "txt", "configuration": { "language": "json" } }
            ] } }
        """.trimIndent()
        val cfg = DatastoreConfigurationJson.decode(text, registry)
        assertEquals(DatastoreCompositionIdentity("c1"), cfg.identity)
        assertEquals("", cfg.name)
        assertEquals("", cfg.description)
        // they are written, even when empty
        val again = DatastoreConfigurationJson.encode(cfg)
        assertTrue(again.contains("\"name\": \"\""), again)
        assertTrue(again.contains("\"description\": \"\""), again)
    }

    @Test
    fun missing_id_fails() {
        val text = """
            { "composition": { "part": [
                { "provider": "test.Target", "id": "txt", "configuration": { "language": "json" } }
            ] } }
        """.trimIndent()
        val e = assertFails { DatastoreConfigurationJson.decode(text, registry) }
        assertTrue(e.message!!.contains("'id'"), e.message)
    }

    @Test
    fun invalid_id_fails() {
        listOf("", " ", "1abc", "-abc", "a b", "a.b", "a/b", "ä").forEach { id ->
            val text = """
                { "composition": { "id": "$id", "part": [
                    { "provider": "test.Target", "id": "txt", "configuration": { "language": "json" } }
                ] } }
            """.trimIndent()
            val e = assertFailsWith<IllegalStateException>(id) { DatastoreConfigurationJson.decode(text, registry) }
            assertTrue(e.message!!.contains("Composition id '$id' is not valid"), e.message)
        }
    }

    @Test
    fun invalid_id_is_refused_by_decodeAccess() {
        val text = DatastoreConfigurationJson.encode(composition()).replace("\"doors-reqs\"", "\"9x\"")
        assertFailsWith<IllegalStateException> { DatastoreConfigurationJson.decodeAccess(text, registry) }
    }
}
