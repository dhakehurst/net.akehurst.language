package net.akehurst.datastore.generic.zip

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreConfigurationBuilder
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.accessConfiguration
import net.akehurst.datastore.common.datastoreConfiguration
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private object TestProvenance : DataItemProvenance

class test_ZipDatastore {

    @BeforeTest
    fun before() {
        DatastoreProviderRegistryDefault.GLOBAL.register(ZipProvider())
    }

    private suspend fun open(config: DatastoreConfigurationBuilder.() -> Unit = {}): Datastore {
        val cfg = datastoreConfiguration(ZipProvider.IDENTITY, init = config)
        val sut = ZipProvider().provide(DatastoreIdentity("zip"), cfg)
        sut.open()
        return sut
    }

    private fun access(init: DatastoreConfigurationBuilder.() -> Unit = {}): DatastoreConfiguration =
        accessConfiguration(ZipProvider.IDENTITY, init = init)

    private suspend fun archiveItem(sut: Datastore, data: Any, name: String = "test.zip"): DataItem =
        DataItemDefault(null, DataItemIdentity(name), sut.accessorMutator().toTypedObject(data, StdLibDefault.AnyType), TestProvenance)

    private suspend fun read(sut: Datastore, bytes: ByteArray, acc: DatastoreConfiguration = access()): List<DataItem> =
        sut.read(acc, flowOf(archiveItem(sut, ZipArchive(bytes)))).toList()

    private suspend fun DataItem.prop(name: String): Any? = data.getPropertySuspend(name).self
    // a null content is 'nothing'
    private suspend fun DataItem.content(name: String): Any? = data.getPropertySuspend("content").getPropertySuspend(name).let { if (it.isNothing) null else it.self }
    private suspend fun DataItem.contentAsString(): String? = content("asString") as String?
    private suspend fun DataItem.contentAsBinary(): ByteArray? = content("asBinary") as ByteArray?

    private val plainZip = zip {
        file("readme.txt", "Hello")
        directory("docs/")
        file("docs/a.txt", "AAA")
        directory("docs/sub/")
        file("docs/sub/b.txt", "BBB", deflate = false)
    }

    @Test
    fun types() = runTest {
        val sut = open()
        val types = sut.types()
        assertTrue(null != types.findFirstDefinitionByPossiblyQualifiedNameOrNull(ZipProvider.ZIP_ARCHIVE_TYPE.asPossiblyQualifiedName))
    }

    @Test
    fun read_plain_zip_with_nested_folders() = runTest {
        val sut = open()
        val actual = read(sut, plainZip)

        assertEquals(listOf("readme.txt", "docs/a.txt", "docs/sub/b.txt"), actual.map { it.identity.value })
        actual.forEach { assertTrue(it.issues.isEmpty(), it.issues.toString()) }
        assertEquals("Hello", actual[0].contentAsString())
        assertEquals("AAA", actual[1].contentAsString())
        assertEquals("BBB", actual[2].contentAsString())
        assertEquals("b.txt", actual[2].prop("name"))
        assertEquals("txt", actual[2].prop("extension"))
        assertEquals("docs/sub/b.txt", actual[2].prop("path"))
        assertEquals(3L, actual[2].prop("size"))
        assertEquals(listOf("test.zip", "docs/a.txt"), actual[1].qualifiedIdentity.map { it.value })
        assertIs<ZipArchive>(actual[1].prop("archive"))
        assertEquals(ZipProvenance(DatastoreIdentity("zip"), listOf(DataItemIdentity("test.zip")), "docs/a.txt"), actual[1].provenance)
    }

