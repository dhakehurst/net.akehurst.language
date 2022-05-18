package net.akehurst.language.agl.util

fun debug(indentDelta: Debug.IndentDelta, lazyMessage:()->String) = Debug.debug(indentDelta, lazyMessage)

object Debug {
    enum class IndentDelta{NONE, INC_BEFORE, INC_AFTER, DEC_BEFORE, DEC_AFTER}

    const val CHECK = false
    const val OUTPUT_BUILD = true
    const val OUTPUT_RUNTIME = false

    val indentDeltaStr = "  "
    var currentIndent = ""

    fun debug(indentDelta:IndentDelta,lazyMessage:()->String) {
        if (OUTPUT_BUILD || OUTPUT_RUNTIME) {
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
