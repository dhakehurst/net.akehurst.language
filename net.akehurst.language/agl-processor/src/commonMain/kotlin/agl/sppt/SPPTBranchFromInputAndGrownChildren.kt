package agl.sppt

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.graph.GrowingChildren
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.structure.RuleOptionId
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.sppt.SPPTNodeFromInputAbstract
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*

class SPPTBranchFromInputAndGrownChildren(
        input: InputFromString,
        runtimeRule: RuntimeRule,
        option: Int,
        startPosition: Int,               // can't use children.first.startPosition, there may not be any children
        nextInputPosition: Int,          // don't use children.sumBy { it.matchedTextLength }, it requires unwanted iteration
        priority: Int
) : SPPTNodeFromInputAbstract(input, runtimeRule, option, startPosition, nextInputPosition, priority), SPPTBranch {

    // option -> children
    internal var grownChildrenAlternatives = mutableMapOf<Int, GrowingChildren>()


    // --- SPPTBranch ---

    override val childrenAlternatives: Set<List<SPPTNode>>
        get() = this.grownChildrenAlternatives.entries.map {
            it.value[RuleOptionId(this.runtimeRule, it.key)]
        }.toSet()

    override val children: List<SPPTNode> get() = this.childrenAlternatives.first()

    override val nonSkipChildren: List<SPPTNode> by lazy { //TODO: maybe not use lazy
        this.children.filter { !it.isSkip }
    }

    override val branchNonSkipChildren: List<SPPTBranch> by lazy { //TODO: maybe not use lazy
        this.children.filter { it.isBranch && !it.isSkip }.filterIsInstance<SPPTBranch>()
    }

    override fun nonSkipChild(index: Int): SPPTNode = this.nonSkipChildren[index]

    override fun branchChild(index: Int): SPPTBranch = this.branchNonSkipChildren[index]

    // --- SPPTNode ---
    override val matchedText: String by lazy { //TODO: maybe not use lazy
        this.children.joinToString(separator = "") { it.matchedText }
    }

    override val nonSkipMatchedText: String get() = this.nonSkipChildren.map { it.nonSkipMatchedText }.joinToString("")

    override fun contains(other: SPPTNode): Boolean {
        if (other is SPPTBranch) {
            if (this.identity == other.identity) {
                // for each alternative list of other children, check there is a matching list
                // of children in this alternative children
                var allOthersAreContained = true // if no other children alternatives contain is a match
                for (otherChildren in other.childrenAlternatives) {
                    // for each of this alternative children, find one that 'contains' otherChildren
                    var foundContainMatch = false
                    for (thisChildren in this.childrenAlternatives) {
                        if (thisChildren.size == otherChildren.size) {
                            // for each pair of nodes, one from each of otherChildren thisChildren
                            // check thisChildrenNode contains otherChildrenNode
                            var thisMatch = true
                            for (i in 0 until thisChildren.size) {
                                val thisChildrenNode = thisChildren.get(i)
                                val otherChildrenNode = otherChildren.get(i)
                                thisMatch = thisMatch && thisChildrenNode.contains(otherChildrenNode)
                            }
                            if (thisMatch) {
                                foundContainMatch = true
                                break
                            } else {
                                // if thisChildren alternative doesn't contain, try the next one
                                continue
                            }
                        } else {
                            // if sizes don't match check next in set of this alternative children
                            continue
                        }
                    }
                    allOthersAreContained = allOthersAreContained && foundContainMatch
                }
                return allOthersAreContained
            } else {
                // if identities don't match
                return false
            }

        } else {
            // if other is not a branch
            return false
        }
    }

    override val isEmptyLeaf: Boolean get() = false

    override val isLeaf: Boolean get() = false

    override val isBranch: Boolean get() = true

    override val asLeaf: SPPTLeaf get() = throw SPPTException("Not a Leaf", null)

    override val asBranch: SPPTBranch get() = this

    //override val lastLocation get() = if (children.isEmpty()) this.location else children.last().lastLocation

    override val lastLeaf: SPPTLeaf get() = children.last().lastLeaf

    // --- Object ---
    override fun toString(): String {
        val tag = if (null == this.embeddedIn) this.runtimeRule.tag else "${embeddedIn}.${runtimeRule.tag}"
        var r = ""
        r += this.startPosition.toString() + ","
        r += this.nextInputPosition
        r += ":" + tag + "(" + this.runtimeRule.number + ")"
        return r
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SPPTBranch) {
            return false
        } else {
            return if (this.identity != other.identity) {
                false
            } else {
                this.contains(other) && other.contains(this)
            }
        }
    }
}