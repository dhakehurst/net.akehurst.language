package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.SecretReference
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.DatastoreCompositionBuilder
import net.akehurst.datastore.common.datastoreProviderSchema
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_CompositionEditorState {

    private companion object {
        val FS = DatastoreProviderIdentity("test.fs")
        val TXT = DatastoreProviderIdentity("test.txt")

        fun provider(id: DatastoreProviderIdentity, schema: net.akehurst.datastore.api.DatastoreProviderSchema) = object : DatastoreProvider {
            override val identity = id
            override val schema = schema
            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val fsProvider = provider(FS, datastoreProviderSchema {
            datastore {
                parameter("root", "Root", false) { type("String"); description("the root directory") }
                parameter("depth", "Depth", true) { type("Integer") }
                choice {
                    alternative("password") {
                        parameter("password", "Password", false) { type("String"); sensitive() }
                    }
                    alternative("token") {
                        parameter("token", "Token", false) { type("String"); sensitive() }
                    }
                }
            }
            access {
                parameter("glob", "Glob", true) { type("String") }
            }
        })

        val txtProvider = provider(TXT, datastoreProviderSchema {
            datastore {
                parameter("grammar", "Grammar", false) { type("String") }
            }
            access { }
        })

        val fsTypes = typesDomain("fs", true) {
            namespace("fs") {
                data("FileHandle") {
                    propertyOf(setOf(REF, VAL), "name", "String")
                }
            }
        }
        val txtTypes = typesDomain("txt", true) {
            namespace("txt") {
                data("ParsableUnit") {
                    propertyOf(setOf(REF, VAL), "text", "String")
                }
            }
        }
    }

    private fun newState() = CompositionEditorState(listOf(fsProvider, txtProvider), listOf("keychain"))

    /** fs -> txt, with valid configurations and one connection */
    private fun validState(): CompositionEditorState {
        val s = newState()
        val fs = s.addPart(FS, "fs")
        fs.datastore.parameter["root"]!!.text = "/data"
        fs.datastore.parameter["password"]!!.text = "pw"
        val txt = s.addPart(TXT, "txt")
        txt.datastore.parameter["grammar"]!!.text = "grammar G { }"
        val c = s.addConnection(fs, txt, "txt.ParsableUnit")
        c.addEnd("owner", "fs.FileHandle", "\$self")
        c.addEnd("text", "String", "name")
        return s
    }

    @Test
    fun empty_composition_has_an_error() {
        val s = newState()
        assertTrue(s.hasErrors)
        assertNull(s.build())
    }

    @Test
    fun valid_composition_builds() {
        val s = validState()
        assertEquals(emptyList(), s.issues.filter { it.severity == IssueSeverity.ERROR })
        val cfg = assertNotNull(s.build())
        assertEquals(listOf("fs", "txt"), cfg.part.keys.map { it.second.value })
        assertEquals("/data", cfg.part.values.first().getAsString("root"))
        assertEquals(1, cfg.connection.size)
        assertEquals("parsableUnit", cfg.connection[0].identity)
        assertEquals(listOf("owner", "text"), cfg.connection[0].end.map { it.name })
    }

    @Test
    fun missing_required_parameter_is_reported_on_the_parameter() {
        val s = validState()
        val fs = s.parts[0]
        fs.datastore.parameter["root"]!!.text = ""
        val issue = s.issues.first { it.severity == IssueSeverity.ERROR }
        assertEquals(IssueLocation.Part(fs, "root"), issue.location)
        assertNull(s.build())
    }

    @Test
    fun integer_is_converted_and_only_numbers_are_accepted() {
        val s = validState()
        val depth = s.parts[0].datastore.parameter["depth"]!!
        assertTrue(ParameterDraft.accepts(ParameterKind.INTEGER, "-12"))
        assertFalse(ParameterDraft.accepts(ParameterKind.INTEGER, "1a"))
        assertTrue(ParameterDraft.accepts(ParameterKind.REAL, "1.5e"))
        depth.text = "3"
        assertEquals(3L, s.build()!!.part.values.first().argument["depth"])
    }

    @Test
    fun parameters_of_an_alternative_not_chosen_are_ignored() {
        val s = validState()
        val fs = s.parts[0]
        // 'token' is required in its alternative, but 'password' is chosen
        assertNotNull(s.build())
        fs.datastore.choose("2", "token")
        assertTrue(s.issues.any { it.location == IssueLocation.Part(fs, "token") })
        fs.datastore.parameter["token"]!!.text = "tok"
        val cfg = assertNotNull(s.build())
        assertEquals(setOf("root", "token"), cfg.part.values.first().argument.keys)
    }

    @Test
    fun sensitive_parameter_can_be_a_secret_reference() {
        val s = validState()
        val pw = s.parts[0].datastore.parameter["password"]!!
        pw.mode = ValueMode.SECRET_REFERENCE
        pw.secretSource = "keychain"
        assertTrue(s.hasErrors) // no key yet
        pw.secretKey = "rm/david"
        assertEquals(SecretReference("keychain", "rm/david"), s.build()!!.part.values.first().argument["password"])
    }

    @Test
    fun duplicate_part_id_is_an_error() {
        val s = validState()
        s.parts[1].id = "fs"
        assertTrue(s.issues.any { it.location == IssueLocation.PartId(s.parts[1]) })
    }

    @Test
    fun connection_must_join_neighbours() {
        val s = validState()
        s.movePart(0, 1)
        assertTrue(s.issues.any { it.severity == IssueSeverity.ERROR && it.location is IssueLocation.Connection })
        s.movePart(1, 0)
        assertFalse(s.hasErrors)
    }

    @Test
    fun removing_a_part_removes_its_connections() {
        val s = validState()
        s.removePart(s.parts[1])
        assertEquals(0, s.connections.size)
    }

    @Test
    fun expression_that_does_not_parse_is_an_error() {
        val s = validState()
        s.connections[0].ends[1].expression = "name +"
        assertTrue(s.issues.any { it.severity == IssueSeverity.ERROR && (it.location as? IssueLocation.Connection)?.field == ConnectionField.END_EXPRESSION })
    }

    @Test
    fun with_types_the_target_type_and_expressions_are_checked() {
        val s = validState()
        s.setTypes(s.parts[0], fsTypes)
        s.setTypes(s.parts[1], txtTypes)
        assertEquals(emptyList(), s.issues)

        val c = s.connections[0]
        c.targetType = "txt.Unknown"
        assertTrue(s.issues.any { it.severity == IssueSeverity.ERROR && (it.location as? IssueLocation.Connection)?.field == ConnectionField.TARGET_TYPE })
        c.targetType = "txt.ParsableUnit"

        c.ends[1].expression = "notAProperty"
        assertTrue(s.issues.any { it.severity == IssueSeverity.WARNING && (it.location as? IssueLocation.Connection)?.field == ConnectionField.END_EXPRESSION })
    }

    @Test
    fun not_usable_for_write_is_a_warning() {
        val s = validState()
        s.connections[0].ends[0].expression = "name"
        assertTrue(s.issues.any { it.severity == IssueSeverity.WARNING && it.message.startsWith("Cannot be used for write") })
        assertNotNull(s.build())
    }

    @Test
    fun load_an_existing_composition() {
        val registry = DatastoreProviderRegistryDefault().also { it.register(fsProvider); it.register(txtProvider) }
        val composition = DatastoreCompositionBuilder(registry).apply {
            datastore("fs", FS) {
                argument("root", "/data")
                argument("token", secret("keychain", "k"))
            }
            datastore("txt", TXT) { argument("grammar", "g") }
            connect("fs", "txt", "txt.ParsableUnit") {
                end("owner", "fs.FileHandle", "\$self")
                end("text", "String", "name")
            }
        }.build()
        val s = CompositionEditorState(listOf(fsProvider, txtProvider), listOf("keychain"), composition.configuration)
        assertEquals(listOf("fs", "txt"), s.parts.map { it.id })
        val token = s.parts[0].datastore.parameter["token"]!!
        assertEquals(ValueMode.SECRET_REFERENCE, token.mode)
        assertEquals("token", s.parts[0].datastore.chosenAlternative("2", fsProvider.schema.datastore.parameterInfo[2] as net.akehurst.datastore.api.DsConfigParameterChoice))
        val cfg = assertNotNull(s.build())
        assertEquals(composition.configuration.part.values.map { it.argument }, cfg.part.values.map { it.argument })
        assertEquals(composition.configuration.connection, cfg.connection)
    }

    @Test
    fun provider_that_is_not_available_is_reported_when_loading() {
        val registry = DatastoreProviderRegistryDefault().also { it.register(fsProvider); it.register(txtProvider) }
        val composition = DatastoreCompositionBuilder(registry).apply {
            datastore("txt", TXT) { argument("grammar", "g") }
        }.build()
        val s = CompositionEditorState(listOf(fsProvider), initial = composition.configuration)
        assertEquals(0, s.parts.size)
        assertTrue(s.issues.any { it.location == IssueLocation.Composition && it.message.contains("test.txt") })
    }

    @Test
    fun providers_come_from_the_registry() {
        val registry = DatastoreProviderRegistryDefault().also { it.register(fsProvider); it.register(txtProvider) }
        val s = CompositionEditorState(registry)
        assertEquals(listOf(FS, TXT), s.providers.map { it.identity })
        assertEquals(txtProvider, s.providerOrNull(TXT))
    }

    @Test
    fun access_configuration() {
        val s = validState()
        val a = AccessEditorState(s)
        a.draftFor(s.parts[0]).parameter["glob"]!!.text = "**/*.txt"
        val acc = assertNotNull(a.build())
        assertEquals("**/*.txt", acc.part[DatastoreIdentity("fs")]!!.getAsString("glob"))
    }
}
