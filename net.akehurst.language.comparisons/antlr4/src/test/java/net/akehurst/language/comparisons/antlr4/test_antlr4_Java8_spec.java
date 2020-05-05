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
import net.akehurst.language.comparisons.common.TimeLogger;
import org.antlr.v4.runtime.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

@RunWith(Parameterized.class)
public class test_antlr4_Java8_spec {

    static CharStream input;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<FileData> getFiles() {
        return Java8TestFiles.INSTANCE.getFiles();
    }

    static antlr4.spec.Java8Parser.CompilationUnitContext parseWithAntlr4Spec(final FileData file) {
        final Lexer lexer = new antlr4.spec.Java8Lexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        //pre cache
        final antlr4.optm.Java8Parser p1 = new antlr4.optm.Java8Parser(tokens);
        p1.setErrorHandler(new BailErrorStrategy());
        p1.compilationUnit();

        final antlr4.spec.Java8Parser p = new antlr4.spec.Java8Parser(tokens);
        p.setErrorHandler(new BailErrorStrategy());

        try (TimeLogger timer = new TimeLogger("antlr4_spec", file)) {
            antlr4.spec.Java8Parser.CompilationUnitContext r = p.compilationUnit();
            timer.success();
            return r;
        }
    }

    final FileData file;

    public test_antlr4_Java8_spec(final FileData file) {
        this.file = file;
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
    public void antlr4_spec_compilationUnit() {
        final antlr4.spec.Java8Parser.CompilationUnitContext tree = parseWithAntlr4Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

}