    @Test
    fun read_metadata() = runTest {
        val sut = open()
        val bytes = zip {
            file("a.txt", "Hello")
            file("b.txt", "World", unixTime = 1_700_000_000)
        }
        val actual = read(sut, bytes)

        assertEquals(Crc32.of("Hello".encodeToByteArray(), 0, 5), actual[0].prop("crc32"))
        assertEquals(0xF7D18982L, actual[0].prop("crc32")) // CRC-32 of "Hello"
        assertEquals(Instant.parse("2024-05-17T10:30:20Z"), actual[0].prop("lastModified"))
        assertEquals(Instant.fromEpochSeconds(1_700_000_000), actual[1].prop("lastModified"))
        assertTrue((actual[0].prop("compressedSize") as Long) > 0)
    }

    @Test
    fun read_directories_when_asked() = runTest {
        val sut = open()
        val actual = read(sut, plainZip, access { argument(ZipProvider.ACCESS_INCLUDE_DIRECTORIES, true) })

        assertEquals(listOf("readme.txt", "docs/", "docs/a.txt", "docs/sub/", "docs/sub/b.txt"), actual.map { it.identity.value })
        assertEquals(true, actual[1].prop("isDirectory"))
        assertNull(actual[1].contentAsBinary())
    }

    @Test
    fun reqifz_like_archive_with_glob() = runTest {
        val sut = open()
        val bytes = zip {
            file("specs/battery.reqif", "<REQ-IF>battery</REQ-IF>")
            file("specs/thermal.reqif", "<REQ-IF>thermal</REQ-IF>")
            binary("specs/files/picture.png", byteArrayOf(1, 2, 3))
        }
        val actual = read(sut, bytes, access { argument(ZipProvider.ACCESS_GLOB, "**/*.reqif") })

        assertEquals(listOf("specs/battery.reqif", "specs/thermal.reqif"), actual.map { it.identity.value })
        assertEquals("<REQ-IF>thermal</REQ-IF>", actual[1].contentAsString())
    }

    @Test
    fun mdzip_like_archive() = runTest {
        val sut = open()
        val bytes = zip {
            file("model.xmi", "<xmi:XMI>model</xmi:XMI>")
            file("meta/info.properties", "a=b")
            file("diagrams/d1.xml", "<d/>")
        }
        val actual = read(sut, bytes, access { argument(ZipProvider.ACCESS_EXCLUDE_GLOB, "**/diagrams/**") })

        assertEquals(listOf("model.xmi", "meta/info.properties"), actual.map { it.identity.value })
        assertEquals("<xmi:XMI>model</xmi:XMI>", actual[0].contentAsString())
    }

    @Test
    fun include_and_exclude_glob() = runTest {
        val sut = open()
        val actual = read(sut, plainZip, access {
            argument(ZipProvider.ACCESS_GLOB, "docs/**")
            argument(ZipProvider.ACCESS_EXCLUDE_GLOB, "**/sub/**")
        })

        assertEquals(listOf("docs/a.txt"), actual.map { it.identity.value })
    }

    @Test
    fun empty_archive() = runTest {
        val sut = open()
        val bytes = zip { }
        val actual = read(sut, bytes)

        assertEquals(emptyList(), actual)
    }

    @Test
    fun bytes_input() = runTest {
        val sut = open()
        val actual = sut.read(access(), flowOf(archiveItem(sut, plainZip))).toList()

        assertEquals(3, actual.size)
    }

    @Test
    fun non_zip_input_gives_error_item() = runTest {
        val sut = open()
        val actual = read(sut, "this is not a zip archive, just text".encodeToByteArray())

        assertEquals(1, actual.size)
        assertEquals("zipArchive", actual[0].identity.value)
        assertEquals(1, actual[0].issues.errors.size)
        assertTrue(actual[0].issues.errors[0].message.contains("not a zip archive"), actual[0].issues.toString())
    }

    @Test
    fun truncated_zip_gives_error_item() = runTest {
        val sut = open()
        val actual = read(sut, plainZip.copyOf(plainZip.size - 30))

        assertEquals(1, actual.size)
        assertEquals(1, actual[0].issues.errors.size, actual[0].issues.toString())
    }

