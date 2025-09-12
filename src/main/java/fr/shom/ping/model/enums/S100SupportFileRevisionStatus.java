package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100SupportFileRevisionStatus {

    NEW("new"),
    REPLACEMENT("replacement"),
    DELETION("deletion");

    private final String value;
}
