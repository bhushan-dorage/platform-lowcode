package com.platform.studio.artifact.messaging;

/** A single BPMN process definition's XML content, resolved from the git artifact store before publishing. */
public record BpmnResource(String name, String content) {}
