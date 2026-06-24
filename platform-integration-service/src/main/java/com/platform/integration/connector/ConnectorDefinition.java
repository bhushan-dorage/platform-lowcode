package com.platform.integration.connector;

import java.util.List;
import java.util.Map;

public record ConnectorDefinition(
        ConnectorType type,
        String displayName,
        String description,
        List<ConnectorParam> requiredParams,
        List<ConnectorParam> optionalParams) {

    public record ConnectorParam(String name, String type, String description) {}
}
