export class TabView {

    static initialise() {
        document.querySelectorAll('tabview').forEach((tabview) => {
            new TabView(tabview);
        });
    }

    constructor( tabview) {
        this.tabview = tabview;
        let nav = document.createElement("tab-nav");
        tabview.insertBefore(nav, tabview.firstElementChild);
        tabview.querySelectorAll(':scope > tab').forEach((tab) => {
            let header = document.createElement('tab-header');
            nav.appendChild(header);
            header.textContent = tab.getAttribute('id');
            header.onclick = e => this.tabSelect(tab);
        });
    }

    tabSelect(tab) {
        this.tabview.querySelectorAll(':scope > tab').forEach((e) => e.style.display = 'none');
        tab.style.display = 'grid';
    }
}