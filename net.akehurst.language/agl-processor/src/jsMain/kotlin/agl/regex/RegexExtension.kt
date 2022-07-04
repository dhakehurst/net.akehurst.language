package net.akehurst.language.agl.regex

actual fun String.asRegexLiteral() :Regex {
    val p = this
    val escaped = js("String(p).replace(/([\\.\\\\\\+\\*\\?\\[\\^\\]\\\$\\(\\)\\{\\}])/g,'\\\\$1')")
    return Regex(escaped)
}

//actual fun Regex.matchAtStart(input: String): String? {
//    this.matchAt(input, 0)?.value
//}

/*
actual fun Regex.matchAtStart(input: String): String? {
    val re = Regex("^${this.pattern}")
    val m = re.find(input)
    return if (0==m?.range?.first) {
        m.value
    }else {
        null
    }
}
*/
