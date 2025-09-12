package fr.shom.ping.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class CsvParsingException  extends Exception {

    public CsvParsingException(String message) {
        super(message);
    }

    public CsvParsingException(Throwable e) {
        super(e);
    }
}
