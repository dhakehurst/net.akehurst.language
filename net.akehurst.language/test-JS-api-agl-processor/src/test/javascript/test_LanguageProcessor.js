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

let assert = require('assert');
let Agl = require('./net.akehurst.language-agl-processor').net.akehurst.language.agl.processor.Agl;
describe('test_LanguageProcessor', function () {

    let grammarStr=`
            namespace test
            grammar Test {
              skip WS="\\s+" ;
              S= H W ;
              H = 'hello' ;
              W = 'world' '!' ;
            }
        `;
    let proc = Agl.processorFromString(grammarStr);

    it('scan', function () {
        let result = proc.scan('hello world !');

        assert.notEqual(null, result);
        assert.equal(5, result.toArray().length);
        assert.notEqual('hello', result.toArray()[0]);
    });

    it('parse_noOptions', function () {
        let result = proc.parse('hello world !');

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('parse_defaultOptions', function () {
        let options = proc.parseOptionsDefault();
        options.goalRuleName="W";
        let result = proc.parse("world !", options);

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('parse_buildOptions', function () {
        let result = proc.parse("world !", Agl.parseOptions(b => {
            b.goalRuleName("W");
        }));

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('syntaxAnalysis', function () {
        let parse = proc.parse('hello world !');
        assert.notEqual(null, parse.sppt);
        let result = proc.syntaxAnalysis(parse.sppt);

        assert.notEqual(null, result.asm);
        console.log( result.asm );
    });

    it('semanticAnalysis', function () {
        let parse = proc.parse('hello world !');
        assert.notEqual(null, parse.sppt);
        let synt = proc.syntaxAnalysis(parse.sppt);
        assert.notEqual(null, synt.asm);

        let result = proc.semanticAnalysis(synt.asm);

        assert.notEqual(0, result.issues.size);
    });

    it('process_noOptions', function () {
        let result = proc.process('hello world !');

        assert.notEqual(null, result.asm);
        assert.notEqual(null, result.issues);
    });

    it('process_defaultOptions', function () {
        let options = proc.optionsDefault();
        options.parse.goalRuleName = "W"

        let result = proc.process('world !', options);

        assert.notEqual(null, result.asm);
        assert.notEqual(null, result.issues);
    });
});