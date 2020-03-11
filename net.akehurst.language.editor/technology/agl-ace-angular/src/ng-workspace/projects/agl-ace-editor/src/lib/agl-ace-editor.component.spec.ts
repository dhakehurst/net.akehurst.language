import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AglAceEditorComponent } from './agl-ace-editor.component';

describe('AglAceEditorComponent', () => {
  let component: AglAceEditorComponent;
  let fixture: ComponentFixture<AglAceEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AglAceEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AglAceEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
