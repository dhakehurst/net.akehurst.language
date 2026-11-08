package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.datastoreProviderSchema
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.builder.typesDomain
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_Preview {

    class Person(val name: String, val tags: List<String>, val friend: Person?)

    private companion object {
        val P = DatastoreProviderIdentity("test.p")
        val provider = object : DatastoreProvider {
            override val identity = P
            override val schema = datastoreProviderSchema {
                datastore { parameter("root", "Root", false) { type("String") } }
                access { parameter("glob", "Glob", false) { type("String") } }
            }
            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val types = typesDomain("people", true) {
            namespace("people") {
                data("Person") {
                    propertyOfWithBinding<Person, String>(setOf(REF, VAL), "name", "String", accessor = { it.name })
                    propertyOfWithBinding<Person, List<String>>(setOf(CMP, VAL), "tags", "List", accessor = { it.tags }) { typeArgument("String") }
                    propertyOfWithBinding<Person, Person?>(setOf(REF, VAL), "friend", "Person", isNullable = true, accessor = { it.friend })
                    propertyOfWithBinding<Person, String>(setOf(REF, VAL), "broken", "String", accessor = { error("cannot read") })
                }
            }
        }

        fun <T> runNow(block: suspend () -> T): T {
            var result: Result<T>? = null
            block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
            return result!!.getOrThrow()
        }

        fun typedPerson(p: Person) = ObjectGraphAccessorMutatorByReflection(types, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()).let { a ->
            a.typedAs(p, types.findFirstDefinitionByPossiblyQualifiedNameOrNull(QualifiedName("people.Person"))!!.type())
        }
    }

    /** a -> b, both valid, with access configurations */
    private fun states(): Pair<CompositionEditorState, AccessEditorState> {
        val s = CompositionEditorState(listOf(provider))
        val a = s.addPart(P, "a"); a.datastore.parameter["root"]!!.text = "/a"
        val b = s.addPart(P, "b"); b.datastore.parameter["root"]!!.text = "/b"
        val acc = AccessEditorState(s)
        acc.draftFor(a).parameter["glob"]!!.text = "*"
        acc.draftFor(b).parameter["glob"]!!.text = "*"
        return Pair(s, acc)
    }

    private val noItems: PreviewCallback = { _, _, _, _ -> PreviewResult.Items(emptyList()) }

    @Test
    fun preview_needs_a_callback_and_valid_configurations() {
        val (s, acc) = states()
        val b = s.parts[1]
        assertFalse(acc.canPreview(b, null))
        assertTrue(acc.canPreview(b, noItems))
        acc.draftFor(b).parameter["glob"]!!.text = ""
        assertFalse(acc.canPreview(b, noItems)) // access has errors
        acc.draftFor(b).parameter["glob"]!!.text = "*"
        b.datastore.parameter["root"]!!.text = ""
        assertFalse(acc.canPreview(b, noItems)) // composition has errors
    }

    @Test
    fun preview_is_given_the_configurations_the_part_and_the_limit() {
        val (s, acc) = states()
        var args: List<Any?>? = null
        val cb: PreviewCallback = { c: DatastoreCompositionConfiguration, a: AccessCompositionConfiguration, upTo, limit ->
            args = listOf(c.part.keys.map { it.second.value }, a.part.keys.map { it.value }, upTo, limit)
            PreviewResult.Items(emptyList(), more = true)
        }
        assertEquals(PreviewStatus.Idle, acc.previewStatus(s.parts[1]))
        runNow { acc.preview(s.parts[1], cb, 7) }
        assertEquals(listOf(listOf("a", "b"), listOf("a", "b"), DatastoreIdentity("b"), 7), args)
        assertEquals(PreviewStatus.Done(PreviewResult.Items(emptyList(), more = true)), acc.previewStatus(s.parts[1]))
        assertEquals(PreviewStatus.Idle, acc.previewStatus(s.parts[0]))
    }

    @Test
    fun preview_exception_is_a_failure() {
        val (s, acc) = states()
        runNow { acc.preview(s.parts[0], { _, _, _, _ -> error("cannot connect") }, 10) }
        val r = assertIs<PreviewResult.Failure>(assertIs<PreviewStatus.Done>(acc.previewStatus(s.parts[0])).result)
        assertEquals("cannot connect", r.message)
    }

    @Test
    fun preview_result_is_dropped_when_a_configuration_changes() {
        val (s, acc) = states()
        runNow { acc.preview(s.parts[1], noItems, 10) }
        assertIs<PreviewStatus.Done>(acc.previewStatus(s.parts[1]))
        acc.draftFor(s.parts[0]).parameter["glob"]!!.text = "*.txt"
        assertEquals(PreviewStatus.Idle, acc.previewStatus(s.parts[1]))
    }

    @Test
    fun properties_are_read_one_level() {
        val obj = typedPerson(Person("ann", listOf("x", "y"), Person("bob", emptyList(), null)))
        assertIs<PreviewValue.Structure>(PreviewReader.valueOf(obj))
        val props = runNow { PreviewReader.propertiesOf(obj) }.associate { it.name to it.value }
        assertEquals(listOf("name", "tags", "friend", "broken"), props.keys.toList())
        assertEquals(PreviewValue.Text("\"ann\""), props["name"])
        val tags = assertIs<PreviewValue.Collection>(props["tags"])
        assertEquals(2, tags.size)
        assertEquals(listOf(PreviewEntry("[0]", PreviewValue.Text("\"x\"")), PreviewEntry("[1]", PreviewValue.Text("\"y\""))), PreviewReader.elementsOf(tags.obj))
        assertIs<PreviewValue.Structure>(props["friend"])
        val broken = props["broken"]
        assertTrue(broken is PreviewValue.Error && broken.message.contains("cannot read"), "$broken")
    }

    @Test
    fun null_property_is_shown_as_null() {
        val obj = typedPerson(Person("ann", emptyList(), null))
        val props = runNow { PreviewReader.propertiesOf(obj) }.associate { it.name to it.value }
        assertEquals(PreviewValue.Text("null"), props["friend"])
    }

    @Test
    fun a_data_item_can_be_previewed() {
        val item = DataItemDefault(null, DataItemIdentity("ann.txt"), typedPerson(Person("ann", emptyList(), null)), object : DataItemProvenance {})
        assertEquals(listOf("ann.txt"), item.qualifiedIdentity.map { it.value })
        assertNull((PreviewReader.valueOf(item.data) as? PreviewValue.Error))
    }
}
