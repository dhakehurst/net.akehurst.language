package test;

import kotlin.jvm.functions.Function0;
import net.akehurst.language.agl.Agl;
import net.akehurst.language.agl.default_.ContextAsmDefault;
import net.akehurst.language.agl.processor.LanguageProcessorResult;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.asm.api.Asm;
import net.akehurst.language.parser.api.ParseResult;
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault;
import net.akehurst.language.sppt.api.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class test_ParseAndWalkSppt {

    @Test
    public void parse() {
        String grammarStr = ""
                + "namespace CalculatorModelLanguage"


                + "}";

        LanguageProcessorResult<Asm, ContextAsmDefault> res = Agl.INSTANCE.processorFromStringSimpleJava(
                grammarStr,
                null, null, null, null, null,
                Agl.INSTANCE.configurationDefault(),
                null
        );
        System.out.println(res.getIssues());
        Assert.assertTrue(res.getIssues().getErrors().isEmpty());

        LanguageProcessor<Asm, ContextAsmDefault> proc = res.getProcessor();
        Assert.assertNotNull(proc);

        String sentence = "{ a:false b:1 c:3.141 d:'bob' e:var2 }";
        ParseResult pres = proc.parse(sentence, new ParseOptionsDefault());
        System.out.println(res.getIssues());
        Assert.assertTrue(res.getIssues().getErrors().isEmpty());

        SharedPackedParseTree sppt = pres.getSppt();
        Assert.assertNotNull(sppt);

        SpptWalker walker = new SpptWalker() {

            @Override
            public void beginTree() {
                System.out.println("start of SPPT");
            }

            @Override
            public void endTree() {
                System.out.println("end of SPPT");
            }

            @Override
            public void skip(int startPosition, int nextInputPosition) {
                System.out.println("a skip node: ${startPosition}-${nextInputPosition}");

            }

            @Override
            public void leaf(@NotNull SpptDataNodeInfo nodeInfo) {
                System.out.println("leaf node: ${nodeInfo.node.rule.tag} ${nodeInfo.node.startPosition}-${nodeInfo.node.nextInputPosition}");

            }

            @Override
            public void beginBranch(@NotNull SpptDataNodeInfo nodeInfo) {
                System.out.println("start branch: " + nodeInfo.getNode().getRule().getTag());
            }

            @Override
            public void endBranch(@NotNull SpptDataNodeInfo nodeInfo) {
                System.out.println("end branch: " + nodeInfo.getNode().getRule().getTag());
            }

            @Override
            public void beginEmbedded(@NotNull SpptDataNodeInfo nodeInfo) {
                System.out.println("start embedded");
            }

            @Override
            public void endEmbedded(@NotNull SpptDataNodeInfo nodeInfo) {
                System.out.println("end embedded");
            }

            @Override
            public void error(@NotNull String msg, @NotNull NodeListCallback path) {
                System.out.println(msg);
            }
        };
        sppt.traverseTreeDepthFirst(walker, false);
    }

}