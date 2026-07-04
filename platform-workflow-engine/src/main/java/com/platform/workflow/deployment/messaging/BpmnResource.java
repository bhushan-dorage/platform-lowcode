package com.platform.workflow.deployment.messaging;

/** A single BPMN process definition's XML content, resolved by studio-backend before publishing. */
public record BpmnResource(String name, String content) {}
