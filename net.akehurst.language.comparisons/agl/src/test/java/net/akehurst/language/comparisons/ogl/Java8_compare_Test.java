/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.ogl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.comparisons.common.Java8TestFiles;
import net.akehurst.language.comparisons.common.TimeLogger;
import net.akehurst.language.processor.Agl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class Java8_compare_Test {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles() {
        return Java8TestFiles.getFiles();
    }

    static CharSequence input;
    static LanguageProcessor oglSpecJava8Processor = getOglSpecJava8Processor();
    static LanguageProcessor oglOptmAntlrJava8Processor = getOglOptmAntlr8JavaProcessor();
    static LanguageProcessor oglOptm1Java8Processor = getOglOptm1Java8Processor();

    static LanguageProcessor getOglSpecJava8Processor() {
        if (null == oglSpecJava8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8Spec.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                oglSpecJava8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                oglSpecJava8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }
        }
        return oglSpecJava8Processor;
    }

    static LanguageProcessor getOglOptmAntlr8JavaProcessor() {
        if (null == oglOptmAntlrJava8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8OptmAntlr.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                oglOptmAntlrJava8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                oglOptmAntlrJava8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }
        }
        return oglOptmAntlrJava8Processor;
    }

    static LanguageProcessor getOglOptm1Java8Processor() {
        if (null == oglOptm1Java8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8Optm1.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                oglOptm1Java8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                oglOptm1Java8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }

        }
        return oglOptm1Java8Processor;
    }

    static SharedPackedParseTree parseWithOglJava8Spec(final Path file) {
        try (TimeLogger timer = new TimeLogger("ogl_spec", file.toString());) {
            final SharedPackedParseTree tree = oglSpecJava8Processor.parse("compilationUnit",
                    input);
            timer.success();
            return tree;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static SharedPackedParseTree parseWithOglJava8OptmAntlr(final Path file) {
        try (TimeLogger timer = new TimeLogger("ogl_optmAntlr", file.toString());) {
            final SharedPackedParseTree tree = oglOptmAntlrJava8Processor.parse("compilationUnit",
                    input);
            timer.success();
            return tree;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static SharedPackedParseTree parseWithOglJava8Optm1(final Path file) {
        try (TimeLogger timer = new TimeLogger("ogl_optm1", file.toString());) {
            final SharedPackedParseTree tree = oglOptm1Java8Processor.parse("compilationUnit",
                    input);
            timer.success();
            return tree;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    final Path file;

    public Java8_compare_Test(final Path file) {
        this.file = file;
    }

    @Before
    public void setUp() {
        try {
            input = new String(Files.readAllBytes(this.file) );
        } catch (final IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void ogl_spec_compilationUnit() {
        final SharedPackedParseTree tree = parseWithOglJava8Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void ogl_optmAntlr_compilationUnit() {
        final SharedPackedParseTree tree = parseWithOglJava8OptmAntlr(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void ogl_optm1_compilationUnit() {
        final SharedPackedParseTree tree = parseWithOglJava8Optm1(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }
}
