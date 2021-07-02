package net.akehurst.language.api.style

data class AglStyleRule(
        val selector:String
) {
    var styles = mutableMapOf<String,AglStyle>()

    fun getStyle(name:String) : AglStyle? {
        return this.styles[name]
    }

    fun toCss(): String {
        return """
            ${this.selector} {
                ${this.styles.values.joinToString(separator = "\n"){it.toCss()}}
            }
         """.trimIndent()
    }
}

data class AglStyle(
        val name: String,
        val value:String
) {
    fun toCss() = "$name : $value ;"
}