import { useEffect, useRef, useImperativeHandle, forwardRef } from 'react';
import BpmnJS from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="156" y="81" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

export interface BpmnModelerHandle {
  getXml: () => Promise<string>;
}

interface Props {
  initialXml?: string | null;
  onDirty?: () => void;
}

const BpmnModeler = forwardRef<BpmnModelerHandle, Props>(({ initialXml, onDirty }, ref) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<InstanceType<typeof BpmnJS> | null>(null);

  useImperativeHandle(ref, () => ({
    getXml: async () => {
      if (!modelerRef.current) return '';
      const { xml } = await modelerRef.current.saveXML({ format: true });
      return xml ?? '';
    },
  }));

  useEffect(() => {
    if (!containerRef.current) return;

    const modeler = new BpmnJS({ container: containerRef.current });
    modelerRef.current = modeler;

    const xml = initialXml ?? EMPTY_BPMN;
    modeler.importXML(xml).catch(console.error);

    modeler.on('commandStack.changed', () => onDirty?.());

    return () => {
      modeler.destroy();
      modelerRef.current = null;
    };
  }, []); // mount once

  // re-import when initialXml changes (opening a different artifact)
  useEffect(() => {
    if (modelerRef.current && initialXml) {
      modelerRef.current.importXML(initialXml).catch(console.error);
    }
  }, [initialXml]);

  return (
    <div
      ref={containerRef}
      className="bpmn-container"
      style={{ width: '100%', height: '100%', minHeight: 500 }}
    />
  );
});

BpmnModeler.displayName = 'BpmnModeler';
export default BpmnModeler;
