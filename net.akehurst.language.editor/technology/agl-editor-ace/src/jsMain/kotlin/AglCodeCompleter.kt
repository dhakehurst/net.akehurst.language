package net.akehurst.language.editor.ace

import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.editor.common.AglComponents


class AglCodeCompleter(
        val languageId: String,
        val agl: AglComponents
) {

    @JsName("getCompletions")
    fun getCompletions(editor: ace.Editor, session: ace.EditSession, pos: dynamic, prefix: dynamic, callback: dynamic) {
        val posn = session.getDocument().positionToIndex(pos, 0)
        val wordList = this.getCompletionItems(editor, posn)
        val aceCi = wordList.map { ci ->
            object : Any() {
                val caption = ci.text
                val value = ci.text
                val meta = "(${ci.rule.name})"
            }
        }.toTypedArray()
        callback(null, aceCi)
    }

    fun getCompletionItems(editor: ace.Editor, pos: Int): List<CompletionItem> {
        val proc = this.agl.processor
        return if (null != proc) {
            val goalRule = this.agl.goalRule
            if (null == goalRule) {
                val list = proc.expectedAt(editor.getValue(), pos, 1);
                list
            } else {
                val list = proc.expectedAt(goalRule, editor.getValue(), pos, 1);
                list
            }
        } else {
            emptyList()
        }
    }

}