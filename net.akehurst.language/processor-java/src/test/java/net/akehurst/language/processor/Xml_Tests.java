package net.akehurst.language.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.ParseTreeToInputText;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;

public class Xml_Tests {

	static OGLanguageProcessor processor;

	static {
		Xml_Tests.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Xml_Tests.processor) {
			Xml_Tests.processor = new OGLanguageProcessor();
			Xml_Tests.processor.getParser().build();
		}
		return Xml_Tests.processor;
	}

	static LanguageProcessor xmlProcessor;

	static {
		Xml_Tests.xmlProcessor = Xml_Tests.getXmlProcessor();
	}

	static LanguageProcessor getXmlProcessor() {
		if (null == Xml_Tests.xmlProcessor) {
			try {
				final FileReader reader = new FileReader(Paths.get("src/test/resources/Xml.og").toFile());
				final GrammarDefault grammar = Xml_Tests.getOGLProcessor().process(reader, "grammarDefinition", GrammarDefault.class);
				final LanguageProcessorDefault proc = new LanguageProcessorDefault(grammar, null);
				proc.getParser().build();
				Xml_Tests.xmlProcessor = proc;
			} catch (final IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (final ParseFailedException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (final UnableToAnalyseExeception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		return Xml_Tests.xmlProcessor;
	}

	String toString(final Path file) {
		try {
			final byte[] bytes = Files.readAllBytes(file);
			final String str = new String(bytes);
			return str;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static SharedPackedParseTree parse(final String goalName, final String input) {
		try {
			final Parser parser = Xml_Tests.getXmlProcessor().getParser();
			final SharedPackedParseTree tree = parser.parse(goalName, new StringReader(input));
			return tree;
		} catch (final ParseFailedException e) {
			return null;// e.getLongestMatch();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void emptyFile() {

		final String input = "";
		final SharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElement() {

		final String input = "<xxx />";
		final SharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElementAttribute1() {

		final String input = "<xxx aa='1' />";
		final SharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElementAttribute2() {

		final String input = "<xxx aa='1' bb='2' />";
		final SharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void element() {

		final String input = "<xxx> </xxx>";
		final SharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
}