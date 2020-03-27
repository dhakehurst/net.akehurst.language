/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JsModule("monaco-editor/esm/vs/editor/editor.api")
@file:JsNonModule

package monaco

import org.w3c.dom.Element

external interface IEvent<T> {
    //val listener: (e: T) -> Any, thisArg?: any): IDisposable;
}

external interface IDisposable

external interface CancellationToken {
    val isCancellationRequested: Boolean

    /**
     * An event emitted when cancellation is requested
     * @event
     */
    val onCancellationRequested: IEvent<Any>
}

external object MarkerSeverity {
    val Hint: MarkerSeverity = definedExternally
    val Info: MarkerSeverity = definedExternally
    val Warning: MarkerSeverity = definedExternally
    val Error: MarkerSeverity = definedExternally
}

external interface IPosition {
    /**
     * line number (starts at 1)
     */
    val lineNumber: Int;

    /**
     * column (the first character in a line is between column 1 and column 2)
     */
    val column: Int;
}

external interface IRange {
    val endColumn: Int
    val endLineNumber: Int
    val startColumn: Int
    val startLineNumber: Int

}

external class Position(
        lineNumber: Int,
        column: Int
) : IPosition {
    override val lineNumber: Int
    override val column: Int
    //...
}

external object editor {

    fun create(element: Element, options: IStandaloneEditorConstructionOptions?, override: IEditorOverrideServices?): IStandaloneCodeEditor

    fun defineTheme(themeName: String, themeData: IStandaloneThemeData)

    fun setModelMarkers(model: ITextModel, owner: String, markers: Array<IMarkerData>)


    enum class EndOfLinePreference {
        TextDefined,
        LF,
        CRLF
    }

    interface IModelDecorationOptions {
        val afterContentClassName: String?
        val beforeContentClassName: String?
        val className: String?
        val glyphMarginClassName: String?
        val glyphMarginHoverMessage: dynamic  //IMarkdownString | IMarkdownString[] | null
        val hoverMessage: dynamic  //IMarkdownString | IMarkdownString[] | null
        val inlineClassName: String?
        val inlineClassNameAffectsLetterSpacing: Boolean?
        val isWholeLine: Boolean?
        val linesDecorationsClassName: String?
        val marginClassName: String?
        val minimap: dynamic
        val overviewRuler: dynamic
        val stickiness: dynamic
        val zindex: dynamic
    }

    interface IModelDeltaDecoration {
        val range: IRange
        val options: IModelDecorationOptions
    }

    interface IEditor {
        fun layout(dimension: IDimension? = definedExternally)
    }

    interface ICodeEditor : IEditor {
        fun getModel(): ITextModel

        fun onDidChangeModelContent(listener: (IModelContentChangedEvent) -> Unit): IDisposable
        fun deltaDecorations(oldDecorations: Array<String>, newDecorations: Array<IModelDeltaDecoration>) : Array<String>
        fun getLineDecorations(lineNum: Int): dynamic
    }

    interface IStandaloneCodeEditor : ICodeEditor

    interface IStandaloneEditorConstructionOptions

    interface IEditorOverrideServices

    interface IStandaloneThemeData {
        val base: Any
        val inherit: Boolean;
        val rules: Array<ITokenThemeRule>
        //val encodedTokensColors: Array<String>?
        //val colors: IColors
    }

    interface IModelContentChangedEvent

    interface IDimension

    interface ITextModel {
        fun getValue(eol: EndOfLinePreference? = definedExternally, preserveBOM: Boolean? = definedExternally): String
        fun setValue(newValue: String)

        fun getOffsetAt(position: IPosition): Int

        fun resetTokenization()
    }

    interface ITokenThemeRule {
        val token: String
        val foreground: String?
        val background: String?
        val fontStyle: String?
    }

    interface IMarkerData {
        val code: String?
        val severity: MarkerSeverity;
        val message: String
        val source: String?
        val startLineNumber: Int
        val startColumn: Int
        val endLineNumber: Int
        val endColumn: Int
        //val relatedInformation: Array<IRelatedInformation>?
        //val tags: Array<MarkerTag>?
    }
}