    @Test
    fun wrong_input_type_gives_error_item() = runTest {
        val sut = open()
        val actual = sut.read(access(), flowOf(archiveItem(sut, "text"))).toList()

        assertEquals(1, actual.size)
        assertEquals(1, actual[0].issues.errors.size)
    }

    @Test
    fun encrypted_entry_gives_error_issue() = runTest {
        val sut = open()
        val bytes = zip {
            file("secret.txt", "xxxx", flags = 0x0801)
            file("open.txt", "open")
        }
        val actual = read(sut, bytes)

        assertEquals(2, actual.size)
        assertTrue(actual[0].issues.errors.single().message.contains("encrypted"))
        assertNull(actual[0].contentAsBinary())
        assertEquals("open", actual[1].contentAsString())
    }

    @Test
    fun unsupported_compression_gives_error_issue() = runTest {
        val sut = open()
        val bytes = zip { file("a.txt", "xxxx", deflate = false, method = 12) }
        val actual = read(sut, bytes)

        assertTrue(actual[0].issues.errors.single().message.contains("compression method 12"))
        assertNull(actual[0].contentAsBinary())
    }

    @Test
    fun unsafe_names_give_warnings() = runTest {
        val sut = open()
        val bytes = zip {
            file("../evil.txt", "a")
            file("/abs.txt", "b")
            file("C:/drive.txt", "c")
            file("ok/fine.txt", "d")
        }
        val actual = read(sut, bytes)

        assertEquals(4, actual.size)
        (0..2).forEach { assertEquals(1, actual[it].issues.warnings.size, actual[it].identity.value) }
        assertTrue(actual[3].issues.isEmpty())
        // the name is a plain string, the content can still be read
        assertEquals("a", actual[0].contentAsString())
    }

