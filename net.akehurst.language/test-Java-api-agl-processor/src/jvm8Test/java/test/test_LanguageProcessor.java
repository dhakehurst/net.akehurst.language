package test;

import kotlin.Unit;
import net.akehurst.language.agl.processor.Agl;
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple;
import net.akehurst.language.api.asm.AsmSimple;
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
            + "  skip WS = \"\\s+\" ;" + EOL
            + "  S = H W ;" + EOL
            + "  H = 'hello' ;"+EOL
            + "  W = 'world' '!' ;" + EOL
            + "}";

    private static final LanguageProcessor<AsmSimple, ContextSimple> proc = Agl.INSTANCE.processorFromStringDefault(grammarStr, null);

    @Test
    public void scan() {

        List<SPPTLeaf> result = proc.scan("hello world !");

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("hello", result.get(0).getMatchedText());
    }

    @Test
    public void parse_noOptions() {
        ParseResult result = proc.parse("hello world !", null);

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void parse_defaultOptions() {
        ParseOptions options = proc.parseOptionsDefault();
        options.setGoalRuleName("H");
        ParseResult result = proc.parse("world !", options);

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void parse_buildOptions() {

        ParseResult result = proc.parse("world !", proc.parseOptions( KotlinFromJava.toKotlin(b-> {
            b.goalRuleName("H");
            return Unit.INSTANCE;
        })));

        Assert.assertNotNull(result.getSppt());
        System.out.println(result.getSppt().getToStringAll());
    }

    @Test
    public void syntaxAnalysis() {
        ParseResult parse = proc.parse("hello world !", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<AsmSimple> result = proc.syntaxAnalysis(parse.getSppt(), null);

        Assert.assertNotNull(result.getAsm());
        System.out.println(result.getAsm());
    }

    @Test
    public void semanticAnalysis() {
        ParseResult parse = proc.parse("hello world !", null);
        Assert.assertNotNull(parse.getSppt());
        SyntaxAnalysisResult<AsmSimple> synt = proc.syntaxAnalysis(parse.getSppt(), null);
        Assert.assertNotNull(synt.getAsm());
        SemanticAnalysisResult result = proc.semanticAnalysis(synt.getAsm(), null);

        Assert.assertEquals(0, result.getIssues().size());
    }

    @Test
    public void process_noOptions() {
        ProcessResult<AsmSimple> result = proc.process("hello world !", null);
        Assert.assertNotNull(result.getAsm());
        Assert.assertNotNull(result.getIssues());
    }

    @Test
    public void process_defaultOptions() {
        ProcessOptions<AsmSimple, ContextSimple> options = proc.optionsDefault();
        options.getParse().setGoalRuleName("H");
        ProcessResult<AsmSimple> result = proc.process("world !", options);
        Assert.assertNotNull(result.getAsm());
        Assert.assertNotNull(result.getIssues());
    }
}
