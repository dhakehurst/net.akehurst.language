package test;

import kotlin.Pair;
import kotlin.Unit;
import net.akehurst.language.agl.processor.Agl;
import net.akehurst.language.api.processor.*;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class test_Agl {

    private static final String EOL = System.lineSeparator();
    private static final String grammarStr = ""
            + "namespace test" + EOL
            + "grammar Test {" + EOL
            + "  skip WS=\"\\s+\";" + EOL
            + "  S='hello' 'world' '!';" + EOL
            + "}";


    @Test
    public void getBuildStamp() {
        String actual = Agl.INSTANCE.getBuildStamp().substring(0, 4);

        String expected = "2023";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processorFromString_noConfig() {
        LanguageProcessor<Object,Object> proc = Agl.INSTANCE.processorFromString(grammarStr, null, null).getProcessor();

        Assert.assertNotNull(proc);
    }

    @Test
    public void processorFromString_withConfigDefault() {
        LanguageProcessorConfiguration<Object,Object> config = Agl.INSTANCE.configurationDefault();

        LanguageProcessor<Object,Object> proc = Agl.INSTANCE.processorFromString(grammarStr, config, null).getProcessor();

        Assert.assertNotNull(proc);
    }

    @Test
    public void processorFromString_withConfigSet() {
        LanguageProcessorConfiguration<Object,Object> config = Agl.INSTANCE.configuration( KotlinFromJava.toKotlin(b-> {
            b.targetGrammarName("");
            return Unit.INSTANCE;
        }));

        LanguageProcessor<Object,Object> proc = Agl.INSTANCE.processorFromString(grammarStr, config, null).getProcessor();

        Assert.assertNotNull(proc);
    }

}