/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.antlr4;

import net.akehurst.language.comparisons.common.FileData;
import net.akehurst.language.comparisons.common.Java8TestFiles;
import net.akehurst.language.comparisons.common.Results;
import net.akehurst.language.comparisons.common.TimeLogger;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

@RunWith(Parameterized.class)
public class test_antlr4_Java8_optm {

    static int totalFiles;
    static CharStream input;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<FileData> getFiles() {
        var files = Java8TestFiles.INSTANCE.getFiles();
        totalFiles = files.size();
        return files;
    }

    static antlr4.optm.Java8ParserOpt.CompilationUnitContext parseWithAntlr4Optm(final FileData file) {
        try {
            final Lexer lexer = new antlr4.optm.Java8LexerOpt(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);

            //pre cache
            final antlr4.optm.Java8ParserOpt p1 = new antlr4.optm.Java8ParserOpt(tokens);
            p1.setErrorHandler(new BailErrorStrategy());
            p1.compilationUnit();

            tokens.seek(0);
            final antlr4.optm.Java8ParserOpt p = new antlr4.optm.Java8ParserOpt(tokens);
            p.setErrorHandler(new BailErrorStrategy());
            try (TimeLogger timer = new TimeLogger("antlr4_optm", file)) {
                antlr4.optm.Java8ParserOpt.CompilationUnitContext r = p.compilationUnit();
                timer.success();
                return r;
            }
        } catch (ParseCancellationException | RecognitionException e) {
            Results.INSTANCE.logError("antlr4_optm", file);
            Assert.assertTrue(file.isError());
            return null;
        }
    }

    final FileData file;

    public test_antlr4_Java8_optm(final FileData file) {
        this.file = file;
    }

    @BeforeClass
    public static void init() {
        Results.INSTANCE.reset();
    }

    @Before
    public void setUp() {
        try {
            input = CharStreams.fromPath(this.file.getPath());
        } catch (final IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void antlr4_optm_compilationUnit1() {
        System.out.println("" + file.getIndex() + " of " + totalFiles);
        parseWithAntlr4Optm(this.file);
    }

    @AfterClass
    public static void end() {
        Results.INSTANCE.write();
    }
}
