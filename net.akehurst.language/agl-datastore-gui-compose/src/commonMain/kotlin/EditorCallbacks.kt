package net.akehurst.datastore.gui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.language.types.api.TypesDomain

/** The result of a test run by the host application. */
sealed interface TestResult {
    data class Success(val message: String? = null) : TestResult
    data class Failure(val message: String, val cause: Throwable? = null) : TestResult
}

/**
 * The behaviour the host application gives the editor. The editor is UI only: it never creates, opens or
 * reads datastores itself. A button whose callback is null is shown disabled.
 */
class EditorCallbacks(
    /** tests that the datastore of a part can be connected to, e.g. by providing and opening it */
    val testConnection: (suspend (partId: DatastoreIdentity, provider: DatastoreProviderIdentity, datastore: DatastoreConfiguration) -> TestResult)? = null,
    /** tests a part's access configuration, e.g. by reading with it */
    val testAccess: (suspend (partId: DatastoreIdentity, provider: DatastoreProviderIdentity, datastore: DatastoreConfiguration, access: DatastoreConfiguration) -> TestResult)? = null,
    /** the types of a part with that datastore configuration, for the connection editor's type lists and checks; null if not known */
    val typesFor: (suspend (partId: DatastoreIdentity, provider: DatastoreProviderIdentity, datastore: DatastoreConfiguration) -> TypesDomain?)? = null,
)

/** How a provider is shown in the editor. */
class ProviderPresentation(
    val displayName: String,
    val description: String = "",
    val icon: (@Composable () -> Unit)? = null,
)

/** What a custom parameter renderer is given. */
class ParameterFieldContext(
    val provider: DatastoreProviderIdentity,
    val definition: DsConfigParameterDefinition,
    val draft: ParameterDraft,
    /** the error for this parameter, if any */
    val error: String?,
    val strings: EditorStrings,
)

/**
 * Renders the field of a parameter instead of the default field. Return null from [rendererFor] to use the default.
 * Example: a code editor for the grammar parameter of a text datastore.
 */
fun interface ParameterRenderer {
    fun rendererFor(provider: DatastoreProviderIdentity, definition: DsConfigParameterDefinition): (@Composable (ParameterFieldContext) -> Unit)?
}

/** The labels used by the editor; replace them to translate the editor. Issue messages are not included. */
data class EditorStrings(
    val composition: String = "Composition",
    val chain: String = "Chain",
    val addPart: String = "Add part",
    val add: String = "Add",
    val cancel: String = "Cancel",
    val remove: String = "Remove",
    val moveUp: String = "Move up",
    val moveDown: String = "Move down",
    val partId: String = "Part id",
    val provider: String = "Provider",
    val datastoreConfiguration: String = "Datastore configuration",
    val accessConfiguration: String = "Access configuration",
    val connection: String = "Connection",
    val addConnection: String = "Add connection",
    val connectionName: String = "Name (optional)",
    val targetType: String = "Target type",
    val ends: String = "Ends",
    val addEnd: String = "Add end",
    val endName: String = "Name",
    val endTypeName: String = "Type",
    val endExpression: String = "Expression",
    val required: String = "required",
    val value: String = "Value",
    val secretReference: String = "Secret reference",
    val secretSource: String = "Secret source",
    val secretKey: String = "Key",
    val show: String = "Show",
    val hide: String = "Hide",
    val notSet: String = "Not set",
    val clear: String = "Clear",
    val testConnection: String = "Test connection",
    val testAccess: String = "Test access",
    val testing: String = "Testing…",
    val testPassed: String = "Test passed",
    val testFailed: String = "Test failed",
    val issues: String = "Issues",
    val noIssues: String = "No issues",
    val help: String = "Help",
    val choose: String = "Choose",
    val nothingSelected: String = "Select a part or a connection",
)

/** Changes how the editor looks, without changing this module. */
class EditorCustomisation(
    /** tried in order; the first that gives a renderer is used */
    val parameterRenderers: List<ParameterRenderer> = emptyList(),
    val providerPresentation: (DatastoreProviderIdentity) -> ProviderPresentation = { ProviderPresentation(it.value) },
    val strings: EditorStrings = EditorStrings(),
    /** the host's own buttons (e.g. save, load, run), shown in the editor's toolbar */
    val toolbarActions: @Composable RowScope.() -> Unit = {},
) {
    companion object {
        val Default = EditorCustomisation()
    }

    fun rendererFor(provider: DatastoreProviderIdentity, definition: DsConfigParameterDefinition): (@Composable (ParameterFieldContext) -> Unit)? =
        parameterRenderers.firstNotNullOfOrNull { it.rendererFor(provider, definition) }
}
