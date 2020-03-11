import kotlin.browser.*

import net.akehurst.language.editor.technology.tabview.TabView
import net.akehurst.language.editor.monaco.AglEditorMonaco

fun main() {
    TabView.initialise(document)
    val editors = AglEditorMonaco.initialise(document)
}