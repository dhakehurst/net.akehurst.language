package net.akehurst.language.editor.desktop.jfx.tornado

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.fxmisc.richtext.LineNumberFactory
import tornadofx.*
import java.io.File

fun main(args: Array<String>) {
    println("PWD: " + File(".").absolutePath)
    launch<EditorApplication>(args)
}


class EditorApplication() : App(EditorView::class) {
}

class EditorView : View() {
    val grammarArea = AGLEditorComponent(Agl.grammarProcessor)
    val styleArea = AGLEditorComponent(Agl.styleProcessor)
    val formatArea = AGLEditorComponent(Agl.formatProcessor)

    val expressionArea = AGLEditorComponent(createProcessor(grammarArea.text))

    override val root = borderpane {
        top {
            menubar {
                val menubar = this
                menu("File") {
                    item("New")
                    item("Open...")
                    item("Save")
                    item("Quit")
                }
                menu("Edit") {
                    item("Copy")
                    item("Paste")
                }
                menu("About") {
                    checkmenuitem(name = "Use System Menu Bar", selected = menubar.useSystemMenuBarProperty()) {
                        //menubar.setUseSystemMenuBar(menubar.isUseSystemMenuBar.not())
                    }
                }
            }
            center {
                tabpane {
                    tab("Expression") {
                        splitpane() {
                            opcr(this, expressionArea)
                            treeview<Any> { }
                        }
                    }
                    tab("Language") {
                        tabpane {
                            tab("grammar") {
                                opcr(this, grammarArea)
                            }
                            tab("style") {
                                opcr(this, styleArea)
                            }
                            tab("format") {
                                opcr(this, formatArea)
                            }
                        }

                    }
                }

            }
        }
    }

    init {
        expressionArea.paragraphGraphicFactory = LineNumberFactory.get(expressionArea)
        grammarArea.textProperty().addListener { observable, oldValue, newValue ->
            expressionArea.processor = createProcessor(newValue)
        }
        styleArea.textProperty().addListener { observable, oldValue, newValue ->
            expressionArea.setStyleSheetFromString(newValue)
        }

        grammarArea.setStyleSheetFromString("""
                'namespace' {
                    -fx-font-size: 12pt;
                    -fx-font-family: "Courier New";
                    -fx-fill: darkgreen;
                    -fx-font-weight: bold;
                }
                'grammar' {
                    -fx-font-size: 12pt;
                    -fx-font-family: "Courier New";
                    -fx-fill: darkgreen;
                    -fx-font-weight: bold;
                }
            """.trimIndent())

        styleArea.appendText("""
                'class' {
                    -fx-font-size: 14pt;
                    -fx-font-family: "Courier New";
                    -fx-fill: purple;
                    -fx-font-weight: bold;
                }
                "[A-Za-z_][A-Za-z0-9_]*" {
                    -fx-font-size: 14pt;
                    -fx-font-family: "Courier New";
                    -fx-fill: red;
                    -fx-font-style: italic;
                }
                '{' {
                    -fx-fill: darkgreen;
                }
                '}' {
                    -fx-fill: darkgreen;
                }
            """.trimIndent())
        grammarArea.appendText("""
            namespace test
            
            grammar Test {
              declaration = 'class' ID '{' '}' ;
            
              ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            
            }
        """.trimIndent())

        expressionArea.appendText("""
            class XXX {
            
            }
        """.trimIndent())
    }

    fun createProcessor(grammarStr: String): LanguageProcessor {
        try {
            return Agl.processor(grammarStr) //TODO: don't want this hard coded!
        } catch (t: Throwable) {
            //if can't create processor, must return a valid processor
            t.printStackTrace()
            return Agl.processor("namespace empty grammar Empty { skip WS = \"\\s+\" ; }", "WS")
        }
    }
}

