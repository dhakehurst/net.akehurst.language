package test;

import net.akehurst.language.agl.processor.Agl;
import net.akehurst.language.api.processor.*;
import net.akehurst.language.api.sppt.SPPTLeaf;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class test_LanguageProcessor {

    private static final String EOL = System.lineSeparator();
    private static final String grammarStr = ""
            + "namespace test" + EOL
            + "grammar Test {" + EOL
            + "  skip WS=\"\\s+\";" + EOL
            + "  S='hello' 'world' '!';" + EOL
            + "}";

    private static final LanguageProcessor proc = Agl.INSTANCE.processorFromString(grammarStr, null, null, null, null, null);

    @Test
    public void scan() {

        List<SPPTLeaf> result = proc.scan("hello world !");

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("hello", result.get(0).getMatchedText());
    }

    @Test
    public void parse() {
        ParseResult result = proc.parse("hello world !", null);

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void syntaxAnalysis() {
        ParseResult parse = proc.parse("hello world !", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<Object> result = proc.syntaxAnalysis(parse.getSppt(), null);

        Assert.assertNotNull(result.getAsm());
        System.out.println(result.getAsm());
    }

    @Test
    public void semanticAnalysis() {
        ParseResult parse = proc.parse("hello world !", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<Object> synt = proc.syntaxAnalysis(parse.getSppt(), null);
        Assert.assertNotNull(synt.getAsm());
        SemanticAnalysisResult result = proc.semanticAnalysis(synt.getAsm(), null);

        Assert.assertEquals(0, result.getIssues().size());
    }

    @Test
    public void process() {
        ProcessResult<Object> result = proc.process("hello world !", null);
        Assert.assertNotNull(result.getAsm());
        Assert.assertNotNull(result.getIssues());
    }
}
