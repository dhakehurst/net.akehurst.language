package net.akehurst.language.agl.grammar.style

import net.akehurst.language.api.api.style.AglStyle
import net.akehurst.language.api.api.style.AglStyleRule
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.processor.BranchHandler
import net.akehurst.language.processor.SyntaxAnalyserAbstract

class AglStyleSyntaxAnalyser : SyntaxAnalyserAbstract() {

    init {
        this.register("rules", this::rules as BranchHandler<List<AglStyleRule>>)
        this.register("rule", this::rule as BranchHandler<AglStyleRule>)
        this.register("styleList", this::styleList as BranchHandler<List<AglStyle>>)
        this.register("style", this::style as BranchHandler<AglStyle>)

    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return this.transform<T>(sppt.root.asBranch, "")
    }

    //   rules : rule* ;
    fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<AglStyleRule> {
        return children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<AglStyleRule>(it, arg)
        }
    }

    // rule = SELECTOR '{' styleList '}'
    fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyleRule {
        val selector = children[0].nonSkipMatchedText //TODO: ?
        val rule = AglStyleRule(selector)
        rule.styles = this.transform(children[1], arg)
        return rule
    }

    // styleList = style* ;
    fun styleList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?) : List<AglStyle> {
        return children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<AglStyle>(it, arg)
        }
    }

    // style = STYLE_ID ':' STYLE_VALUE ';' ;
    fun style(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyle {
        val name = children[0].nonSkipMatchedText //TODO: ?
        val value = children[1].nonSkipMatchedText //TODO: ?
        return AglStyle(name, value)
    }
}