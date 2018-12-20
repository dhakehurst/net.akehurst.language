import { Component, ViewChild, ElementRef, AfterViewInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { ResizedEvent } from 'angular-resize-event/resized-event';
import * as nal from './Editor'
import * as nalM from './EditorMonaco'

@Component( {
    selector: 'nal-editor',
    template: '<div class="resizable" (resized)="onResized($event)" ><div class="monacoDiv" ></div></div>',
    styleUrls: ['./editor.component.css'],
} )
export class NalEditorComponent implements AfterViewInit, OnDestroy {

    @Input() language: string;

    @Input() editorOptions: any;
    
    @Input() text: string;
    @Output() textChange: EventEmitter<any> = new EventEmitter<any>();

    private editor: nal.Editor;

    constructor(
            private el: ElementRef
    ) {

    }

    ngAfterViewInit() {
        var onGotAmdLoader = () => {
            // Load monaco
            ( <any>window ).require( ['vs/editor/editor.main'], () => {
                this.initMonaco();
            } );
        };

        // Load AMD loader if necessary
        if ( !( <any>window ).require ) {
            var loaderScript = document.createElement( 'script' );
            loaderScript.type = 'text/javascript';
            loaderScript.src = 'vs/loader.js';
            loaderScript.addEventListener( 'load', onGotAmdLoader );
            document.body.appendChild( loaderScript );
        } else {
            onGotAmdLoader();
        }
    }

    // Will be called once monaco library is available
    initMonaco() {
        if (!this.editorOptions) {
            this.editorOptions = {
              language: 'tgql',
              autoClosingBrackets: false,
              codeLens: false,
              contextmenu: false,
              folding: false,
              minimap: {
                  enabled: false
              }
          }
        }
        
        const ed_container = this.el.nativeElement.children[0].children[0];
        this.editor = new nalM.EditorMonaco( ed_container, this.language, this.text, this.editorOptions );
        
        this.editor.onChange((text)=>{
            this.textChange.emit(text);
        });
    }

    onResized( event: ResizedEvent ): void {
        // resize later in case not visible yet!
        setTimeout(() => {
            if ( this.editor ) {
                this.editor.layout();
            }
        }, 200 );
    }

    ngOnDestroy() {
    }

}
