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
describe('test_Agl', function () {

    it('getBuildStamp', function () {
        let actual = Agl.buildStamp.substring(0,4);
        let expected = '2023';
        assert.equal(actual, expected);
    });

    it('processorFromString', function () {
        let grammarStr=`
            namespace test
            grammar Test {
              skip WS="\\s+";
              S='hello' 'world' '!';
            }
        `;
        let proc = Agl.processorFromString(grammarStr);
        assert.notEqual(null, proc);
    });

});
