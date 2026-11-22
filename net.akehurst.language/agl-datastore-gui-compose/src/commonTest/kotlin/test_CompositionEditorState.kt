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
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
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

        val spTypes = typesDomain("sp", true) {
            namespace("microsoft.sharepoint") {
                data("SourceFile") {
                    propertyOf(setOf(REF, VAL), "name", "String")
                    propertyOf(setOf(CMP, VAL), "content", "FileContent")
                }
                data("FileContent") {
                    propertyOf(setOf(REF, VAL), "asBytes", "Binary")
                    propertyOf(setOf(REF, VAL), "asText", "String")
                }
                data("Folder") {
                    propertyOf(setOf(REF, VAL), "name", "String")
                }
            }
        }
        val wordTypes = typesDomain("word", true) {
            namespace("microsoft.word") {
                data("WordFile") {
                    constructor_ {
                        parameter(setOf(REF, VAL), "bytes", "Any")
                    }
                }
                data("Report") {
                    constructor_ {
                        parameter(setOf(REF, VAL), "title", "String")
                        parameter(setOf(REF, VAL), "bytes", "Binary")
                    }
                }
            }
        }

        /** runs a suspend function that does not suspend */
        fun <T> runNow(block: suspend () -> T): T {
            var result: Result<T>? = null
            block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
            return result!!.getOrThrow()
        }
    }

    private fun newState() = CompositionEditorState(listOf(fsProvider, txtProvider), listOf("keychain"), suggestedIdentity = "c1")

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
    fun moving_a_part_removes_connections_that_no_longer_join_neighbours() {
        val s = validState()
        val c = s.connections[0]
        val move = assertNotNull(s.movePart(0, 1))
        assertEquals(listOf("txt", "fs"), s.parts.map { it.id })
        assertEquals(listOf(c), move.removedConnections.map { it.second })
        assertEquals(0, s.connections.size)
        assertFalse(s.issues.any { it.location is IssueLocation.Connection })
    }

    @Test
    fun undo_a_move_restores_order_and_connections() {
        val s = validState()
        val c = s.connections[0]
        val move = assertNotNull(s.movePart(0, 1))
        s.undoMove(move)
        assertEquals(listOf("fs", "txt"), s.parts.map { it.id })
        assertEquals(listOf(c), s.connections.toList())
        assertFalse(s.hasErrors)
    }

    @Test
    fun moving_keeps_connections_that_still_join_neighbours() {
        val s = validState()
        val fs2 = s.addPart(FS, "fs2")
        // fs -> txt stays neighbouring when fs2 moves before fs
        val move = assertNotNull(s.movePart(2, 0))
        assertEquals(listOf("fs2", "fs", "txt"), s.parts.map { it.id })
        assertEquals(0, move.removedConnections.size)
        assertEquals(1, s.connections.size)
        assertTrue(fs2 in s.parts)
        assertNull(s.movePart(1, 1))
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
        val composition = DatastoreCompositionBuilder("c1", registry).apply {
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
        val composition = DatastoreCompositionBuilder("c1", registry).apply {
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
    fun types_status_is_loading_until_requested() {
        val s = validState()
        assertEquals(TypesStatus.Loading, s.typesStatus(s.parts[0]))
        assertNull(s.typesOrNull(s.parts[0]))
    }

    @Test
    fun types_found() {
        val s = validState()
        val fs = s.parts[0]
        var asked: DatastoreConfiguration? = null
        runNow { s.requestTypes(fs) { _, _, cfg -> asked = cfg; TypesResult.Found(fsTypes, listOf("fs.FileHandle")) } }
        assertEquals("/data", asked?.getAsString("root"))
        assertEquals(TypesResult.Found(fsTypes, listOf("fs.FileHandle")), s.typesStatus(fs))
        assertEquals(fsTypes, s.typesOrNull(fs))
        assertEquals(1, s.typesCount(fsTypes))
    }

    @Test
    fun types_not_available_with_a_reason() {
        val s = validState()
        val fs = s.parts[0]
        runNow { s.requestTypes(fs) { _, _, _ -> TypesResult.NotAvailable("not connected") } }
        assertEquals(TypesResult.NotAvailable("not connected"), s.typesStatus(fs))
        assertNull(s.typesOrNull(fs))
    }

    @Test
    fun types_callback_that_throws_is_not_available_with_its_message() {
        val s = validState()
        val fs = s.parts[0]
        runNow { s.requestTypes(fs) { _, _, _ -> error("server down") } }
        assertEquals(TypesResult.NotAvailable("server down"), s.typesStatus(fs))
    }

    @Test
    fun types_not_available_without_callback_or_with_configuration_errors() {
        val s = validState()
        val fs = s.parts[0]
        runNow { s.requestTypes(fs, null) }
        assertEquals(TypesResult.NotAvailable("no typesFor callback"), s.typesStatus(fs))
        fs.datastore.parameter["root"]!!.text = ""
        var called = false
        runNow { s.requestTypes(fs) { _, _, _ -> called = true; TypesResult.Found(fsTypes) } }
        assertFalse(called)
        assertEquals(TypesResult.NotAvailable("the configuration has errors"), s.typesStatus(fs))
    }

    @Test
    fun types_retry_changes_the_request_key() {
        val s = validState()
        val fs = s.parts[0]
        val k = s.typesRequestKey(fs)
        s.retryTypes(fs)
        assertTrue(k != s.typesRequestKey(fs))
        assertEquals(0, s.typesRequestKey(s.parts[1]))
    }

    /** sp -> word, with types, and a connection with no ends */
    private fun spWordState(spItemTypes: List<String> = emptyList()): Pair<CompositionEditorState, ConnectionDraft> {
        val s = newState()
        val sp = s.addPart(FS, "sp")
        val word = s.addPart(TXT, "word")
        s.setTypesStatus(sp, TypesResult.Found(spTypes, spItemTypes))
        s.setTypes(word, wordTypes)
        return Pair(s, s.addConnection(sp, word, "microsoft.word.WordFile"))
    }

    private fun ends(c: ConnectionDraft) = c.ends.map { Triple(it.name, it.typeName, it.expression) }

    @Test
    fun prefill_ends_with_exactly_one_item_type() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        assertTrue(s.prefillEnds(c))
        assertEquals(listOf(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), Triple("bytes", "Any", "")), ends(c))
    }

    @Test
    fun prefill_ends_with_one_data_type_of_the_source() {
        val s = validState()
        val c = s.addConnection(s.parts[0], s.parts[1], "txt.ParsableUnit")
        s.connections.remove(s.connections[0])
        s.setTypes(s.parts[0], fsTypes)
        s.setTypes(s.parts[1], txtTypes)
        assertTrue(s.prefillEnds(c))
        // ParsableUnit of the test types has no constructor
        assertEquals(listOf(Triple("owner", "fs.FileHandle", "\$self")), ends(c))
    }

    @Test
    fun prefill_ends_leaves_the_owner_type_when_ambiguous() {
        val (s, c) = spWordState()
        assertTrue(s.prefillEnds(c))
        assertEquals(listOf(Triple("owner", "", "\$self"), Triple("bytes", "Any", "")), ends(c))
    }

    @Test
    fun prefill_ends_does_not_change_ends_already_there() {
        val (s, c) = spWordState()
        c.addEnd("mine", "String", "name")
        c.addEnd("bytes", "Binary", "content.asBytes")
        assertTrue(s.prefillEnds(c))
        // owner is added, 'bytes' is not added again, the user's ends are unchanged
        assertEquals(listOf(Triple("owner", "", "\$self"), Triple("mine", "String", "name"), Triple("bytes", "Binary", "content.asBytes")), ends(c))
        assertEquals(listOf(true, false, false), c.ends.map { it.prefilled })
        assertFalse(s.prefillEnds(c))
    }

    @Test
    fun prefilled_ends_are_marked_and_editing_clears_the_mark() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        s.prefillEnds(c)
        assertEquals(listOf(true, true), c.ends.map { it.prefilled })
        c.ends[1].expression = "content.asBytes"
        assertFalse(c.ends[1].prefilled)
        c.ends[0].typeName = "microsoft.sharepoint.Folder"
        assertFalse(c.ends[0].prefilled)
        val e = c.addEnd("x", "String", "", prefilled = true)
        e.name = "y"
        assertFalse(e.prefilled)
        // setting the same value is not an edit
        val f = c.addEnd("z", "String", "", prefilled = true)
        f.name = "z"
        assertTrue(f.prefilled)
    }

    @Test
    fun target_type_change_replaces_prefilled_ends_and_keeps_owner() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        c.targetType = ""
        s.setTargetType(c, "microsoft.word.WordFile")
        assertEquals(listOf(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), Triple("bytes", "Any", "")), ends(c))
        val owner = c.ends[0]
        s.setTargetType(c, "microsoft.word.Report")
        assertEquals(listOf(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), Triple("title", "String", ""), Triple("bytes", "Binary", "")), ends(c))
        assertTrue(owner === c.ends[0]) // owner kept, its type is unchanged
    }

    @Test
    fun target_type_change_keeps_edited_ends_without_duplicate_names() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        c.targetType = ""
        s.setTargetType(c, "microsoft.word.Report")
        c.ends.first { it.name == "bytes" }.expression = "content.asBytes"
        s.setTargetType(c, "microsoft.word.WordFile")
        // 'title' was still pre-filled, so removed; the edited 'bytes' is kept and not added again
        assertEquals(listOf(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), Triple("bytes", "Binary", "content.asBytes")), ends(c))
        assertEquals(1, c.ends.count { it.name == "bytes" })
    }

    @Test
    fun target_type_change_keeps_an_edited_owner() {
        val (s, c) = spWordState()
        c.targetType = ""
        s.setTargetType(c, "microsoft.word.WordFile")
        c.ends[0].typeName = "microsoft.sharepoint.Folder"
        s.setTargetType(c, "microsoft.word.Report")
        assertEquals(Triple("owner", "microsoft.sharepoint.Folder", "\$self"), ends(c)[0])
        assertEquals(1, c.ends.count { it.name == "owner" })
    }

    @Test
    fun target_type_change_replaces_a_prefilled_owner_whose_type_is_now_different() {
        val (s, c) = spWordState()
        c.targetType = ""
        s.setTargetType(c, "microsoft.word.WordFile")
        assertEquals("", c.ends[0].typeName) // ambiguous
        s.setTypesStatus(c.source, TypesResult.Found(spTypes, listOf("microsoft.sharepoint.SourceFile")))
        s.setTargetType(c, "microsoft.word.Report")
        assertEquals(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), ends(c)[0])
        assertTrue(c.ends[0].prefilled)
    }

    @Test
    fun end_that_is_not_a_constructor_parameter_is_a_warning() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        s.prefillEnds(c)
        c.ends[1].expression = "content.asBytes"
        fun warnings() = s.issues.filter { it.severity == IssueSeverity.WARNING && (it.location as? IssueLocation.Connection)?.field == ConnectionField.END_NAME }
        assertEquals(emptyList(), warnings())
        val sheet = c.addEnd("sheet", "String", "name")
        assertEquals(listOf("'sheet' is not a constructor parameter of 'microsoft.word.WordFile'"), warnings().map { it.message })
        assertEquals(sheet, (warnings()[0].location as IssueLocation.Connection).end)
        // an end whose type is a data type of the source is an association end, not a constructor argument
        sheet.typeName = "microsoft.sharepoint.Folder"
        assertEquals(emptyList(), warnings())
    }

    @Test
    fun target_type_change_keeps_ends_while_types_are_not_available() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        c.targetType = ""
        s.setTargetType(c, "microsoft.word.WordFile")
        s.setTypesStatus(c.target, TypesResult.NotAvailable("offline"))
        s.setTargetType(c, "microsoft.word.Report")
        assertEquals("microsoft.word.Report", c.targetType)
        assertEquals(listOf(Triple("owner", "microsoft.sharepoint.SourceFile", "\$self"), Triple("bytes", "Any", "")), ends(c))
    }

    @Test
    fun prefill_ends_needs_a_known_target_data_type() {
        val (s, c) = spWordState()
        c.targetType = "microsoft.word.Unknown"
        assertFalse(s.prefillEnds(c))
        assertEquals(0, c.ends.size)
    }

    @Test
    fun expression_suggestions_follow_properties_one_segment_at_a_time() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        s.prefillEnds(c)
        val bytes = c.ends[1]
        assertEquals(listOf("content", "name"), s.expressionSuggestions(c, bytes)) // Any: all conform, so by name
        bytes.expression = "content."
        assertEquals(listOf("content.asBytes", "content.asText"), s.expressionSuggestions(c, bytes))
        bytes.expression = "content.asT"
        assertEquals(listOf("content.asText"), s.expressionSuggestions(c, bytes))
        bytes.expression = "\$self.content.asB"
        assertEquals(listOf("\$self.content.asBytes"), s.expressionSuggestions(c, bytes))
        bytes.expression = "nothing.here"
        assertEquals(emptyList(), s.expressionSuggestions(c, bytes))
        // none for the $self end
        assertEquals(emptyList(), s.expressionSuggestions(c, c.ends[0]))
    }

    @Test
    fun expression_suggestions_rank_conforming_paths_first() {
        val (s, c) = spWordState(listOf("microsoft.sharepoint.SourceFile"))
        s.prefillEnds(c)
        val e = c.addEnd("text", "String", "content.")
        assertEquals(listOf("content.asText", "content.asBytes"), s.expressionSuggestions(c, e))
        e.expression = ""
        assertEquals(listOf("name", "content"), s.expressionSuggestions(c, e))
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
