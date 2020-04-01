package net.akehurst.language.agl.processor.java8

import net.akehurst.language.agl.processor.Agl
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull

class test_Java8_Grammars {


    @Test(timeout=5000)
    fun Java8() {
        val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

    @Test(timeout=5000)
    fun Java8OptmAgl() {
        val grammarStr = this::class.java.getResource("/java8/Java8OptmAgl.agl").readText()
        //val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8OptmAgl.agl")
        //val bytes = Files.readAllBytes(grammarFile)
        //val grammarStr = String(bytes)
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

    @Test(timeout=5000)
    fun Java8OptmAntlr() {
        val grammarStr = this::class.java.getResource("/java8/Java8OptmAntlr.agl").readText()
        //val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8OptmAntlr.agl")
        //val bytes = Files.readAllBytes(grammarFile)
        //val grammarStr = String(bytes)
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

}