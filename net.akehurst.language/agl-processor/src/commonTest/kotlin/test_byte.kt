import kotlin.test.Test
import kotlin.test.assertEquals

class test_byte {

    @Test
    fun byte() {
        val b = 1.toByte()
        assertEquals("Byte", b::class.simpleName) // pass

        val x:Any = b
        assertEquals("Byte", x::class.simpleName)
    }
}