package net.akehurst.language.processor

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_Java8_Grammars {


    @Test(timeout=5000)
    fun Java8() {
        //val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
        val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8_all.agl")
        val bytes = Files.readAllBytes(grammarFile)
        val grammarStr = String(bytes)
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

    @Test(timeout=5000)
    fun Java8Optm1() {
        //val grammarStr = this::class.java.getResource("/java8/Java8Optm1.agl").readText()
        val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8Optm1.agl")
        val bytes = Files.readAllBytes(grammarFile)
        val grammarStr = String(bytes)
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

    @Test(timeout=5000)
    fun Java8OptmAntlr() {
        //val grammarStr = this::class.java.getResource("/java8/Java8OptmAntlr.agl").readText()
        val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8OptmAntlr.agl")
        val bytes = Files.readAllBytes(grammarFile)
        val grammarStr = String(bytes)
        val actual = Agl.processor(grammarStr)
        assertNotNull(actual)
    }

}