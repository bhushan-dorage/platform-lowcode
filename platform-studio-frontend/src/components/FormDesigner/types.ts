export type FieldType =
  | 'text'
  | 'number'
  | 'date'
  | 'select'
  | 'checkbox'
  | 'textarea'
  | 'email'
  | 'section';

export interface FieldDefinition {
  id: string;
  type: FieldType;
  label: string;
  name: string;
  placeholder?: string;
  required?: boolean;
  options?: string[];       // for select fields
  rows?: number;            // for textarea
  children?: FieldDefinition[]; // for section
}

export const FIELD_DRAG_TYPE = 'FORM_FIELD';

export const PALETTE_FIELDS: { type: FieldType; label: string; icon: string }[] = [
  { type: 'text', label: 'Text Input', icon: 'T' },
  { type: 'number', label: 'Number', icon: '#' },
  { type: 'email', label: 'Email', icon: '@' },
  { type: 'date', label: 'Date Picker', icon: '📅' },
  { type: 'select', label: 'Dropdown', icon: '▼' },
  { type: 'checkbox', label: 'Checkbox', icon: '☑' },
  { type: 'textarea', label: 'Text Area', icon: '≡' },
  { type: 'section', label: 'Section', icon: '▬' },
];
