package net.akehurst.language.agl.regex

actual fun String.asRegexLiteral() :Regex {
    val escaped = this.replace("\\W","\\\\$&")
    return Regex("\\Q${this}\\E")
}

