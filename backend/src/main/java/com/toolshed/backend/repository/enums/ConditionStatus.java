package com.toolshed.backend.repository.enums;

/**
 * Represents the condition status of a tool at the end of a rental.
 */
public enum ConditionStatus {
    OK, // Tool returned in perfect condition
    USED, // Normal wear and tear
    MINOR_DAMAGE, // Small damage requiring repair
    BROKEN, // Tool is broken/unusable
    MISSING_PARTS // Parts are missing from the tool
}
