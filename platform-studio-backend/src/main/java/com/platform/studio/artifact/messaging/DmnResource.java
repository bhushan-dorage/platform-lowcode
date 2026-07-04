package com.platform.studio.artifact.messaging;

/** A single DMN decision's XML content, resolved from the git artifact store before publishing. */
public record DmnResource(String name, String content) {}
