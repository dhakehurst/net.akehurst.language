import { TestBed } from '@angular/core/testing';

import { AglAceEditorService } from './agl-ace-editor.service';

describe('AglAceEditorService', () => {
  let service: AglAceEditorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AglAceEditorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
