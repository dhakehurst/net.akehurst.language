/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.ogl.runtime.structure

enum class RuntimeRuleItemKind {
    EMPTY,                       // n : ;
    CHOICE_EQUAL,                      // n : a | b | c ;
    CHOICE_PRIORITY,             // n : a < b < c ;
    CONCATENATION,               // n : a b c ;
    UNORDERED,                   // n : a & b & c ;                           TODO: support this
    MULTI,                       // n : a? ; , n : a* ; n : a+ ; ,            TODO: n : a[0..5]
    SEPARATED_LIST,              // n : [ a / ',' ]* ;  n : [ a / ',' ]+ ;    TODO: n : [ a / ',' ][0..6] ;
    LEFT_ASSOCIATIVE_LIST,       // n : [ a < '+' ]* ;  n : [ a < ',' ]+ ;    TODO: support this
    RIGHT_ASSOCIATIVE_LIST,      // n : [ a > '-' ]* ;  n : [ a > ',' ]+ ;    TODO: support this
}