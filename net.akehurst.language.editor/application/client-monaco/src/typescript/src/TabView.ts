export class TabView {

    static initialise() {
        document.querySelectorAll('tabview').forEach((tabview: HTMLElement) => {
            new TabView(tabview);
        });
    }

    constructor(private tabview: HTMLElement) {
        let nav = document.createElement("tab-nav");
        tabview.insertBefore(nav, tabview.firstElementChild);
        tabview.querySelectorAll(':scope > tab').forEach((tab: HTMLElement) => {
            let header = document.createElement('tab-header');
            nav.appendChild(header);
            header.textContent = tab.getAttribute('id');
            header.onclick = e => this.tabSelect(tab);
        });
    }

    tabSelect(tab: HTMLElement) {
        this.tabview.querySelectorAll(':scope > tab').forEach((e: any) => e.style.display = 'none');
        tab.style.display = 'grid';
    }
}