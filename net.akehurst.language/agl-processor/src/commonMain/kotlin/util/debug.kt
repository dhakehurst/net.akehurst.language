package net.akehurst.language.agl.util

fun debug(indentDelta: Debug.IndentDelta, lazyMessage:()->String) = Debug.debug(indentDelta, lazyMessage)

object Debug {
    enum class IndentDelta{NONE, INC_BEFORE, INC_AFTER, DEC_BEFORE, DEC_AFTER}

    const val CHECK = false
    const val OUTPUT_SM_BUILD = false
    const val OUTPUT_RUNTIME_BUILD = false
    const val OUTPUT_RUNTIME = true

    val indentDeltaStr = "  "
    var currentIndent = ""

    fun debug(indentDelta:IndentDelta,lazyMessage:()->String) {
        if (OUTPUT_SM_BUILD || OUTPUT_RUNTIME || OUTPUT_RUNTIME_BUILD) {
            when(indentDelta){
                IndentDelta.DEC_BEFORE -> currentIndent = currentIndent.substring(indentDeltaStr.length)
                IndentDelta.INC_BEFORE -> currentIndent += indentDeltaStr
                else -> Unit
            }
            println("${currentIndent}${lazyMessage.invoke()}")
            when(indentDelta){
                IndentDelta.DEC_AFTER -> currentIndent = currentIndent.substring(indentDeltaStr.length)
                IndentDelta.INC_AFTER -> currentIndent += indentDeltaStr
                else -> Unit
            }
        }
    }
}
