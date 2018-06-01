/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.core.parser;

import java.io.Reader;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.core.grammar.INodeType;
import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.sppt.SharedPackedParseTree;

public interface Parser {

    /**
     * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
     */
    void build();

    /**
     * not sure this is useful any more ! what is it used for?
     *
     * @return
     */
    Set<INodeType> getNodeTypes();

    /**
     * parse the inputText starting with the given grammar rule and return the shared packed parse Tree.
     *
     * @param goalRuleName
     * @param inputText
     * @return
     * @throws ParseFailedException
     * @throws ParseTreeException
     * @throws GrammarRuleNotFoundException
     */
    SharedPackedParseTree parse(String goalRuleName, CharSequence inputText) throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException;

    SharedPackedParseTree parse(String goalRuleName, Reader inputText) throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException;

    List<IRuleItem> expectedAt(String goalRuleName, CharSequence inputText, int position) throws ParseFailedException, ParseTreeException;

    List<IRuleItem> expectedAt(String goalRuleName, Reader reader, int position) throws ParseFailedException, ParseTreeException;
}
