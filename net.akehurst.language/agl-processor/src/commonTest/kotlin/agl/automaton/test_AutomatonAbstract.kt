/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

internal abstract class test_AutomatonAbstract : test_AutomatonUtilsAbstract() {

    fun <T1, T2, T3> List<Triple<T1, T2, T3>>.testAll(f: (arg1: T1, arg2: T2, arg3: T3) -> Unit) {
        for (data in this) {
            f.invoke(data.first, data.second, data.third)
        }
    }

    fun <T> List<List<T>>.testAll(f: (arg1: List<T>) -> Unit) {
        for (data in this) {
            f.invoke(data)
        }
    }

}