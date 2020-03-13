/*
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 */
package ace

import org.w3c.dom.Element

@JsModule("kotlin-ace-loader!?id=ace/virtual_renderer&name=VirtualRenderer")
@JsNonModule
external class VirtualRenderer(
        container: Element,
        theme: String?
) {

}