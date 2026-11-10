package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionIdentity
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DatastoreCompositionBuilder
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.datastoreProviderSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_CompositionIdentity {

    private companion object {
        val P = DatastoreProviderIdentity("test.p")

        val provider = object : DatastoreProvider {
            override val identity = P
            override val schema = datastoreProviderSchema {
                datastore { parameter("url", "URL", true) { type("String") } }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val registry = DatastoreProviderRegistryDefault().also { it.register(provider) }

        fun loaded() = DatastoreCompositionBuilder("doors-reqs", registry).apply {
            name = "DOORS requirements"
            description = "what it is for"
            datastore("a", P) { }
        }.build().configuration

        fun identityIssues(s: CompositionEditorState) = s.issues.filter { it.location == IssueLocation.CompositionIdentity }
    }

    @Test
    fun new_composition_starts_with_the_suggested_identity_and_it_can_be_edited() {
        val s = CompositionEditorState(registry, suggestedIdentity = "proj-1")
        assertTrue(s.identityEditable)
        assertEquals("proj-1", s.identity)
        assertEquals("", s.name)
        assertEquals("", s.description)
        s.identity = "proj-2"
        assertEquals("proj-2", s.identity)
    }

    @Test
    fun build_returns_identity_name_and_description() {
        val s = CompositionEditorState(registry, suggestedIdentity = "c1")
        s.addPart(P, "a")
        s.name = "My composition"
        s.description = "for testing"
        val cfg = assertNotNull(s.build())
        assertEquals(DatastoreCompositionIdentity("c1"), cfg.identity)
        assertEquals("My composition", cfg.name)
        assertEquals("for testing", cfg.description)
    }

    @Test
    fun blank_identity_is_an_error() {
        val s = CompositionEditorState(registry)
        s.addPart(P, "a")
        assertEquals("", s.identity)
        assertEquals(listOf(IssueSeverity.ERROR), identityIssues(s).map { it.severity })
        assertTrue(s.hasErrors)
        assertNull(s.build())
        s.identity = "c1"
        assertEquals(emptyList(), identityIssues(s))
        assertFalse(s.hasErrors)
        assertNotNull(s.build())
    }

    @Test
    fun invalid_identity_is_an_error() {
        val s = CompositionEditorState(registry, suggestedIdentity = "c1")
        s.addPart(P, "a")
        listOf("1abc", "a b", "a.b", "-a", "ä").forEach { id ->
            s.identity = id
            val issue = identityIssues(s).single()
            assertEquals(IssueSeverity.ERROR, issue.severity, id)
            assertTrue(issue.message.contains("not valid"), issue.message)
            assertTrue(s.hasErrors, id)
            assertNull(s.build(), id)
        }
    }

    @Test
    fun invalid_identity_also_blocks_the_access_configuration() {
        val s = CompositionEditorState(registry, suggestedIdentity = "c1")
        s.addPart(P, "a")
        val acc = AccessEditorState(s)
        assertNotNull(acc.build())
        s.identity = "1"
        assertNull(acc.build())
    }

    @Test
    fun loaded_composition_identity_is_read_only() {
        val s = CompositionEditorState(registry, initial = loaded(), suggestedIdentity = "ignored")
        assertFalse(s.identityEditable)
        assertEquals("doors-reqs", s.identity)
        assertEquals("DOORS requirements", s.name)
        assertEquals("what it is for", s.description)
        assertFailsWith<IllegalStateException> { s.identity = "other" }
        assertEquals("doors-reqs", s.identity)
    }

    @Test
    fun name_and_description_of_a_loaded_composition_can_be_changed() {
        val s = CompositionEditorState(registry, initial = loaded())
        s.name = "renamed"
        s.description = "new text"
        val cfg = assertNotNull(s.build())
        assertEquals(DatastoreCompositionIdentity("doors-reqs"), cfg.identity)
        assertEquals("renamed", cfg.name)
        assertEquals("new text", cfg.description)
    }

    @Test
    fun load_makes_the_identity_read_only() {
        val s = CompositionEditorState(registry, suggestedIdentity = "c1")
        assertTrue(s.identityEditable)
        s.load(loaded())
        assertFalse(s.identityEditable)
        assertEquals("doors-reqs", s.identity)
    }
}
