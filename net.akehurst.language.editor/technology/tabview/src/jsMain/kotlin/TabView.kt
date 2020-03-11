package net.akehurst.language.editor.technology.tabview

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.asList
import org.w3c.dom.events.Event

class TabView(
        val element: Element
) {

    companion object {
        fun initialise(document: Document) {
            document.querySelectorAll("tabview").asList().forEach { tabview ->
                TabView(tabview as Element)
            }
        }
    }

    val document: Document get() = element.ownerDocument!!

    init {
        val nav = document.createElement("tab-nav");
        element.insertBefore(nav, element.firstElementChild);
        element.querySelectorAll(":scope > tab").asList().filterIsInstance<Element>().forEach { tab ->
            val header = document.createElement("tab-header")
            nav.appendChild(header)
            header.textContent = tab.getAttribute("id")
            header.addEventListener("click", { e: Event ->
                this.tabSelect(header, tab)
            })
        }
    }

    fun tabSelect(header: Element, tab: Element) {
        this.element.querySelectorAll(":scope > tab-nav > tab-header").asList().filterIsInstance<Element>().forEach { e ->
            e.classList.remove("tab-active")
        }
        this.element.querySelectorAll(":scope > tab").asList().filterIsInstance<Element>().forEach { e ->
            e.setAttribute("style", "display : none")
        }
        header.classList.add("tab-active")
        tab.setAttribute("style", "display : grid")
    }
}

