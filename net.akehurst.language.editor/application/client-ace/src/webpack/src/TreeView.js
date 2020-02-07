export class TreeView {

    static initialise() {
        document.querySelectorAll('treeview').forEach((treeview) => {
            new TreeView(treeview);
        });
    }

    constructor(treeview) {
        this.treeview = treeview;
        this.root = document.createElement("ul");
    }

    addChild(parent, child) {

    }

}