package net.akehurst.datastore.generic.zip

import korlibs.io.compression.deflate.Deflate
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/** the bytes are not a zip archive that can be read, or an entry cannot be read */
class ZipFormatException(message: String) : Exception(message)

/**
 * One entry of a zip archive, as its central directory record describes it.
 * Sizes, CRC and time are the declared values; the content is not read.
 */
class ZipEntryRecord(
    /** the entry's name, '/'-separated, as stored (decoded as UTF-8 or CP437) */
    val path: String,
    val flags: Int,
    /** 0 = stored, 8 = deflated; others are not supported */
    val compressionMethod: Int,
    /** CRC-32 of the uncompressed content, as an unsigned value */
    val crc32: Long,
    val compressedSize: Long,
    val size: Long,
    val localHeaderOffset: Long,
    val lastModified: Instant,
) {
    val isDirectory: Boolean get() = path.endsWith("/")

    /** traditional (bit 0) or strong (bit 6) encryption */
    val isEncrypted: Boolean get() = (flags and 0x0001) != 0 || (flags and 0x0040) != 0

    val isCompressionSupported: Boolean get() = compressionMethod == METHOD_STORED || compressionMethod == METHOD_DEFLATED

    companion object {
        const val METHOD_STORED = 0
        const val METHOD_DEFLATED = 8
    }
}

/**
 * Reads zip archives held in a ByteArray, on every platform.
 * Only the central directory is read up front; an entry's content is inflated when asked for.
 * Handles ZIP64, UTF-8 (flag bit 11, or the Info-ZIP Unicode Path extra field) and CP437 names,
 * and the extended timestamp extra field. Inflating is done by korlibs Deflate (java.util.zip on the JVM).
 */
object ZipFormat {

    private const val SIG_LOCAL = 0x04034b50L
    private const val SIG_CENTRAL = 0x02014b50L
    private const val SIG_EOCD = 0x06054b50L
    private const val SIG_ZIP64_EOCD = 0x06064b50L
    private const val SIG_ZIP64_LOCATOR = 0x07064b50L

    private const val EXTRA_ZIP64 = 0x0001
    private const val EXTRA_EXT_TIMESTAMP = 0x5455
    private const val EXTRA_UNICODE_PATH = 0x7075

    private const val FLAG_UTF8 = 0x0800

    /** true if the bytes start like a zip archive: a local file header (PK\3\4), or the end record of an empty archive (PK\5\6) */
    fun looksLikeZip(bytes: ByteArray): Boolean = looksLikeZipStart(bytes, bytes.size)

    internal fun looksLikeZipStart(bytes: ByteArray, length: Int): Boolean = length >= 4 && (u32(bytes, 0) == SIG_LOCAL || u32(bytes, 0) == SIG_EOCD)

    /**
     * the number of entries the archive declares, read from its end record, without reading the records,
     * so that an archive with too many entries can be refused before they are read
     */
    fun declaredEntryCount(bytes: ByteArray): Long = endRecord(bytes).entryCount

