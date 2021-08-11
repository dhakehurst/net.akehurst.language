/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.processor


enum class AutomatonKind {
    LOOKAHEAD_NONE,     // LC(O) like LR(0)
    LOOKAHEAD_SIMPLE,   // SLC like SLR
    LOOKAHEAD_1         // LC(1) like LR(1)
}

//FIXME: added because currently Kotlin will not 'export' enums to JS
object AutomatonKind_api {
    val LOOKAHEAD_NONE= AutomatonKind.LOOKAHEAD_NONE
    val LOOKAHEAD_SIMPLE= AutomatonKind.LOOKAHEAD_SIMPLE
    val LOOKAHEAD_1= AutomatonKind.LOOKAHEAD_1
}
