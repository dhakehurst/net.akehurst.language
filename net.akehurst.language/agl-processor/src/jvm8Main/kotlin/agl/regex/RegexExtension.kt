package net.akehurst.language.agl.regex

actual fun Regex.matchAtStart(input:String) : String? {
    val m = this.toPattern().matcher(input)
    return if (m.lookingAt()) {
        input.substring(0,m.end())
    } else {
        null
    }
}