external object languages {
    fun register(language: ILanguageExtensionPoint)
    fun setTokensProvider(languageId: String, provider: TokensProvider): IDisposable;
    fun registerCompletionItemProvider(languageId: String, provider: CompletionItemProvider): IDisposable;


    interface ILanguageExtensionPoint {
        val id: String
    }

    interface TokensProvider {
        fun getInitialState(): IState
        fun tokenize(line: String, state: IState): ILineTokens
    }

    interface IState {
        fun clone(): IState;
        override fun equals(other: Any?): Boolean;
    }

    interface ILineTokens {
        @JsName("tokens")
        val tokens: Array<IToken>

        @JsName("endState")
        val endState: IState;
    }

    interface IToken {
        @JsName("startIndex")
        val startIndex: Number

        @JsName("scopes")
        val scopes: String;
    }

    interface CompletionItemProvider {
        val triggerCharacters: Array<String>?

        /**
         * Provide completion items for the given position and document.
         */
        fun provideCompletionItems(model: editor.ITextModel, position: Position, context: CompletionContext, token: CancellationToken): CompletionList?

        /**
         * Given a completion item fill in more data, like [doc-comment](#CompletionItem.documentation)
         * or [details](#CompletionItem.detail).
         *
         * The editor will only resolve a completion item once.
         */
        fun resolveCompletionItem(model: editor.ITextModel, position: Position, item: CompletionItem, token: CancellationToken): CompletionList?
    }

    interface CompletionList {
        val suggestions: Array<CompletionItem>
        val incomplete: Boolean
        //fun dispose()
    }

    interface CompletionContext {
        /**
         * How the completion was triggered.
         */
        val triggerKind: CompletionTriggerKind

        /**
         * Character that triggered the completion item provider.
         *
         * `undefined` if provider was not triggered by a character.
         */
        val triggerCharacter: String?
    }

    enum class CompletionTriggerKind {
        Invoke, TriggerCharacter, TriggerForIncompleteCompletions
    }

    enum class CompletionItemKind {
        Method, Function, Constructor, Field, Variable, Class,
        Struct, Interface, Module, Property, Event, Operator,
        Unit, Value, Constant, Enum, EnumMember,
        Keyword, Text, Color, File, Reference,
        Customcolor, Folder, TypeParameter, Snippet
    }


    /**
     * A completion item represents a text snippet that is
     * proposed to complete text that is being typed.
     */
    interface CompletionItem {
        /**
         * The label of this completion item. By default
         * this is also the text that is inserted when selecting
         * this completion.
         */
        val label: String

        /**
         * The kind of this completion item. Based on the kind
         * an icon is chosen by the editor.
         */
        val kind: CompletionItemKind

        /**
         * A string or snippet that should be inserted in a document when selecting
         * this completion.
         * is used.
         */
        val insertText: String

        /**
         * A modifier to the `kind` which affect how the item
         * is rendered, e.g. Deprecated is rendered with a strikeout
         */
        //val tags: Array<CompletionItemTag>?
        /**
         * A human-readable string with additional information
         * about this item, like type or symbol information.
         */
        //val detail: String?
        /**
         * A human-readable string that represents a doc-comment.
         */
        //val documentation: String?
        /**
         * A string that should be used when comparing this item
         * with other items. When `falsy` the [label](#CompletionItem.label)
         * is used.
         */
        //val sortText: String?
        /**
         * A string that should be used when filtering a set of
         * completion items. When `falsy` the [label](#CompletionItem.label)
         * is used.
         */
        //val filterText: String?
        /**
         * Select this item when showing. *Note* that only one completion item can be selected and
         * that the editor decides which item that is. The rule is that the *first* item of those
         * that match best is selected.
         */
        //val preselect: Boolean?
        /**
         * Addition rules (as bitmask) that should be applied when inserting
         * this completion.
         */
        //val insertTextRules: CompletionItemInsertTextRule?
        /**
         * A range of text that should be replaced by this completion item.
         *
         * Defaults to a range from the start of the [current word](#TextDocument.getWordRangeAtPosition) to the
         * current position.
         *
         * *Note:* The range must be a [single line](#Range.isSingleLine) and it must
         * [contain](#Range.contains) the position at which completion has been [requested](#CompletionItemProvider.provideCompletionItems).
         */
        //val range: IRange
        /**
         * An optional set of characters that when pressed while this completion is active will accept it first and
         * then type that character. *Note* that all commit characters should have `length=1` and that superfluous
         * characters will be ignored.
         */
        //val commitCharacters: Array<string>?
        /**
         * An optional array of additional text edits that are applied when
         * selecting this completion. Edits must not overlap with the main edit
         * nor with themselves.
         */
        //val additionalTextEdits: Array<editor.ISingleEditOperation>?
        /**
         * A command that should be run upon acceptance of this item.
         */
        //val command: Command?
    }
}