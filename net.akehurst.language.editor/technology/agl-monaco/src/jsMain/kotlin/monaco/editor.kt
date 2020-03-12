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

external interface IDisposable

external object MarkerSeverity {
    val Hint:MarkerSeverity = definedExternally
    val Info:MarkerSeverity = definedExternally
    val Warning:MarkerSeverity = definedExternally
    val Error:MarkerSeverity = definedExternally
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

    interface IEditor {
        fun layout(dimension: IDimension? = definedExternally)
    }

    interface ICodeEditor : IEditor {
        fun getModel(): ITextModel

        fun onDidChangeModelContent(listener: (IModelContentChangedEvent)-> Unit): IDisposable
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
        fun getValue(eol: EndOfLinePreference?= definedExternally, preserveBOM: Boolean?= definedExternally): String
        fun setValue(newValue: String)
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
}