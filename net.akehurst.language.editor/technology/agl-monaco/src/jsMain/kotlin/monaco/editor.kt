@file:JsModule("monaco-editor/esm/vs/editor/editor.api")
@file:JsNonModule

package monaco

import org.w3c.dom.Element

external object editor {

    fun create(element: Element, options: IStandaloneEditorConstructionOptions?, override: IEditorOverrideServices?): IStandaloneCodeEditor

    fun defineTheme(themeName: String, themeData: IStandaloneThemeData)


    interface IStandaloneCodeEditor

    interface IStandaloneEditorConstructionOptions

    interface IEditorOverrideServices

    interface IStandaloneThemeData
}