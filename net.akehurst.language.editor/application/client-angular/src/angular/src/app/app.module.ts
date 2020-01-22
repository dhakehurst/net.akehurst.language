import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import {TabViewModule} from 'primeng/tabview';
import {TreeModule} from 'primeng/tree';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {AGlEditorComponent} from "./agl-editor/agl-editor.component";

@NgModule({
  declarations: [
    AppComponent,
    AGlEditorComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,

    TabViewModule,
    TreeModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
