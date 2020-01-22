package net.akehurst.language.api.api.style

data class AglStyleRule(
        val selector:String
) {
    var styles = listOf<AglStyle>()
}

data class AglStyle(
        val name: String,
        val value:String
) {
    fun toCss() = "$name : $value ;"
}