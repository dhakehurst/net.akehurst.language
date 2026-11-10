package net.akehurst.datastore.gui.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderSchema
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.common.datastoreProviderSchema
import net.akehurst.datastore.gui.compose.ui.AccessConfigurationEditor
import net.akehurst.datastore.gui.compose.ui.DatastoreCompositionEditor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class test_ReadOnlyEditors {

    private companion object {
        val FS = DatastoreProviderIdentity("test.fs")
        val TXT = DatastoreProviderIdentity("test.txt")

        fun provider(id: DatastoreProviderIdentity, schema: DatastoreProviderSchema) = object : DatastoreProvider {
            override val identity = id
            override val schema = schema
            override fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration): Datastore = error("not used")
        }

        val fsProvider = provider(FS, datastoreProviderSchema {
            datastore {
                parameter("root", "Root", false) { type("String") }
                group("Advanced", advanced = true) {
                    parameter("timeout", "Timeout", true) { type("Integer") }
                }
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
            datastore { parameter("grammar", "Grammar", false) { type("String") } }
            access { }
        })

        val choice = fsProvider.schema.datastore.parameterInfo[2] as DsConfigParameterChoice

        /** fs -> txt, valid, with one connection */
        fun state(): CompositionEditorState {
            val s = CompositionEditorState(listOf(fsProvider, txtProvider), suggestedIdentity = "c1")
            val fs = s.addPart(FS, "fs")
            fs.datastore.parameter["root"]!!.text = "/data"
            fs.datastore.parameter["password"]!!.text = "pw"
            val txt = s.addPart(TXT, "txt")
            txt.datastore.parameter["grammar"]!!.text = "g"
            val c = s.addConnection(fs, txt, "txt.ParsableUnit")
            c.addEnd("owner", "fs.FileHandle", "\$self")
            c.addEnd("text", "String", "name")
            return s
        }
    }

    @Test
    fun editable_composition_editor_has_editing_controls() = runComposeUiTest {
        val s = state()
        setContent { MaterialTheme { DatastoreCompositionEditor(s, Modifier.size(1000.dp, 900.dp)) } }
        onNodeWithText("+ Add part").assertExists()
        onNodeWithText("+ Add connection").assertExists()
        onNodeWithText("1. fs").performClick()
        // the part id, root and password fields can be edited
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun read_only_composition_editor() = runComposeUiTest {
        val s = state()
        var tested = false
        var partAction = 0
        val callbacks = EditorCallbacks(testConnection = { _, _, _ -> tested = true; TestResult.Success() })
        val customisation = EditorCustomisation(partActions = { TextButton(onClick = { partAction++ }) { Text("Show types") } })
        setContent { MaterialTheme { DatastoreCompositionEditor(s, Modifier.size(1000.dp, 900.dp), callbacks, customisation, readOnly = true) } }

        // nothing that changes the chain
        onNodeWithText("+ Add part").assertDoesNotExist()
        onNodeWithText("+ Add connection").assertDoesNotExist()
        onNodeWithText("↑").assertDoesNotExist()
        onNodeWithText("↓").assertDoesNotExist()
        onAllNodesWithText("✕").assertCountEquals(0)

        // a part can be selected; its fields show their values but cannot be edited
        onNodeWithText("1. fs").performClick()
        onNodeWithText("/data").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)

        // the choice cannot be changed
        onNodeWithText("token").performClick()
        assertEquals("password", s.parts[0].datastore.chosenAlternative("2", choice))

        // an advanced group can still be unfolded
        onNodeWithText("Timeout").assertDoesNotExist()
        onNodeWithText("Advanced").performClick()
        onNodeWithText("Timeout").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)

        // the host's part actions and 'Test connection' still work
        onNodeWithText("Show types").performClick()
        assertEquals(1, partAction)
        onNodeWithText("Test connection").assertIsEnabled().performClick()
        waitUntil(5000) { tested }

        // a connection can be selected; its fields cannot be edited and ends cannot be added or removed
        onNodeWithText("↓ parsableUnit : txt.ParsableUnit").performClick()
        onNodeWithText("txt.ParsableUnit").assertExists()
        onNodeWithText("+ Add end").assertDoesNotExist()
        onAllNodesWithText("✕").assertCountEquals(0)
        onAllNodes(hasSetTextAction()).assertCountEquals(0)

        // nothing was changed
        assertEquals(listOf("fs", "txt"), s.parts.map { it.id })
        assertEquals(1, s.connections.size)
        assertEquals(2, s.connections[0].ends.size)
    }

    @Test
    fun read_only_access_editor() = runComposeUiTest {
        val s = state()
        val acc = AccessEditorState(s)
        acc.draftFor(s.parts[0]).parameter["glob"]!!.text = "*.txt"
        var tested = false
        var previewed = false
        val callbacks = EditorCallbacks(
            testAccess = { _, _, _, _ -> tested = true; TestResult.Success() },
            preview = { _, _, _, _ -> previewed = true; PreviewResult.Items(emptyList()) },
        )
        setContent { MaterialTheme { AccessConfigurationEditor(acc, Modifier.size(1000.dp, 900.dp), callbacks, readOnly = true) } }

        onNodeWithText("*.txt").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        onAllNodesWithText("Test access")[0].assertIsEnabled().performClick()
        waitUntil(5000) { tested }
        onAllNodesWithText("Preview")[0].assertIsEnabled().performClick()
        waitUntil(5000) { previewed }
        assertEquals("*.txt", acc.draftFor(s.parts[0]).parameter["glob"]!!.text)
    }

    @Test
    fun editable_access_editor_has_editable_fields() = runComposeUiTest {
        val s = state()
        val acc = AccessEditorState(s)
        setContent { MaterialTheme { AccessConfigurationEditor(acc, Modifier.size(1000.dp, 900.dp)) } }
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
    }
}
