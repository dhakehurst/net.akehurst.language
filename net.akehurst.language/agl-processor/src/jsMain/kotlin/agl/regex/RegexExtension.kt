package net.akehurst.language.agl.regex

// see https://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript/63838890#63838890
actual fun String.asRegexLiteral() :Regex {
    val p = this
    //val special = ".*+-?^\${}()|[]\"
    val escaped = js("String(p).replace(/[\\.\\*\\-\\?\\^\\\$\\{\\}\\(\\)\\|\\[\\]\\\\]/g,'\\\\$1')")
    return Regex(escaped)
}
