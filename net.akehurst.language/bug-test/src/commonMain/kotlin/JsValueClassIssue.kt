package bugs

import kotlin.jvm.JvmInline

@JvmInline
value class VC(val value:String) {
    companion object {
        val String.asVC2companion get() = VC(this)
    }
}
val String.asVC2topFun get() = VC(this)