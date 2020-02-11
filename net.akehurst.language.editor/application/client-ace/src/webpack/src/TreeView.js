export class TreeView {

    static initialise() {
        let map = new Map();
        document.querySelectorAll('treeview').forEach((treeview) => {
            let id = treeview.getAttribute('id');
            const tv = new TreeView(treeview);
            map.set(id, tv);
        });
        return map;
    }

    constructor(treeview) {
        this.treeview = treeview;
    }

    setRoot(node, funcs) {
        while(this.treeview.firstChild) {
            this.treeview.removeChild(this.treeview.firstChild);
        }
        this.addNode(funcs, this.treeview, node)
    }
    addNode(funcs, parentElement, node) {
        if (funcs.hasChildren(node)) {
            const branchEl = document.createElement('tree-branch');
            parentElement.appendChild(branchEl);
            this.setLabel(branchEl, funcs.label(node));
            const childrenEl = document.createElement("tree-children");
            branchEl.appendChild(childrenEl);
            const children = funcs.children(node);
            children.forEach( child => {
                this.addNode(funcs, childrenEl, child)
            });
            branchEl.addEventListener('click',e=>{
                e.cancelBubble = true;
                branchEl.getAttribute('open') ? branchEl.removeAttribute('open') : branchEl.setAttribute('open',true)
            });
        } else {
            const leafEl = document.createElement('tree-leaf');
            this.setLabel(leafEl, funcs.label(node));
            parentElement.appendChild(leafEl);
            leafEl.addEventListener('click',e=>{
                e.cancelBubble = true;
            });
        }
    }

    setLabel(nodeElement, label) {
        const span = document.createElement("span");
        span.append(label);
        nodeElement.appendChild(span)
    }

}