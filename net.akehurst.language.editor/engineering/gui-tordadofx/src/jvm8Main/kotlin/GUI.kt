package net.akehurst.language.editor.gui.tornadofx

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.text.TextFlow
import javafx.stage.Popup
import javafx.stage.Stage
import kotlinx.coroutines.delay
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.realisation.afActive
import net.akehurst.language.api.api.style.AglStyleRule
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.fxmisc.richtext.*
import org.fxmisc.richtext.event.MouseOverTextEvent
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.fxmisc.richtext.model.TwoDimensional
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import tornadofx.*
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors


class MyApp() : App(MyView::class) {
}

class MyView : View() {
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

class StyleObject(
        val name: String,
        val css: String
)

class AGLEditorComponent(
        var processor: LanguageProcessor
) : StyledTextArea<String, StyleObject>(
        "",
        { text: TextFlow, style: String -> text.style = style },
        StyleObject("",""),
        { text: TextExt, style: StyleObject -> text.style = style.css }
) {
    var tabIndent = "  "

    val executor = Executors.newSingleThreadExecutor()

    val styleMap = mutableMapOf<String, String>()

    init {
        this.paragraphGraphicFactory = LineNumberFactory.get(this)
        cfgTabIndent()
        cfgHover()
        cfgAutoComplete()

        //this.textProperty().addListener { observable, oldValue, newValue ->
        //    this.setStyleSpans(0, computeHighlighting(newValue));
        //}

        val cleanupWhenDone = this.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(this.multiPlainChanges())
                .filterMap { t ->
                    if (t.isSuccess()) {
                        Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        Optional.empty();
                    }
                }
                .subscribe(this::applyHighlighting);
    }

    fun setStyleSheetFromString(styleRules: String) {
        styleMap.clear()
        try {
            val rules = Agl.styleProcessor.process<List<AglStyleRule>>("rules", styleRules)
            rules.forEach { rule ->
                val sel = rule.selector
                styleMap[sel] = rule.styles.joinToString("") { it.toCss() }
            }
        } catch (t:Throwable) {

        }
    }

    fun cfgTabIndent() {
        val im = InputMap.consume(EventPattern.keyPressed(KeyCode.TAB)) { this.replaceSelection(tabIndent) }
        Nodes.addInputMap(this, im);
    }

    fun cfgHover() {
        val popup = Popup()
        val popupMsg = Label()
        popupMsg.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5;")
        popup.getContent().add(popupMsg)

        this.setMouseOverTextDelay(Duration.ofSeconds(1))
        this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN) { e ->
            val chIdx = e.getCharacterIndex()
            val pos = e.getScreenPosition()
            popupMsg.setText("Style: '" + this.getStyleAtPosition(chIdx).name + "' at " + pos);
            popup.show(this, pos.getX(), pos.getY() + 10);
        }
        this.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END) {
            popup.hide()
        }
    }

    fun cfgAutoComplete() {
        Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), { event -> codeComplete(event)}));
    }

    private fun computeHighlightingAsync(): Task<StyleSpans<StyleObject>> {
        val text = this.getText();
        val task = object : Task<StyleSpans<StyleObject>>() {
            override fun call(): StyleSpans<StyleObject> {
                return computeHighlighting(text);
            }
        }
        executor.execute(task);
        return task;
    }

    private fun applyHighlighting(highlighting: StyleSpans<StyleObject>) {
        this.setStyleSpans(0, highlighting)
    }

    private fun computeHighlighting(text: String): StyleSpans<StyleObject> {
        val ssb = StyleSpansBuilder<StyleObject>()
        var pos = 0
        processor.scan(text).map {
            if (it.startPosition != pos) {
                val gapLength = it.startPosition - pos
                ssb.add(StyleObject("",""), gapLength)
                pos += gapLength
            }
            val sel = if(it.isPattern) "\"${it.name}\"" else "'${it.name}'"
            val style = this.styleMap[sel]
            val length = it.matchedTextLength
            if (null == style) {
                ssb.add(StyleObject("", ""), length)
            } else {
                ssb.add(StyleObject(it.name, style), length)
            }
            pos += length
        }
        ssb.add(StyleObject("", ""), text.length - pos)
        return ssb.create()
    }

    fun codeComplete(event: KeyEvent) {
        try {
            val inputText = this.text
            val position = this.caretPosition
            val expected = this.processor.expectedAt(inputText, position, 1)

            val popup = Popup()

            val content = vbox {
                label("aaa")
                label("bbbbb")
                label("ccc")
            }
            content.setStyle("""
                -fx-background-color: black;
                -fx-text-fill: white;
                -fx-padding: 5;
            """.trimIndent())
            popup.content.add(content)
            val m = InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), { event ->
                this.insertText(position, "'selected text'")
            })
            Nodes.addInputMap(content, m);

            val pos = this.caretBounds.get()
            popup.show(this, pos.centerX, pos.centerY + 10);
        } catch (t:Throwable) {
            t.printStackTrace()
        }
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