    /** the entries of the central directory, in their stored order */
    fun readCentralDirectory(bytes: ByteArray): List<ZipEntryRecord> {
        val end = endRecord(bytes)
        val result = ArrayList<ZipEntryRecord>(minOf(end.entryCount, 1024L).toInt())
        var pos = end.directoryOffset
        for (n in 0 until end.entryCount) {
            need(bytes, pos, 46, "central directory record $n")
            if (u32(bytes, pos.toInt()) != SIG_CENTRAL) throw ZipFormatException("Central directory record $n is not valid (bad signature)")
            val p = pos.toInt()
            val flags = u16(bytes, p + 8)
            val method = u16(bytes, p + 10)
            val dosTime = u16(bytes, p + 12)
            val dosDate = u16(bytes, p + 14)
            val crc = u32(bytes, p + 16)
            var compressedSize = u32(bytes, p + 20)
            var size = u32(bytes, p + 24)
            val nameLen = u16(bytes, p + 28)
            val extraLen = u16(bytes, p + 30)
            val commentLen = u16(bytes, p + 32)
            var localOffset = u32(bytes, p + 42)
            need(bytes, pos + 46, (nameLen + extraLen + commentLen).toLong(), "central directory record $n")
            val nameBytes = bytes.copyOfRange(p + 46, p + 46 + nameLen)
            val extraStart = p + 46 + nameLen
            var name = if ((flags and FLAG_UTF8) != 0) nameBytes.decodeToString() else Cp437.decode(nameBytes)
            var lastModified = dosToInstant(dosTime, dosDate)

            forEachExtra(bytes, extraStart, extraLen) { id, dataStart, dataLen ->
                when (id) {
                    EXTRA_ZIP64 -> {
                        // only the fields whose 32-bit value is 0xFFFFFFFF are present, in this order
                        var q = dataStart
                        val dataEnd = dataStart + dataLen
                        if (size == 0xFFFFFFFFL && q + 8 <= dataEnd) { size = u64(bytes, q); q += 8 }
                        if (compressedSize == 0xFFFFFFFFL && q + 8 <= dataEnd) { compressedSize = u64(bytes, q); q += 8 }
                        if (localOffset == 0xFFFFFFFFL && q + 8 <= dataEnd) { localOffset = u64(bytes, q) }
                    }

                    EXTRA_EXT_TIMESTAMP -> {
                        // flags byte, then (if bit 0) the modification time in seconds since 1970, UTC
                        if (dataLen >= 5 && (bytes[dataStart].toInt() and 0x01) != 0) {
                            lastModified = Instant.fromEpochSeconds(s32(bytes, dataStart + 1).toLong())
                        }
                    }

                    EXTRA_UNICODE_PATH -> {
                        // version 1, CRC-32 of the stored name, the name in UTF-8; used only if the stored name is unchanged
                        if (dataLen >= 5 && bytes[dataStart].toInt() == 1 && u32(bytes, dataStart + 1) == Crc32.of(nameBytes, 0, nameBytes.size)) {
                            name = bytes.copyOfRange(dataStart + 5, dataStart + dataLen).decodeToString()
                        }
                    }
                }
            }

            result.add(ZipEntryRecord(name, flags, method, crc, compressedSize, size, localOffset, lastModified))
            pos += 46 + nameLen + extraLen + commentLen
        }
        return result
    }

    /** the range (start, length) of the entry's compressed data, found from its local header */
    fun dataRange(bytes: ByteArray, record: ZipEntryRecord): Pair<Int, Int> {
        val off = record.localHeaderOffset
        need(bytes, off, 30, "local header of '${record.path}'")
        if (u32(bytes, off.toInt()) != SIG_LOCAL) throw ZipFormatException("The local header of '${record.path}' is not valid (bad signature)")
        val nameLen = u16(bytes, off.toInt() + 26)
        val extraLen = u16(bytes, off.toInt() + 28)
        val start = off + 30 + nameLen + extraLen
        need(bytes, start, record.compressedSize, "data of '${record.path}'")
        return Pair(start.toInt(), record.compressedSize.toInt())
    }

    /**
     * the uncompressed content of the entry. It must inflate to exactly its declared size (so it can never be
     * bigger than the size the limits were checked against) and match its CRC-32.
     * The caller checks encryption, compression method and size limits first.
     */
    suspend fun content(bytes: ByteArray, record: ZipEntryRecord): ByteArray {
        if (record.isEncrypted) throw ZipFormatException("'${record.path}' is encrypted, which is not supported")
        if (record.size > Int.MAX_VALUE) throw ZipFormatException("'${record.path}' is too big to read into memory (${record.size} bytes)")
        val (start, length) = dataRange(bytes, record)
        val result = when (record.compressionMethod) {
            ZipEntryRecord.METHOD_STORED -> {
                if (length.toLong() != record.size) throw ZipFormatException("'${record.path}' is stored with ${length} bytes but declares ${record.size}")
                bytes.copyOfRange(start, start + length)
            }

            ZipEntryRecord.METHOD_DEFLATED -> {
                val out = BoundedOutput(record.size.toInt())
                try {
                    Deflate.uncompress(SliceInput(bytes, start, length), out)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    when {
                        out.overflow -> throw ZipFormatException("'${record.path}' inflates to more than its declared size of ${record.size} bytes")
                        else -> throw ZipFormatException("'${record.path}' cannot be inflated: ${e.message ?: e::class.simpleName}")
                    }
                }
                if (out.written.toLong() != record.size) throw ZipFormatException("'${record.path}' inflates to ${out.written} bytes but declares ${record.size}")
                out.buffer
            }

            else -> throw ZipFormatException("'${record.path}' uses compression method ${record.compressionMethod}, which is not supported (only stored and deflated)")
        }
        val crc = Crc32.of(result, 0, result.size)
        if (crc != record.crc32) throw ZipFormatException("'${record.path}' is corrupt: its CRC-32 is ${crc.toString(16)}, expected ${record.crc32.toString(16)}")
        return result
    }

