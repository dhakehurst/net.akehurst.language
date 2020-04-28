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

import net.akehurst.language.comparisons.common.TimeLogger;
import org.antlr.v4.runtime.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;

import net.akehurst.language.comparisons.common.Java8TestFiles;

@RunWith(Parameterized.class)
public class test_antlr4_Java8 {

    static CharStream input;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles() {
        return Java8TestFiles.INSTANCE.getFiles();
    }

    static antlr4.spec.Java8Parser.CompilationUnitContext parseWithAntlr4Spec(final Path file) {

        final Lexer lexer = new antlr4.spec.Java8Lexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final antlr4.spec.Java8Parser p = new antlr4.spec.Java8Parser(tokens);
        p.setErrorHandler(new BailErrorStrategy());
        try (TimeLogger timer = new TimeLogger("antlr4_spec", file.toString());) {
            antlr4.spec.Java8Parser.CompilationUnitContext r = p.compilationUnit();
            timer.success();
            return r;
        } catch (final Exception e) {
            return null;
        }
    }

    static antlr4.optm.Java8Parser.CompilationUnitContext parseWithAntlr4Optm(final Path file) {

        final Lexer lexer = new antlr4.optm.Java8Lexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final antlr4.optm.Java8Parser p = new antlr4.optm.Java8Parser(tokens);
        p.setErrorHandler(new BailErrorStrategy());
        try (TimeLogger timer = new TimeLogger("antlr4_optm", file.toString());) {
            antlr4.optm.Java8Parser.CompilationUnitContext r = p.compilationUnit();
            timer.success();
            return r;
        } catch (final Exception e) {
            return null;
        }
    }

    final Path file;

    public test_antlr4_Java8(final Path file) {
        this.file = file;
    }

    @Before
    public void setUp() {
        try {
            input = CharStreams.fromPath(this.file);
        } catch (final IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void antlr4_spec_compilationUnit() {
        final antlr4.spec.Java8Parser.CompilationUnitContext tree = parseWithAntlr4Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void antlr4_optm_compilationUnit1() {
        final antlr4.optm.Java8Parser.CompilationUnitContext tree = parseWithAntlr4Optm(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

}
