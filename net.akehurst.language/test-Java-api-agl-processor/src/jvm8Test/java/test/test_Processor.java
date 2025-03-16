/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package test;

import kotlin.Unit;
import net.akehurst.language.agl.Agl;
import net.akehurst.language.agl.simple.ContextAsmSimple;
import net.akehurst.language.agl.processor.ProcessOptionsDefault;
import net.akehurst.language.api.processor.*;
import net.akehurst.language.asm.api.Asm;
import net.akehurst.language.parser.api.ParseOptions;
import net.akehurst.language.parser.api.ParseResult;
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault;
import net.akehurst.language.scanner.api.ScanResult;
import net.akehurst.language.sentence.api.Sentence;
import net.akehurst.language.sentence.common.SentenceDefault;
import net.akehurst.language.sppt.api.LeafData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class test_Processor {

    private static final String EOL = System.lineSeparator();
    private static final String grammarStr = ""
            + "namespace test\n" +
            "grammar Test {\n" +
            "  skip leaf WHITESPACE = \"\\s+\" ;\n" +
            "  skip leaf MULTI_LINE_COMMENT = \"/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/\" ;\n" +
            "  skip leaf SINGLE_LINE_COMMENT = \"//[\\n\\r]*?\" ;\n" +
            "\n" +
            "  value = predefined | object | literal ;\n" +
            "\n" +
            "  predefined = IDENTIFIER ;\n" +
            "  object = '{' property* '}' ;\n" +
            "  property = IDENTIFIER ':' value ;\n" +
            "\n" +
            "  literal = BOOLEAN | INTEGER | REAL | STRING ;\n" +
            "\n" +
            "  leaf BOOLEAN = \"true|false\";\n" +
            "  leaf REAL = \"[0-9]+[.][0-9]+\";\n" +
            "  leaf STRING = \"'([^'\\\\]|\\\\'|\\\\\\\\)*'\";\n" +
            "  leaf INTEGER = \"[0-9]+\";\n" +
            "  leaf IDENTIFIER = \"[a-zA-Z_][a-zA-Z_0-9-]*\" ;\n" +
            "}";

    private static final LanguageProcessor<Asm, ContextAsmSimple> proc = Agl.INSTANCE.processorFromStringSimpleJava(
            grammarStr,
            null,
            null,
            null,
            null,
            null,
            Agl.INSTANCE.configurationSimple(),
            null
    ).getProcessor();

    private static final List<String> sentences = Arrays.asList(
            "true", //BOOLEAN
            "1", //INTEGER
            "3.14", //REAL
            "'Hello World!'", // STRING
            "var1", // predefined
            "{}", // empty object
            "{ a:false b:1 c:3.141 d:'bob' e:var2 }", // object
            "{ f:{x:1 y:{a:3 b:7}} }" //nested objects
    );

    @Test
    public void scan() {
        for(String s: sentences) {
            Sentence sentence = new SentenceDefault(s);
            assert proc != null;
            ScanResult result = proc.scan(sentence.getText());

            Assert.assertNotNull(result);
            Assert.assertFalse(result.getAllTokens().isEmpty());
            String scanned = "";
            for(LeafData it:result.getAllTokens()) {
                scanned += sentence.textAt(it.getPosition(), it.getLength());
            }

            Assert.assertEquals(s, scanned);
        }
    }

    @Test
    public void parse_noOptions() {
        for(String s: sentences) {
            ParseResult result = proc.parse(s, null);
            Assert.assertNotNull(result.getSppt());
            System.out.println(result.getSppt().getToStringAll());
        }
    }

    @Test
    public void parse_defaultOptions() {
        ParseOptions options = proc.parseOptionsDefault();
        options.setGoalRuleName("value");
        ParseResult result = proc.parse("{ a:false b:1 c:3.141 d:'bob' e:var2 }", options);

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void parse_buildOptions() {
        ParseResult result = proc.parse("{ a:false b:1 c:3.141 d:'bob' e:var2 }", Agl.INSTANCE.parseOptions(new ParseOptionsDefault(), b -> {
            b.goalRuleName("value");
            return Unit.INSTANCE;
        }));

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void syntaxAnalysis() {
        ParseResult parse = proc.parse("{ a:false b:1 c:3.141 d:'bob' e:var2 }", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<Asm> result = proc.syntaxAnalysis(parse.getSppt(), null);

        Assert.assertNotNull(result.getAsm());
        System.out.println(result.getAsm());
    }

    @Test
    public void semanticAnalysis() {
        ParseResult parse = proc.parse("{ a:false b:1 c:3.141 d:'bob' e:var2 }", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<Asm> synt = proc.syntaxAnalysis(parse.getSppt(), null);
        Assert.assertNotNull(synt.getAsm());
        SemanticAnalysisResult result = proc.semanticAnalysis(synt.getAsm(), Agl.INSTANCE.options(new ProcessOptionsDefault<>(), b ->{
            b.semanticAnalysis(b2->{
                b2.context(new ContextAsmSimple());
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        }));

        Assert.assertEquals(result.getIssues().toString(),0, result.getIssues().getErrors().size());
    }

    @Test
    public void process_noOptions() {
        ProcessResult<Asm> result = proc.process("{ a:false b:1 c:3.141 d:'bob' e:var2 }", null);
        Assert.assertNotNull(result.getAsm());
        Assert.assertNotNull(result.getIssues());
    }

    @Test
    public void process_defaultOptions() {
        ProcessOptions<Asm, ContextAsmSimple> options = proc.optionsDefault();
        options.getParse().setGoalRuleName("value");
        ProcessResult<Asm> result = proc.process("{ a:false b:1 c:3.141 d:'bob' e:var2 }", options);
        Assert.assertNotNull(result.getAsm());
        Assert.assertNotNull(result.getIssues());
    }
}
