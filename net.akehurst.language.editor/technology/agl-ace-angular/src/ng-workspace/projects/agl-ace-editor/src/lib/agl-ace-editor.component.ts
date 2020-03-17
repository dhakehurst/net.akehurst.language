import {Component, Input, Output, EventEmitter, OnInit, ElementRef} from '@angular/core';

import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;

import * as agl_ace_js from 'net.akehurst.language.editor-technology-agl-ace/net.akehurst.language.editor-technology-agl-ace.js';
import agl_ace = agl_ace_js.net.akehurst.language.editor.ace;
//import 'net.akehurst.language.editor-technology-agl-ace'
//const agl_ace = window['net.akehurst.language.editor-technology-agl-ace'].net.akehurst.language.editor.ace;

@Component({
  selector: 'agl-ace-editor',
  templateUrl: './agl-ace-editor.component.html',
  styleUrls: ['./agl-ace-editor.component.css']
})
export class AglAceEditorComponent implements OnInit {

  @Input() editorIdentity: string;
  @Input() languageIdentity: string;

  @Input() goalRule: string;

  @Input() get text(): string {
    return this.aglEditor.text;
  }

  private _text : string;

  set text(value: string) {
    if (this._text!=value) {
      this._text = value;  // setter called before ngOnInit
      if (this.aglEditor) {
        this.aglEditor.text = value;
      }
    }
  }

  @Output() textChanged: EventEmitter<any> = new EventEmitter<any>();

  @Input() options: any; //Ace Options

  private _processor: agl.api.processor.LanguageProcessor;
  @Input() get processor(): agl.api.processor.LanguageProcessor {
    return this.aglEditor.agl.processor;
  }

  set processor(value: agl.api.processor.LanguageProcessor) {
    this._processor = value; // setter called before ngOnInit
    if (this.aglEditor) {
      this.aglEditor.agl.processor = value;
    }
  }

  @Input() textStyle: string; // css for styling the processed text in the ace editor

  @Output() click: EventEmitter<any> = new EventEmitter<any>();

  private aglEditor: agl_ace.AglEditorAce;

  constructor(
    private readonly elementRef: ElementRef
  ) {
  }

  ngOnInit(): void {
    this.aglEditor = new agl_ace.AglEditorAce(this.elementRef.nativeElement, this.editorIdentity, this.languageIdentity, this.options);
    this.aglEditor.setStyle(this.textStyle);
    this.aglEditor.agl.processor = this._processor;
    this.aglEditor.agl.goalRule = this.goalRule;
    this.aglEditor.text = this._text;
    this.aglEditor.aceEditor.on('input', (e: Event) => {
      this._text = this.aglEditor.text;
      this.textChanged.emit(this.aglEditor.text)
    });
  }

}
