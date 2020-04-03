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

package net.akehurst.language.agl.sppt

import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor

class CountTreesVisitor : SharedPackedParseTreeVisitor<Int, Unit> {

    override fun visit(target: SharedPackedParseTree, arg: Unit): Int {
        return target.root.accept(this, arg)
    }


    override fun visit(target: SPPTLeaf, arg: Unit): Int {
        return 1
    }


    override fun visit(target: SPPTBranch, arg: Unit): Int {
        var currentCount: Int = 0
        for (children in target.childrenAlternatives) {
            if (children.isEmpty()) {
                currentCount += 1
            } else {
                var max = 0
                for (i in 0 until children.size) {
                    max = maxOf(max, children[i].accept(this, arg))
                }
                currentCount += max
            }
        }
        return currentCount
    }

}
