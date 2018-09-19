package net.akehurst.language.processor;

import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.processor.LanguageProcessor;
import org.junit.Assert;
import org.junit.Test;


import java.util.Arrays;
import java.util.List;

public class test_LanguageProcessor_Java {

    @Test
    public void parser_rules_String() {
        final LanguageProcessor lp = Ogl.processor("namespace test grammar Test { a = 'a'; }");
        lp.parse("a", "a");
    }

    @Test
    public void parser_rules_List_fails() {
        try {
            List<String> rules = Arrays.asList("a = 'a'");
            final LanguageProcessor lp = Ogl.processor(rules);

            lp.parse("a", "a");
            Assert.fail("This test should throw an exception");
        } catch (Throwable t) {
            if (t instanceof ParseFailedException) {
                ParseFailedException e = (ParseFailedException)t;
                Assert.assertEquals(1, e.getLocation().get("line").intValue());
                Assert.assertEquals(7, e.getLocation().get("column").intValue());
            }
        }
    }

    @Test
    public void parser_rules_List_fails1() {
        try {
            List<String> rules = Arrays.asList("!");
            final LanguageProcessor lp = Ogl.processor(rules);

            lp.parse("a", "a");
            Assert.fail("This test should throw an exception");
        } catch (Throwable t) {
            if (t instanceof ParseFailedException) {
                ParseFailedException e = (ParseFailedException)t;
                Assert.assertEquals(1, e.getLocation().get("line").intValue());
                Assert.assertEquals(0, e.getLocation().get("column").intValue());
            }
        }
    }
    @Test
    public void parser_rules_List_fails2() {
        try {
            List<String> rules = Arrays.asList("a!");
            final LanguageProcessor lp = Ogl.processor(rules);

            lp.parse("a", "a");
            Assert.fail("This test should throw an exception");
        } catch (Throwable t) {
            if (t instanceof ParseFailedException) {
                ParseFailedException e = (ParseFailedException)t;
                Assert.assertEquals(1, e.getLocation().get("line").intValue());
                Assert.assertEquals(1, e.getLocation().get("column").intValue());
            }
        }
    }
}