    /** the first 'n' bytes (or fewer, if the entry is shorter) of the entry's uncompressed content, without inflating the rest */
    suspend fun contentPrefix(bytes: ByteArray, record: ZipEntryRecord, n: Int): ByteArray {
        val (start, length) = dataRange(bytes, record)
        return when (record.compressionMethod) {
            ZipEntryRecord.METHOD_STORED -> bytes.copyOfRange(start, start + minOf(n, length))
            ZipEntryRecord.METHOD_DEFLATED -> {
                val out = BoundedOutput(n, stopWhenFull = true)
                try {
                    Deflate.uncompress(SliceInput(bytes, start, length), out)
                } catch (_: StopInflate) {
                    // enough bytes
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw ZipFormatException("'${record.path}' cannot be inflated: ${e.message ?: e::class.simpleName}")
                }
                out.buffer.copyOf(out.written)
            }

            else -> throw ZipFormatException("'${record.path}' uses compression method ${record.compressionMethod}, which is not supported (only stored and deflated)")
        }
    }

    // --- end of central directory ---

    private class EndRecord(val entryCount: Long, val directoryOffset: Long)

    private fun endRecord(bytes: ByteArray): EndRecord {
        if (bytes.size < 22) throw ZipFormatException("Not a zip archive: too short (${bytes.size} bytes)")
        // the end record is the last thing in the archive, followed by a comment of at most 65535 bytes
        val lowest = maxOf(0, bytes.size - 22 - 0xFFFF)
        var eocd = -1
        var i = bytes.size - 22
        while (i >= lowest) {
            if (u32(bytes, i) == SIG_EOCD && i + 22 + u16(bytes, i + 20) == bytes.size) {
                eocd = i; break
            }
            i--
        }
        if (eocd < 0) {
            // accept a comment length that does not match exactly, as some writers get it wrong
            i = bytes.size - 22
            while (i >= lowest) {
                if (u32(bytes, i) == SIG_EOCD) { eocd = i; break }
                i--
            }
        }
        if (eocd < 0) throw ZipFormatException("Not a zip archive: no end of central directory record (the file may be truncated)")
        var count = u16(bytes, eocd + 10).toLong()
        var offset = u32(bytes, eocd + 16)
        // ZIP64: a locator just before the end record points to the ZIP64 end record
        val loc = eocd - 20
        if (loc >= 0 && u32(bytes, loc) == SIG_ZIP64_LOCATOR) {
            val z = u64(bytes, loc + 8)
            need(bytes, z, 56, "ZIP64 end of central directory record")
            if (u32(bytes, z.toInt()) != SIG_ZIP64_EOCD) throw ZipFormatException("The ZIP64 end of central directory record is not valid (bad signature)")
            count = u64(bytes, z.toInt() + 32)
            offset = u64(bytes, z.toInt() + 48)
        } else if (count == 0xFFFFL || offset == 0xFFFFFFFFL) {
            throw ZipFormatException("The archive needs ZIP64 records, but they are missing")
        }
        if (u16(bytes, eocd + 4) != 0 || u16(bytes, eocd + 6) != 0) throw ZipFormatException("Multi-part (split or spanned) archives are not supported")
        if (count < 0) throw ZipFormatException("The archive declares a negative number of entries")
        if (offset < 0 || offset > bytes.size) throw ZipFormatException("The central directory offset is outside the archive")
        return EndRecord(count, offset)
    }

    // --- helpers ---

    private inline fun forEachExtra(bytes: ByteArray, start: Int, length: Int, action: (id: Int, dataStart: Int, dataLen: Int) -> Unit) {
        var q = start
        val end = start + length
        while (q + 4 <= end) {
            val id = u16(bytes, q)
            val len = u16(bytes, q + 2)
            if (q + 4 + len > end) break
            action(id, q + 4, len)
            q += 4 + len
        }
    }

