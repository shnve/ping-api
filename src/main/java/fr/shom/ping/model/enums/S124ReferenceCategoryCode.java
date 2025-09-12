package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S124ReferenceCategoryCode {
    WARNING_CANCELLATION("1", "Cancellation"),
    WARNING_REFERENCE("2", "Warning Reference"),
    IN_FORCE("3", "In-Force");

    private final String code;
    private final String label;
}
