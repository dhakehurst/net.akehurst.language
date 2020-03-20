package net.akehurst.language.editor.desktop.jfx.tornado

import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.text.TextFlow
import javafx.stage.Popup
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.event.MouseOverTextEvent
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import tornadofx.label
import tornadofx.vbox
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set


//TODO: move this into its own lib eventually, here whilst we work on it

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
                styleMap[sel] = rule.styles.values.joinToString("") { it.toCss() }
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
            /*
            val inputText = this.text
            val position = this.caretPosition
            val expected = this.processor.expectedAt(inputText, position, 1)

            //val popup = contextMenu {
            //    item {
            //        label("aaa")
            //    }
            //}

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

             */
        } catch (t:Throwable) {
            t.printStackTrace()
        }
    }
}
