package com.platform.studio.artifact.domain;

public enum ArtifactType {
    BPMN, DMN, FORM, DATA_MODEL, RULE_SET;

    public String fileExtension() {
        return switch (this) {
            case BPMN -> ".bpmn";
            case DMN -> ".dmn";
            case FORM, DATA_MODEL -> ".json";
            case RULE_SET -> ".drl";
        };
    }

    public String dirName() {
        return name().toLowerCase().replace('_', '-');
    }
}
