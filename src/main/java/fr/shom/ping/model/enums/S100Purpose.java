package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100Purpose {
    NEW_DATASET("newDataset"),
    NEW_EDITION("newEdition"),
    UPDATE("update"),
    REISSUE("reissue"),
    CANCELLATION("cancellation"),
    DELTA("delta");

    private final String value;
}
