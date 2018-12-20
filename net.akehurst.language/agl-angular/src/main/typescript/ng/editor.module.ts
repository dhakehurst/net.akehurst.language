import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { AngularResizedEventModule } from 'angular-resize-event';

import { NalEditorComponent } from './editor.component';

@NgModule({
  imports: [
    CommonModule,
    AngularResizedEventModule
  ],
  declarations: [
    NalEditorComponent
  ],
  exports: [
    NalEditorComponent
  ]
})
export class NalEditorModule {

}