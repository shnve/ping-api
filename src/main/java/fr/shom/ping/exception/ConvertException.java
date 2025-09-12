package fr.shom.ping.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class ConvertException extends Exception {

    public ConvertException(String message) {
        super(message);
    }

    public ConvertException(Throwable e) {
        super(e);
    }
}
