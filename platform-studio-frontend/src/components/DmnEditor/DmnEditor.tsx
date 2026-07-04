import { useEffect, useRef, useImperativeHandle, forwardRef } from 'react';
import DmnJS from 'dmn-js/lib/Modeler';
import 'dmn-js/dist/assets/diagram-js.css';
import 'dmn-js/dist/assets/dmn-js-shared.css';
import 'dmn-js/dist/assets/dmn-js-drd.css';
import 'dmn-js/dist/assets/dmn-js-decision-table.css';
import 'dmn-js/dist/assets/dmn-js-literal-expression.css';
import 'dmn-js/dist/assets/dmn-font/css/dmn-embedded.css';

const EMPTY_DMN = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
  xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
  xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/"
  id="Definitions_1" name="DRD" namespace="http://camunda.org/schema/1.0/dmn">
  <decision id="decision_1" name="Decision 1">
    <decisionTable id="decisionTable_1" hitPolicy="UNIQUE">
      <input id="input_1" label="Input">
        <inputExpression id="inputExpression_1" typeRef="string" />
      </input>
      <output id="output_1" label="Output" name="output" typeRef="string" />
    </decisionTable>
  </decision>
  <dmndi:DMNDI>
    <dmndi:DMNDiagram>
      <dmndi:DMNShape dmnElementRef="decision_1">
        <dc:Bounds height="80" width="180" x="160" y="100" />
      </dmndi:DMNShape>
    </dmndi:DMNDiagram>
  </dmndi:DMNDI>
</definitions>`;

export interface DmnEditorHandle {
  getXml: () => Promise<string>;
}

interface Props {
  initialXml?: string | null;
  onDirty?: () => void;
}

const DmnEditor = forwardRef<DmnEditorHandle, Props>(({ initialXml, onDirty }, ref) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<InstanceType<typeof DmnJS> | null>(null);

  useImperativeHandle(ref, () => ({
    getXml: async () => {
      if (!modelerRef.current) return '';
      const { xml } = await modelerRef.current.saveXML({ format: true });
      return xml ?? '';
    },
  }));

  useEffect(() => {
    if (!containerRef.current) return;
    const modeler = new DmnJS({ container: containerRef.current });
    modelerRef.current = modeler;

    modeler.importXML(initialXml ?? EMPTY_DMN).catch(console.error);
    modeler.on('commandStack.changed', () => onDirty?.());

    return () => {
      modeler.destroy();
      modelerRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (modelerRef.current && initialXml) {
      modelerRef.current.importXML(initialXml).catch(console.error);
    }
  }, [initialXml]);

  return (
    <div
      ref={containerRef}
      className="dmn-container"
      style={{ width: '100%', height: '100%', minHeight: 500 }}
    />
  );
});

DmnEditor.displayName = 'DmnEditor';
export default DmnEditor;
