import {Component, Input, Output, EventEmitter, OnInit, ElementRef} from '@angular/core';

import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;

import {AglEditorAce} from 'net.akehurst.language.editor-agl-ace';

@Component({
  selector: 'agl-ace-editor',
  templateUrl: './agl-ace-editor.component.html',
  styleUrls: ['./agl-ace-editor.component.css']
})
export class AglAceEditorComponent implements OnInit {

  @Input() editorIdentity: string;
  @Input() languageIdentity: string;

  @Input() goalRule: string;

  @Input() text: string;
  @Output() textChanged: EventEmitter<any> = new EventEmitter<any>();

  @Input() options: any; //Ace Options

  private _processor: agl.api.processor.LanguageProcessor = null;
  @Input() get processor(): agl.api.processor.LanguageProcessor {
    return this._processor;
  }

  set processor(value: agl.api.processor.LanguageProcessor) {
    this._processor = value;
    if (this.aglEditor) {
      this.aglEditor.processor = this._processor;
    }
  }

  @Input() textStyle: string; // css for styling the processed text in the ace editor

  @Output() click: EventEmitter<any> = new EventEmitter<any>();


  private aglEditor: AglEditorAce;


  constructor(
    private readonly elementRef: ElementRef
  ) {
  }

  ngOnInit(): void {
    this.aglEditor = new AglEditorAce(this.elementRef.nativeElement, this.editorIdentity, this.languageIdentity, this.goalRule, this.text, this.options);
    this.aglEditor.processor = this._processor;
    this.aglEditor.setStyle( this.textStyle );
    this.aglEditor.editor.on('input', (e: Event) => {
      this.textChanged.emit( this.aglEditor.editor.getValue() )
    });
  }

}
