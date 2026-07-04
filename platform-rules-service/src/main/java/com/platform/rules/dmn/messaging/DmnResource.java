package com.platform.rules.dmn.messaging;

/** A single DMN decision's XML content, resolved by studio-backend before publishing. */
public record DmnResource(String name, String content) {}