    @Test
    fun limit_max_entries() = runTest {
        val sut = open { argument(ZipProvider.CONFIG_MAX_ENTRIES, 2L) }
        val actual = read(sut, plainZip)

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.single().message.contains("more than the limit of 2"))
    }

    @Test
    fun limit_max_entry_size() = runTest {
        val sut = open { argument(ZipProvider.CONFIG_MAX_ENTRY_SIZE, 4L) }
        val actual = read(sut, plainZip)

        assertEquals(listOf(1, 0, 0), actual.map { it.issues.errors.size })
        assertNull(actual[0].contentAsString())
        assertEquals("AAA", actual[1].contentAsString())
    }

    @Test
    fun limit_max_total_size() = runTest {
        val sut = open { argument(ZipProvider.CONFIG_MAX_TOTAL_SIZE, 10L) }
        val actual = read(sut, plainZip)

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.single().message.contains("more than the limit of 10"))
    }

    @Test
    fun small_bomb_compression_ratio() = runTest {
        val sut = open()
        val bytes = zip { binary("zeros.bin", ByteArray(4 * 1024 * 1024)) }
        val actual = read(sut, bytes)

        assertTrue(actual[0].issues.errors.single().message.contains("compression ratio"), actual[0].issues.toString())
        assertNull(actual[0].contentAsBinary())
    }

    @Test
    fun entry_that_inflates_to_more_than_declared() = runTest {
        val sut = open()
        val bytes = zip { file("lie.txt", "a".repeat(1000), declaredSize = 10) }
        val actual = read(sut, bytes)

        assertTrue(actual[0].issues.isEmpty())
        assertNull(actual[0].contentAsString())
        assertTrue(actual[0].issues.errors.single().message.contains("more than its declared size"), actual[0].issues.toString())
    }

    @Test
    fun utf8_and_cp437_names() = runTest {
        val sut = open()
        val bytes = zip {
            file("Ünïcödé/数据.txt", "utf8")
            cp437File("Über/Größe.txt", "cp437")
        }
        val actual = read(sut, bytes)

        assertEquals(listOf("Ünïcödé/数据.txt", "Über/Größe.txt"), actual.map { it.identity.value })
        assertEquals("cp437", actual[1].contentAsString())
    }

    @Test
    fun zip64_records() = runTest {
        val sut = open()
        val bytes = TestZipBuilder().apply {
            zip64 = true
            file("a.txt", "AAA")
            file("b/c.txt", "CCC")
        }.build()
        val actual = read(sut, bytes)

        assertEquals(listOf("a.txt", "b/c.txt"), actual.map { it.identity.value })
        assertEquals("CCC", actual[1].contentAsString())
        assertEquals(3L, actual[1].prop("size"))
    }

    private val nestedZip = zip {
        file("outer.txt", "outer")
        binary("inner/inner.zip", zip {
            file("inner.txt", "inner")
            binary("deeper.zip", zip { file("deep.txt", "deep") })
        })
    }

    @Test
    fun nested_zip_not_read_by_default() = runTest {
        val sut = open()
        val actual = read(sut, nestedZip)

        assertEquals(listOf("outer.txt", "inner/inner.zip"), actual.map { it.identity.value })
    }

    @Test
    fun nested_zip_read_when_asked() = runTest {
        val sut = open()
        val actual = read(sut, nestedZip, access { argument(ZipProvider.ACCESS_RECURSE_NESTED, true) })

        assertEquals(
            listOf(
                listOf("test.zip", "outer.txt"),
                listOf("test.zip", "inner/inner.zip"),
                listOf("test.zip", "inner/inner.zip", "inner.txt"),
                listOf("test.zip", "inner/inner.zip", "deeper.zip"),
                listOf("test.zip", "inner/inner.zip", "deeper.zip", "deep.txt"),
            ),
            actual.map { it.qualifiedIdentity.map { it.value } }
        )
        assertEquals("deep", actual[4].contentAsString())
        assertEquals(listOf(DataItemIdentity("test.zip"), DataItemIdentity("inner/inner.zip"), DataItemIdentity("deeper.zip")), (actual[4].provenance as ZipProvenance).archiveIdentity)
    }

    @Test
    fun nested_zip_depth_limit() = runTest {
        val sut = open()
        val actual = read(sut, nestedZip, access {
            argument(ZipProvider.ACCESS_RECURSE_NESTED, true)
            argument(ZipProvider.ACCESS_MAX_NESTING_DEPTH, 1L)
        })

        assertEquals(listOf("outer.txt", "inner/inner.zip", "inner.txt", "deeper.zip"), actual.map { it.identity.value })
    }

    @Test
    fun nested_zip_entries_found_when_the_nested_zip_does_not_match_the_glob() = runTest {
        val sut = open()
        val actual = read(sut, nestedZip, access {
            argument(ZipProvider.ACCESS_GLOB, "**/*.txt")
            argument(ZipProvider.ACCESS_RECURSE_NESTED, true)
        })

        assertEquals(listOf("outer.txt", "inner.txt", "deep.txt"), actual.map { it.identity.value })
        assertEquals(listOf("test.zip", "inner/inner.zip", "deeper.zip", "deep.txt"), actual[2].qualifiedIdentity.map { it.value })
    }

    @Test
    fun content_is_not_held() = runTest {
        val sut = open()
        val actual = read(sut, plainZip)
        // each read inflates again, so two reads give equal but different arrays
        val a = actual[0].contentAsBinary()!!
        val b = actual[0].contentAsBinary()!!
        assertTrue(a.contentEquals(b))
        assertTrue(a !== b)
    }

    @Test
    fun write_is_not_supported() = runTest {
        val sut = open()
        val items = read(sut, plainZip)
        sut.beginTransaction()
        val actual = sut.write(access(), flowOf(items[0])).toList()
        sut.rollback()

        assertEquals(1, actual.size)
        assertTrue(actual[0].issues.errors.single().message.contains("read only"))
    }
}
