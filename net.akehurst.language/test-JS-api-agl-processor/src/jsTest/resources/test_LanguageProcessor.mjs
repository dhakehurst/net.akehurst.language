/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import {
    assertTrue1upg56ex908m as assertTrue,
    suitenlt29rr40l1o as suite,
    test3806p0uwinskq as test,
} from './kotlin-kotlin-test.mjs';

import {Agl} from './net.akehurst.language-agl-processor.mjs';

function test_LanguageProcessor() {
    suite('test_LanguageProcessor', false, function () {
        return () => {
            let grammarStr = `
            namespace test
            grammar Test {
              skip WS="\\s+" ;
              S= H W ;
              H = 'hello' ;
              W = 'world' '!' ;
            }
        `;
            let pr = Agl.processorFromStringSimple(grammarStr);
            let proc = pr.processor;

            test('scan', function () {
                return () => {
                    let result = proc.scan('hello world !');

                    assert.notEqual(null, result.tokens);
                    assert.equal(5, result.tokens.toArray().length);
                    assert.notEqual('hello', result.tokens.toArray()[0]);
                }
            });

            test('parse_noOptions', function () {
                return () => {
                    let result = proc.parse('hello world !');

                    assert.notEqual(null, result.sppt);
                    console.log(result.sppt.toStringAll);
                }
            });

            test('parse_defaultOptions', function () {
                return () => {
                    let options = proc.parseOptionsDefault();
                    options.goalRuleName = "W";
                    let result = proc.parse("world !", options);

                    assert.notEqual(null, result.sppt);
                    console.log(result.sppt.toStringAll);
                }
            });

            test('parse_buildOptions', function () {
                return () => {
                    let result = proc.parse("world !", Agl.parseOptions(b => {
                        b.goalRuleName("W");
                    }));

                    assert.notEqual(null, result.sppt);
                    console.log(result.sppt.toStringAll);
                }
            });

            test('syntaxAnalysis', function () {
                return () => {
                    let parse = proc.parse('hello world !');
                    assert.notEqual(null, parse.sppt);
                    let result = proc.syntaxAnalysis(parse.sppt);

                    assert.notEqual(null, result.asm);
                    console.log(result.asm);
                }
            });

            test('semanticAnalysis', function () {
                return () => {
                    let parse = proc.parse('hello world !');
                    assert.notEqual(null, parse.sppt);
                    let synt = proc.syntaxAnalysis(parse.sppt);
                    assert.notEqual(null, synt.asm);

                    let result = proc.semanticAnalysis(synt.asm);

                    assert.notEqual(0, result.issues.size);
                }
            });

            test('process_noOptions', function () {
                return () => {
                    let result = proc.process('hello world !');

                    assert.notEqual(null, result.asm);
                    assert.notEqual(null, result.issues);
                }
            });

            test('process_defaultOptions', function () {
                return () => {
                    let options = proc.optionsDefault();
                    options.parse.goalRuleName = "W"

                    let result = proc.process('world !', options);

                    assert.notEqual(null, result.asm);
                    assert.notEqual(null, result.issues);
                }
            });
        }
    });
}

export {
    test_LanguageProcessor
};