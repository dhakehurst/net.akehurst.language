package net.akehurst.language.comparisons.iguana

import org.iguana.iggy.*
import iguana.utils.input.Input
import iguana.utils.io.FileUtils
import org.iguana.grammar.Grammar
import org.iguana.grammar.symbol.Nonterminal
import org.iguana.grammar.symbol.Rule
import org.iguana.grammar.symbol.Symbol
import org.iguana.grammar.transformation.DesugarPrecedenceAndAssociativity
import org.iguana.grammar.transformation.DesugarStartSymbol
import org.iguana.grammar.transformation.EBNFToBNF
import org.iguana.grammar.transformation.LayoutWeaver
import org.iguana.parser.IguanaParser
import org.iguana.parsetree.*
import org.iguana.util.serialization.JsonSerializer
import java.io.IOException
import java.io.InputStream
import java.util.function.Consumer


object Iggy {

    fun iggyGrammar(): Grammar {
        return try {
            val content = FileUtils.readFile(IggyParser::class.java.getResourceAsStream("/iggy.json"))
            JsonSerializer.deserialize(content, Grammar::class.java)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getGrammar(source: InputStream): Grammar {
        val sourceStr = String( source.readBytes() )
        val input: Input = Input.fromString(sourceStr)
        val iggyGrammar: Grammar = iggyGrammar()
        val parser = IguanaParser(iggyGrammar)
        val parseTree: ParseTreeNode = parser.getParserTree(input) ?: throw RuntimeException("Parse error")
        val parseTreeVisitor: ParseTreeVisitor = object : ParseTreeVisitor {
            override fun visitNonterminalNode(node: org.iguana.parsetree.NonterminalNode): Any? {
                when (node.name) {
                    "Definition" -> {
                        val builder: Grammar.Builder = Grammar.builder()
                        val rules = node.children().get(0) as List<Rule>
                        rules.forEach{r -> builder.addRule(r) }
                        return builder.build()
                    }
                    "Rule" -> when (node.grammarDefinition.label) {
                        "Syntax" -> {
                            val head: Nonterminal = node.getChildWithName("NontName") as Nonterminal
                            val tag: Any = node.getChildWithName("Tag?").accept(this)
                            val body = node.getChildWithName("Body").accept(this) as List<Symbol>
                            return Rule.Builder(head).addSymbols(body).build()
                        }
                    }
                    "Identifier" -> return input.subString(node.getStart(), node.getEnd())
                }
                throw RuntimeException("Should not reach here")
            }

            override fun visitAmbiguityNode(node: AmbiguityNode?): Any? {
                return null
            }

            override fun visitTerminalNode(node: TerminalNode?): Any? {
                return null
            }

            override fun visitMetaSymbolNode(node: MetaSymbolNode?): Any? {
                return null
            }
        }
        var grammar: Grammar = parseTree.accept(parseTreeVisitor) as Grammar
        val precedenceAndAssociativity = DesugarPrecedenceAndAssociativity()
        precedenceAndAssociativity.setOP2()
        //        grammar = precedenceAndAssociativity.transform(grammar);
        grammar = EBNFToBNF().transform(grammar)
        grammar = LayoutWeaver().transform(grammar)
        grammar = DesugarStartSymbol().transform(grammar)
        return grammar
    }

}