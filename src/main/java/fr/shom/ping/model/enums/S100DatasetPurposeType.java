package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100DatasetPurposeType {
    BASE("base"),
    UPDATE("update");

    private final String code;
}
