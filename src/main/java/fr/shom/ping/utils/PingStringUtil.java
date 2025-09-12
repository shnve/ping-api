package fr.shom.ping.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PingStringUtil {

    public static final String REGEX_PATTERN_NULL_OR_NOT_BLANK = "^(?!\\s*\\n*$).+";

    public static final String REGEX_PATTERN_FILE_NAME = "^[a-zA-Z0-9_\\-\\. \\(\\)]+$";
    public static final String REGEX_PATTERN_FILE_IDENTIFIER = "^[a-zA-Z0-9_\\-]+$";

    private PingStringUtil() {}

    public static String indentMultilineText(String multilineText, int numberOfSpaces) {
        final char[] chars = new char[numberOfSpaces];
        Arrays.fill(chars, ' ');
        return multilineText.replaceAll("(?m)^", new String(chars));
    }

    public static String blobAsString(byte[] blob) {
        if (blob == null) {
            return null;
        }
        return new String(blob, StandardCharsets.UTF_8);
    }

    public static byte[] stringAsBlob(String content) {
        if (content == null) {
            return new byte[0];
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
