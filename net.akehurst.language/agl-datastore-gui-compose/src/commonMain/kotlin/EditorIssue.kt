package net.akehurst.datastore.gui.compose

enum class IssueSeverity { ERROR, WARNING }

/** Where in the editor an issue is, so it can be shown next to the field and selected from the issue list. */
sealed interface IssueLocation {
    /** the composition as a whole */
    data object Composition : IssueLocation

    /** a part, or one of its parameters when 'parameterIdentity' is given */
    data class Part(val part: PartDraft, val parameterIdentity: String? = null) : IssueLocation

    /** the id of a part */
    data class PartId(val part: PartDraft) : IssueLocation

    /** a connection, or one of its fields */
    data class Connection(val connection: ConnectionDraft, val field: ConnectionField? = null, val end: EndDraft? = null) : IssueLocation

    /** the access configuration of a part, or one of its parameters */
    data class Access(val part: PartDraft, val parameterIdentity: String? = null) : IssueLocation
}

enum class ConnectionField { NAME, TARGET_TYPE, END_NAME, END_TYPE_NAME, END_EXPRESSION }

data class EditorIssue(
    val severity: IssueSeverity,
    val location: IssueLocation,
    val message: String,
)
