package net.akehurst.language.agl.regex

actual fun Regex.matchAtStart(input: String): String? {
    val re = Regex("^${this.pattern}")
    val m = re.find(input)
    return if (0==m?.range?.first) {
        m.value
    }else {
        null
    }
}

