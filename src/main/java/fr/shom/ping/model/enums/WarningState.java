package fr.shom.ping.model.enums;

/**
 * Enum values order is important. We use enum ordinal value to order database result depending on computed warning state :
 * - warningStatus = PUBLISHED => warningState order is TO_REVIEW, OUTDATED, END_OF_BROADCAST, ACTIVE
 * - warningStatus = DRAFT => warningState order is DAY_OF_PUBLICATION, TO_REVIEW
 * - warningStatus = CANCELLED => warningState order doesn't matter
 */
public enum WarningState {
    DAY_OF_PUBLICATION,
    TO_REVIEW,
    OUTDATED,
    END_OF_BROADCAST,
    ACTIVE
}
