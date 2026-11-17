package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreLocation
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DatastoreCompositionBuilder
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.datastoreProviderSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_PartLocation {

    private val provider = object : DatastoreProvider {
        override val identity = DatastoreProviderIdentity("test.p")
        override val schema = datastoreProviderSchema {
            datastore { parameter("x", "X", true) { type("String") } }
            access { }
        }
        override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
    }
    private val registry = DatastoreProviderRegistryDefault().also { it.register(provider) }

    @Test
    fun new_part_is_any_and_location_is_built() {
        val state = CompositionEditorState(registry, suggestedIdentity = "c")
        val part = state.addPart(provider.identity, "a")
        assertEquals(DatastoreLocation.ANY, part.datastore.location)
        part.datastore.location = DatastoreLocation.SERVER
        val built = assertNotNull(state.build())
        assertEquals(DatastoreLocation.SERVER, built.part.values.single().location)
    }

    @Test
    fun location_is_loaded() {
        val initial = DatastoreCompositionBuilder("c", registry).apply {
            datastore("a", provider.identity) { location = DatastoreLocation.CLIENT }
        }.build().configuration
        val state = CompositionEditorState(registry, initial = initial)
        assertEquals(DatastoreLocation.CLIENT, state.parts.single().datastore.location)
        assertEquals(DatastoreLocation.CLIENT, assertNotNull(state.build()).part.values.single().location)
    }
}
