package net.akehurst.language.editor.gui.tornadofx

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.stage.Popup
import javafx.stage.Stage
import kotlinx.coroutines.delay
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.realisation.afActive
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.event.MouseOverTextEvent
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import tornadofx.*
import java.time.Duration

class MyApp() : App(MyView::class) {
}

class MyView : View() {
    val grammarArea = AGLEditorComponent(Agl.grammarProcessor)
    val styleArea = AGLEditorComponent(Agl.styleProcessor)
    val formatArea = AGLEditorComponent(Agl.formatProcessor)
    val expressionArea = CodeArea()
    val document = expressionArea.document
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
    }
}

class AGLEditorComponent(
        val processor: LanguageProcessor
) : CodeArea() {
    var tabIndent = "  "

    init {
        this.paragraphGraphicFactory = LineNumberFactory.get(this)
        cfgTabIndent()
        cfgHover()

        this.textProperty().addListener { observable, oldValue, newValue ->
            this.setStyleSpans(0, computeHighlighting(newValue));
        }

        val cleanupWhenDone = this.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
            if(t.isSuccess()) {
                return Optional.of(t.get());
            } else {
                t.getFailure().printStackTrace();
                return Optional.empty();
            }
        })
        .subscribe(this::applyHighlighting);

    }

    fun cfgTabIndent() {
        val im = InputMap.consume(EventPattern.keyPressed(KeyCode.TAB)) { this.replaceSelection(tabIndent) }
        Nodes.addInputMap(this, im);
    }

    fun cfgHover() {
        val popup = Popup();
        val popupMsg = Label();
        popupMsg.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5;");
        popup.getContent().add(popupMsg);

        this.setMouseOverTextDelay(Duration.ofSeconds(1));
        this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN) { e ->
            val chIdx = e.getCharacterIndex()
            val pos = e.getScreenPosition()
            popupMsg.setText("Style: '" + this.getStyleAtPosition(chIdx) + "' at " + pos);
            popup.show(this, pos.getX(), pos.getY() + 10);
        };
        this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END) {
            popup.hide()
        };
    }

    fun computeHighlightingAsync(text: String): StyleSpans<Collection<String>> {
        val ssb = StyleSpansBuilder<Collection<String>>()
        processor.scan(text).map {
            val styles = listOf(it.name)
            val length = it.matchedTextLength
            ssb.add(styles, length)
        }
        return ssb.create()
    }
}

class GUI : Active {

    val expression by stringProperty("// an expression")
    val grammar by stringProperty("// the grammar")
    val style by stringProperty("// style definitions")
    val format by stringProperty("// format definitions")

    var stopped = false

    override val af = afActive {
        execute = {
            val l = object : PlatformImpl.FinishListener {
                override fun exitCalled() {
                    stopped = true
                }

                override fun idle(implicitExit: Boolean) {
                    stopped = true
                }
            }
            PlatformImpl.addListener(l)
            Platform.startup {
                val stage = Stage()
                stage.title = "AGL Editor"
                stage.setOnCloseRequest { we ->
                    stopped = true
                }
                val app = MyApp()
                app.start(stage)
            }

            while (stopped.not()) {
                delay(1000)
            }
        }
        finalise = {
            Platform.exit()
        }
    }


}