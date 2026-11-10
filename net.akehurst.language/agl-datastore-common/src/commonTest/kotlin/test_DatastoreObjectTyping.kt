package net.akehurst.datastore.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionExt.use
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreObjectTyping
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_DatastoreObjectTyping {

    /** one class for several types; the object carries the qualified name of its type */
    class Node(val typeName: String, val children: List<Node> = emptyList())

    /** a class used by only one type */
    class Leaf(val name: String)
    class Holder(val leaves: List<Leaf>)

    /** emits its input, then one root of its own */
    private abstract class TestDatastore(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : DatastoreAbstract(identity, configuration) {
        abstract val root: Any
        override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = flow {
            emitAll(input)
            val am = accessorMutator()
            emit(DataItemDefault(null, DataItemIdentity(identity.value), am.toTypedObject(root, StdLibDefault.AnyType), provenanceFor(null, root)))
        }

        override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> = input
    }

    private class DatastoreA(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : TestDatastore(identity, configuration), DatastoreObjectTyping {
        override val root = Node("a.Node", listOf(Node("a.ANode"), Node("a.AOther"), Node("a.ANode")))
        override fun qualifiedTypeNameOf(obj: Any): QualifiedName? = (obj as? Node)?.typeName?.let { QualifiedName(it) }
        override suspend fun createTypes(): TypesDomain = typesDomain("A", true) {
            namespace("a") {
                data("Node", Node::class) {
                    propertyOfWithBinding<Node, List<Node>>(setOf(REF, VAL), "children", "List", accessor = { it.children }) { typeArgument("Node") }
                }
                data("ANode", Node::class) { supertype("Node") }
                data("AOther", Node::class) { supertype("Node") }
            }
        }
    }

    private class DatastoreB(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : TestDatastore(identity, configuration), DatastoreObjectTyping {
        override val root = Node("b.Node", listOf(Node("b.BNode")))
        override fun qualifiedTypeNameOf(obj: Any): QualifiedName? = (obj as? Node)?.typeName?.let { QualifiedName(it) }
        override suspend fun createTypes(): TypesDomain = typesDomain("B", true) {
            namespace("b") {
                data("Node", Node::class) {
                    propertyOfWithBinding<Node, List<Node>>(setOf(REF, VAL), "children", "List", accessor = { it.children }) { typeArgument("Node") }
                }
                data("BNode", Node::class) { supertype("Node") }
            }
        }
    }

    /** does not implement DatastoreObjectTyping: its objects are typed by their class */
    private class DatastoreC(identity: DatastoreIdentity, configuration: DatastoreConfiguration) : TestDatastore(identity, configuration) {
        override val root = Holder(listOf(Leaf("x"), Leaf("y")))
        override suspend fun createTypes(): TypesDomain = typesDomain("C", true) {
            namespace("c") {
                data("Holder", Holder::class) {
                    propertyOfWithBinding<Holder, List<Leaf>>(setOf(REF, VAL), "leaves", "List", accessor = { it.leaves }) { typeArgument("Leaf") }
                }
                data("Leaf", Leaf::class)
            }
        }
    }

    private companion object {
        val PROVIDER_A = DatastoreProviderIdentity("test.typing.A")
        val PROVIDER_B = DatastoreProviderIdentity("test.typing.B")
        val PROVIDER_C = DatastoreProviderIdentity("test.typing.C")

        fun provider(id: DatastoreProviderIdentity, create: (DatastoreIdentity, DatastoreConfiguration) -> Datastore) = object : DatastoreProvider {
            override val identity = id
            override val schema = datastoreProviderSchema {
                datastore { }
                access { }
            }

            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = create(datastoreIdentity, configuration)
        }

        val registry = DatastoreProviderRegistryDefault().also {
            it.register(provider(PROVIDER_A) { i, c -> DatastoreA(i, c) })
            it.register(provider(PROVIDER_B) { i, c -> DatastoreB(i, c) })
            it.register(provider(PROVIDER_C) { i, c -> DatastoreC(i, c) })
        }

        fun composition(): DatastoreCompositionDefault {
            val comp = DatastoreCompositionDefault(object : DatastoreCompositionConfiguration {
                override val identity = net.akehurst.datastore.api.DatastoreCompositionIdentity("test")
                override val name = ""
                override val description = ""
                override val part = linkedMapOf(
                    Pair(PROVIDER_A, DatastoreIdentity("a")) to datastoreConfiguration(PROVIDER_A, registry) {},
                    Pair(PROVIDER_B, DatastoreIdentity("b")) to datastoreConfiguration(PROVIDER_B, registry) {},
                    Pair(PROVIDER_C, DatastoreIdentity("c")) to datastoreConfiguration(PROVIDER_C, registry) {},
                )
                override val connection = emptyList<DatastoreConnection>()
            })
            comp.create(registry)
            return comp
        }

        fun accessFor(comp: DatastoreCompositionDefault) = object : AccessCompositionConfiguration {
            override val datastoreConfiguration = comp.configuration
            override val part = mapOf(
                DatastoreIdentity("a") to accessConfiguration(PROVIDER_A, registry) {},
                DatastoreIdentity("b") to accessConfiguration(PROVIDER_B, registry) {},
                DatastoreIdentity("c") to accessConfiguration(PROVIDER_C, registry) {},
            )
        }

        val TypedObject.typeName get() = type.resolvedDefinition.qualifiedName.value

        fun elementTypeNames(collection: TypedObject): List<String> {
            val names = mutableListOf<String>()
            collection.forEachIndexed { _, v -> names.add(v.typeName) }
            return names
        }
    }

    @Test
    fun composition_types_nested_objects_by_their_datastore() = runTest {
        val comp = composition()
        val items = comp.use { it.read(accessFor(comp)).toList() }
        val byId = items.associateBy { it.identity.value }

        val a = byId["a"]!!.data
        assertEquals("a.Node", a.typeName)
        assertEquals(listOf("a.ANode", "a.AOther", "a.ANode"), elementTypeNames(a.getProperty("children")))

        val b = byId["b"]!!.data
        assertEquals("b.Node", b.typeName)
        assertEquals(listOf("b.BNode"), elementTypeNames(b.getProperty("children")))
    }

    @Test
    fun composition_types_by_class_when_datastore_does_not_decide() = runTest {
        val comp = composition()
        val items = comp.use { it.read(accessFor(comp)).toList() }
        val c = items.first { it.identity.value == "c" }.data
        assertEquals("c.Holder", c.typeName)
        assertEquals(listOf("c.Leaf", "c.Leaf"), elementTypeNames(c.getProperty("leaves")))
    }

    @Test
    fun composition_accessorMutator_types_by_datastore() = runTest {
        val comp = composition()
        val am = comp.accessorMutator()
        val root = am.toTypedObject(Node("b.Node", listOf(Node("b.BNode"), Node("a.AOther"))), StdLibDefault.AnyType)
        assertEquals("b.Node", root.typeName)
        assertEquals(listOf("b.BNode", "a.AOther"), elementTypeNames(root.getProperty("children")))
    }

    @Test
    fun datastore_on_its_own_types_by_itself() = runTest {
        val ds = DatastoreA(DatastoreIdentity("a"), datastoreConfiguration(PROVIDER_A, registry) {})
        val am = ds.accessorMutator()
        val root = am.toTypedObject(ds.root, StdLibDefault.AnyType)
        assertEquals("a.Node", root.typeName)
        assertEquals(listOf("a.ANode", "a.AOther", "a.ANode"), elementTypeNames(root.getProperty("children")))
    }
}
