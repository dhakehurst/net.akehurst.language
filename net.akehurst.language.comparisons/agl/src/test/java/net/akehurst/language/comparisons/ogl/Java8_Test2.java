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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.akehurst.language.processor.Agl;

@RunWith(Parameterized.class)
public class Java8_Test2 {

    static String javaTestFiles = "../javaTestFiles/javac";

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles() {
        final ArrayList<Object[]> params = new ArrayList<>();
        try {
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

            Files.walkFileTree(Paths.get(javaTestFiles), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() && matcher.matches(file)) {
                        params.add(new Object[] { file });
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            e.printStackTrace();
            //Assert.fail(e.getMessage());
        }
        return params;
    }

    static String og_input;
    static LanguageProcessor oglSpecJava8Processor = Java8_Test2.getOglSpecJava8Processor();
    static LanguageProcessor oglOptmAntlrJava8Processor = Java8_Test2.getOglOptmAntlr8JavaProcessor();
    static LanguageProcessor oglOptm1Java8Processor = Java8_Test2.getOglOptm1Java8Processor();

    static LanguageProcessor getOglSpecJava8Processor() {
        if (null == Java8_Test2.oglSpecJava8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8Spec.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                Java8_Test2.oglSpecJava8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                Java8_Test2.oglSpecJava8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }
        }
        return Java8_Test2.oglSpecJava8Processor;
    }

    static LanguageProcessor getOglOptmAntlr8JavaProcessor() {
        if (null == Java8_Test2.oglOptmAntlrJava8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8OptmAntlr.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                Java8_Test2.oglOptmAntlrJava8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                Java8_Test2.oglOptmAntlrJava8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }
        }
        return Java8_Test2.oglOptmAntlrJava8Processor;
    }

    static LanguageProcessor getOglOptm1Java8Processor() {
        if (null == Java8_Test2.oglOptm1Java8Processor) {
            try {
                final Path grammarFile = Paths.get("src/test/agl/Java8Optm1.agl");
                final byte[] bytes = Files.readAllBytes(grammarFile);
                final String javaGrammarStr = new String(bytes);
                Java8_Test2.oglOptm1Java8Processor = Agl.INSTANCE.processor(javaGrammarStr,null,null);
                Java8_Test2.oglOptm1Java8Processor.build();
            } catch (final Exception e) {
                e.printStackTrace();
                //Assert.fail(e.getMessage());
            }

        }
        return Java8_Test2.oglOptm1Java8Processor;
    }

    static SharedPackedParseTree parseWithOglJava8Spec(final Path file) {
        try {
            final SharedPackedParseTree tree = oglSpecJava8Processor.parse("compilationUnit",
                    Java8_Test2.og_input);
            return tree;
        } catch (Exception e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    static SharedPackedParseTree parseWithOglJava8OptmAntlr(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_Test2.oglOptmAntlrJava8Processor.parse("compilationUnit",
                    Java8_Test2.og_input);
            return tree;
        } catch (Exception e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    static SharedPackedParseTree parseWithOglJava8Optm1(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_Test2.oglOptm1Java8Processor.parse("compilationUnit",
                    Java8_Test2.og_input);
            return tree;
        } catch (Exception e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    final Path file;

    public Java8_Test2(final Path file) {
        this.file = file;
    }

    @Before
    public void setUp() {
        try {
            final byte[] bytes = Files.readAllBytes(this.file);
            Java8_Test2.og_input = new String(bytes);
        } catch (final IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void ogl_spec_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    @Ignore
    public void ogl_optmAntlr_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8OptmAntlr(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    @Ignore
    public void ogl_optm1_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8Optm1(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

}
