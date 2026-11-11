package net.akehurst.datastore.generic.zip

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.Deflate

/**
 * Writes small zip archives for tests, with control over the parts that the tests need to get wrong on purpose
 * (flags, declared sizes, name encoding, ZIP64 records). Fictional data only.
 */
class TestZipBuilder {

    class Entry(
        val nameBytes: ByteArray,
        val content: ByteArray,
        val deflate: Boolean,
        val flags: Int,
        val method: Int?,
        val declaredSize: Long?,
        val unixTime: Int?,
        val dosTime: Int,
        val dosDate: Int,
    )

    private val entries = mutableListOf<Entry>()
    var zip64 = false

    fun file(
        name: String,
        content: String,
        deflate: Boolean = true,
        flags: Int = 0x0800, // UTF-8 names
        method: Int? = null,
        declaredSize: Long? = null,
        unixTime: Int? = null,
    ) = bytes(name.encodeToByteArray(), content.encodeToByteArray(), deflate, flags, method, declaredSize, unixTime)

    fun binary(name: String, content: ByteArray, deflate: Boolean = true) =
        bytes(name.encodeToByteArray(), content, deflate, 0x0800, null, null, null)

    fun directory(name: String) = bytes(name.encodeToByteArray(), ByteArray(0), false, 0x0800, null, null, null)

    /** an entry whose name is CP437 encoded, without the UTF-8 flag */
    fun cp437File(name: String, content: String) = bytes(Cp437.encode(name), content.encodeToByteArray(), true, 0, null, null, null)

    fun bytes(
        nameBytes: ByteArray, content: ByteArray, deflate: Boolean, flags: Int, method: Int?, declaredSize: Long?, unixTime: Int?,
        dosTime: Int = (10 shl 11) or (30 shl 5) or (20 / 2), // 10:30:20
        dosDate: Int = ((2024 - 1980) shl 9) or (5 shl 5) or 17, // 2024-05-17
    ) {
        entries.add(Entry(nameBytes, content, deflate, flags, method, declaredSize, unixTime, dosTime, dosDate))
    }

    fun build(): ByteArray {
        val out = Out()
        val central = Out()
        for (e in entries) {
            val data = if (e.deflate) e.content.compress(Deflate) else e.content
            val method = e.method ?: if (e.deflate) 8 else 0
            val crc = Crc32.of(e.content, 0, e.content.size)
            val size = e.declaredSize ?: e.content.size.toLong()
            val offset = out.size.toLong()
            val extTs = e.unixTime?.let { t -> Out().apply { u16(0x5455); u16(5); u8(1); u32(t.toLong() and 0xFFFFFFFFL) }.bytes() } ?: ByteArray(0)
            val localZip64 = if (zip64) Out().apply { u16(0x0001); u16(16); u64(size); u64(data.size.toLong()) }.bytes() else ByteArray(0)
            // local header
            out.u32(0x04034b50); out.u16(if (zip64) 45 else 20); out.u16(e.flags); out.u16(method)
            out.u16(e.dosTime); out.u16(e.dosDate); out.u32(crc)
            out.u32(if (zip64) 0xFFFFFFFFL else data.size.toLong()); out.u32(if (zip64) 0xFFFFFFFFL else size)
            out.u16(e.nameBytes.size); out.u16(localZip64.size + extTs.size)
            out.bytes(e.nameBytes); out.bytes(localZip64); out.bytes(extTs); out.bytes(data)
            // central record
            val centralZip64 = if (zip64) Out().apply { u16(0x0001); u16(24); u64(size); u64(data.size.toLong()); u64(offset) }.bytes() else ByteArray(0)
            central.u32(0x02014b50); central.u16(if (zip64) 45 else 20); central.u16(if (zip64) 45 else 20)
            central.u16(e.flags); central.u16(method); central.u16(e.dosTime); central.u16(e.dosDate); central.u32(crc)
            central.u32(if (zip64) 0xFFFFFFFFL else data.size.toLong()); central.u32(if (zip64) 0xFFFFFFFFL else size)
            central.u16(e.nameBytes.size); central.u16(centralZip64.size + extTs.size); central.u16(0)
            central.u16(0); central.u16(0); central.u32(0)
            central.u32(if (zip64) 0xFFFFFFFFL else offset)
            central.bytes(e.nameBytes); central.bytes(centralZip64); central.bytes(extTs)
        }
        val cdOffset = out.size.toLong()
        val cd = central.bytes()
        out.bytes(cd)
        if (zip64) {
            val z64Offset = out.size.toLong()
            out.u32(0x06064b50); out.u64(44); out.u16(45); out.u16(45); out.u32(0); out.u32(0)
            out.u64(entries.size.toLong()); out.u64(entries.size.toLong()); out.u64(cd.size.toLong()); out.u64(cdOffset)
            out.u32(0x07064b50); out.u32(0); out.u64(z64Offset); out.u32(1)
        }
        out.u32(0x06054b50); out.u16(0); out.u16(0)
        out.u16(if (zip64) 0xFFFF else entries.size); out.u16(if (zip64) 0xFFFF else entries.size)
        out.u32(if (zip64) 0xFFFFFFFFL else cd.size.toLong()); out.u32(if (zip64) 0xFFFFFFFFL else cdOffset)
        out.u16(0)
        return out.bytes()
    }

    private class Out {
        private var buf = ByteArray(1024)
        var size = 0
            private set

        private fun ensure(n: Int) {
            if (size + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, size + n))
        }

        fun u8(v: Int) { ensure(1); buf[size++] = v.toByte() }
        fun u16(v: Int) { u8(v and 0xFF); u8((v shr 8) and 0xFF) }
        fun u32(v: Long) { u16((v and 0xFFFF).toInt()); u16(((v shr 16) and 0xFFFF).toInt()) }
        fun u64(v: Long) { u32(v and 0xFFFFFFFFL); u32((v ushr 32) and 0xFFFFFFFFL) }
        fun bytes(b: ByteArray) { ensure(b.size); b.copyInto(buf, size); size += b.size }
        fun bytes(): ByteArray = buf.copyOf(size)
    }
}

fun zip(init: TestZipBuilder.() -> Unit): ByteArray = TestZipBuilder().apply(init).build()
