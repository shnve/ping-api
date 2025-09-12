package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S100SupportFileFormat {

    ASCII("ASCII"),
    JPEG2000("JPEG2000"),
    HTML("HTML"),
    XML("XML"),
    XSLT("XSLT"),
    VIDEO("VIDEO"),
    TIFF("TIFF"),
    PDF("PDF/AorUA"),
    LUA("LUA"),
    OTHER("other");

    private final String value;
}