    private fun need(bytes: ByteArray, pos: Long, len: Long, what: String) {
        if (pos < 0 || len < 0 || pos + len > bytes.size) throw ZipFormatException("The $what is outside the archive (the file may be truncated or corrupt)")
    }

    private fun u16(b: ByteArray, i: Int): Int = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
    private fun s32(b: ByteArray, i: Int): Int = u16(b, i) or (u16(b, i + 2) shl 16)
    private fun u32(b: ByteArray, i: Int): Long = s32(b, i).toLong() and 0xFFFFFFFFL
    private fun u64(b: ByteArray, i: Int): Long = u32(b, i) or (u32(b, i + 4) shl 32)

    /** MS-DOS date and time (no time zone; taken as UTC) */
    internal fun dosToInstant(time: Int, date: Int): Instant {
        val second = (time and 0x1F) * 2
        val minute = (time shr 5) and 0x3F
        val hour = (time shr 11) and 0x1F
        val day = maxOf(1, date and 0x1F)
        val month = maxOf(1, minOf(12, (date shr 5) and 0x0F))
        val year = 1980 + ((date shr 9) and 0x7F)
        return Instant.fromEpochSeconds(daysFromCivil(year, month, day) * 86400L + hour * 3600L + minute * 60L + second)
    }

    /** days since 1970-01-01 of a proleptic Gregorian date (H. Hinnant's algorithm) */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    private object StopInflate : Exception("enough bytes")

    /** reads a range of a ByteArray, without copying it */
    private class SliceInput(private val bytes: ByteArray, start: Int, length: Int) : AsyncInputStream {
        private var pos = start
        private val end = start + length
        override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
            if (pos >= end) return -1
            val n = minOf(len, end - pos)
            bytes.copyInto(buffer, offset, pos, pos + n)
            pos += n
            return n
        }

        override suspend fun close() {}
    }

    /**
     * collects at most 'capacity' bytes. If more are written it either stops the inflater (stopWhenFull)
     * or marks 'overflow' and stops it, so a lying entry cannot use more memory than it declares.
     */
    private class BoundedOutput(capacity: Int, private val stopWhenFull: Boolean = false) : AsyncOutputStream {
        val buffer = ByteArray(capacity)
        var written = 0
        var overflow = false
        override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
            val n = minOf(len, this.buffer.size - written)
            buffer.copyInto(this.buffer, written, offset, offset + n)
            written += n
            if (n < len) {
                if (stopWhenFull) throw StopInflate
                overflow = true
                throw ZipFormatException("inflates to more than its declared size")
            }
            if (stopWhenFull && written == this.buffer.size) throw StopInflate
        }

        override suspend fun close() {}
    }
}

/** CRC-32 (ISO-HDLC, as zip uses) */
object Crc32 {
    private val table = LongArray(256) { n ->
        var c = n.toLong()
        repeat(8) { c = if ((c and 1L) != 0L) 0xEDB88320L xor (c ushr 1) else c ushr 1 }
        c
    }

    fun of(bytes: ByteArray, start: Int, length: Int): Long {
        var c = 0xFFFFFFFFL
        for (i in start until start + length) {
            c = table[((c xor bytes[i].toLong()) and 0xFF).toInt()] xor (c ushr 8)
        }
        return c xor 0xFFFFFFFFL
    }
}

/** IBM code page 437, the default encoding of zip entry names */
object Cp437 {
    private const val HIGH =
        "ÇüéâäàåçêëèïîìÄÅ" +
            "ÉæÆôöòûùÿÖÜ¢£¥₧ƒ" +
            "áíóúñÑªº¿⌐¬½¼¡«»" +
            "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐" +
            "└┴┬├─┼╞╟╚╔╩╦╠═╬╧" +
            "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" +
            "αßΓπΣσµτΦΘΩδ∞φε∩" +
            "≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ "

    fun decode(bytes: ByteArray): String = buildString(bytes.size) {
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append(if (v < 0x80) v.toChar() else HIGH[v - 0x80])
        }
    }

    /** the CP437 bytes of 'text'; for tests. Characters that CP437 does not have become '?' */
    fun encode(text: String): ByteArray = ByteArray(text.length) { i ->
        val c = text[i]
        when {
            c.code < 0x80 -> c.code.toByte()
            else -> HIGH.indexOf(c).let { if (it < 0) '?'.code.toByte() else (it + 0x80).toByte() }
        }
    }
}
