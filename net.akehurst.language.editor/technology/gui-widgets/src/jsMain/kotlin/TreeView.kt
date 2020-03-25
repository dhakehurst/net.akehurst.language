package net.akehurst.language.editor.technology.gui.widgets

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.asList
import org.w3c.dom.events.Event

class TreeViewFunctions<T>(
        val label: (node: T) -> String,
        val hasChildren: (node: T) -> Boolean,
        val children: (node: T) -> Array<T>
)

class TreeView(
        val element: Element
) {

    companion object {
        fun initialise(document: Document): Map<String, TreeView> {
            val map = mutableMapOf<String, TreeView>()
            document.querySelectorAll("treeview").asList().forEach { el ->
                val treeview = el as Element
                val id = treeview.getAttribute("id") as String
                val tv = TreeView(treeview)
                map[id] = tv
            }
            return map
        }
    }

    val document: Document get() = element.ownerDocument!!

    private var _loadingElement:Element = this.document.createElement("div")
    private var _loading = false
    var loading
        get() = this._loading
        set(value) {
            this._loading = value
            this.showLoading(value)
        }

    var treeFunctions: TreeViewFunctions<Any>? = null

    var root: Any?
        get() = null
        set(value) {
            while (null != this.element.firstChild) {
                this.element.removeChild(this.element.firstChild!!)
            }
            if (null == value) {
            } else {
                this.addNode(this.element, value)
            }
        }

    init {
        this._loadingElement.setAttribute("class", "treeview-loading")
    }

    fun addNode(parentElement: Element, node: Any) {
        if (this.treeFunctions!!.hasChildren(node)) {
            val branchEl = document.createElement("treeview-branch")
            parentElement.append(branchEl)
            this.setLabel(branchEl, this.treeFunctions!!.label(node))
            val childrenEl = document.createElement("treeview-children")
            branchEl.append(childrenEl)
            branchEl.addEventListener("click", {
                it.stopPropagation()
                if (null == branchEl.getAttribute("open")) {
                    branchEl.setAttribute("open", "true")
                } else {
                    branchEl.removeAttribute("open")
                }
                if (null == childrenEl.firstChild) {
                    val children = this.treeFunctions!!.children(node)
                    children.forEach {
                        this.addNode(childrenEl, it)
                    }
                }
            })
        } else {
            val leafEl = document.createElement("treeview-leaf")
            parentElement.append(leafEl)
            this.setLabel(leafEl, this.treeFunctions!!.label(node))
            leafEl.addEventListener("click", { it.stopPropagation() })
        }
    }

    fun setLabel(nodeElement: Element, label: String) {
        val span = document.createElement("span");
        span.append(label);
        nodeElement.appendChild(span)
    }

    private fun showLoading(visible:Boolean) {
        while (null != this.element.firstChild) {
            this.element.removeChild(this.element.firstChild!!)
        }
        if (visible) {
            this.element.appendChild(this._loadingElement)
        }
    }
}

