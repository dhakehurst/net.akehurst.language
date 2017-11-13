package net.akehurst.language.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.ParseTreeToInputText;
import net.akehurst.language.ogl.semanticStructure.Grammar;

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

	static ILanguageProcessor xmlProcessor;

	static {
		Xml_Tests.xmlProcessor = Xml_Tests.getXmlProcessor();
	}

	static ILanguageProcessor getXmlProcessor() {
		if (null == Xml_Tests.xmlProcessor) {
			try {
				final FileReader reader = new FileReader(Paths.get("src/test/resources/Xml.og").toFile());
				final Grammar grammar = Xml_Tests.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				final LanguageProcessor proc = new LanguageProcessor(grammar, null);
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

	static ISharedPackedParseTree parse(final String goalName, final String input) {
		try {
			final IParser parser = Xml_Tests.getXmlProcessor().getParser();
			final ISharedPackedParseTree tree = parser.parse(goalName, new StringReader(input));
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
		final ISharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElement() {

		final String input = "<xxx />";
		final ISharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElementAttribute1() {

		final String input = "<xxx aa='1' />";
		final ISharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void emptyElementAttribute2() {

		final String input = "<xxx aa='1' bb='2' />";
		final ISharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void element() {

		final String input = "<xxx> </xxx>";
		final ISharedPackedParseTree tree = Xml_Tests.parse("file", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
}