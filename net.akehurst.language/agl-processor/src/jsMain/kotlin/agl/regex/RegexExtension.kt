package net.akehurst.language.agl.regex

actual fun Regex.matchAtStart(input: String): String? {
    val re = Regex("^${this.pattern}")
    val m = re.find(input)
    return m?.value
}

