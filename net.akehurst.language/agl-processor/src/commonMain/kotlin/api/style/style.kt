package net.akehurst.language.api.api.style

import kotlin.js.JsName

data class AglStyleRule(
        val selector:String
) {
    var styles = mutableMapOf<String,AglStyle>()

    @JsName("getStyle")
    fun getStyle(name:String) : AglStyle? {
        return this.styles[name]
    }
}

data class AglStyle(
        val name: String,
        val value:String
) {
    fun toCss() = "$name : $value ;"
}