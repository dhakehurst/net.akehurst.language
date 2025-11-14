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

function test_Agl() {
    suite('test_Agl', false, function () {
        return () => {
            test('getBuildStamp', false, function () {
                return () => {
                    let actual = Agl.buildStamp.substring(0, 4);
                    let expected = '2023';
                    assert.equal(actual, expected);
                }
            });

            test('processorFromString', false, function () {
                return () => {
                    let grammarStr = `
                    namespace test
                    grammar Test {
                      skip WS="\\s+";
                      S='hello' 'world' '!';
                    }
                `;
                    let proc = Agl.processorFromStringSimple(grammarStr);
                    assert.notEqual(null, proc);
                }
            });
        }
    });
}

export {
    test_Agl
};