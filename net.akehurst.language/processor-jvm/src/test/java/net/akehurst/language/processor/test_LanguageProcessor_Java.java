package net.akehurst.language.processor;

import net.akehurst.language.api.processor.LanguageProcessor;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class test_LanguageProcessor_Java {

    @Test
    public void parser_rules_String() {
        final LanguageProcessor lp = OglKt.parser("a = 'a'");
        lp.parse("a", "a");
    }

    @Test
    public void parser_rules_List() {
        List<String> rules = Arrays.asList("a = 'a'");
        final LanguageProcessor lp = OglKt.parser(rules);
        lp.parse("a", "a");
    }

}
