package fr.shom.ping.utils;

import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningNavtexConfig;
import fr.shom.ping.model.enums.DiffusionNavtexEmergencyLevel;
import fr.shom.ping.model.enums.DiffusionNavtexSource;
import fr.shom.ping.model.enums.DiffusionNavtexType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fr.shom.ping.PingApiTranslationsRegistry.isInternationalLanguage;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;

@Slf4j
@Component
public final class NavtexUtil {
    @Autowired
    private TelexUtil telexUtil;

    private static final String LINE_SEPARATOR = "\n";
    private static final String KA_FREQUENCY = "*518Khz*";
    private static final String TA_FREQUENCY = "*490Khz*";
    private static final String INTERNATIONAL_TELEGRAPH_ALPHABET_2_NOT_ALLOWED_CHARS_REGEX = "[^\\da-zA-Z\\s\\.,:\\?'\\+-/=\\(\\)]";

    public static String getNavtexCommunicationNumber(Warning warning) {
        final String warningNumber = zeroLeftPad(warning.getWarningNumber(), 4);
        return warningNumber.substring(warningNumber.length() - 4);
    }

    public static String getSeriePrefix(String serieName) {
        return Arrays.stream(serieName.split(" ")).map(s -> s.charAt(0)).map(String::valueOf).map(String::toUpperCase).collect(Collectors.joining());
    }

    public static String getYearSuffix(Integer year) {
        return year.toString().substring(2);
    }

    public String generateCancellationNavtex(Warning warning, String targetFolder) throws IOException {
        final String seriePath = createTargetFolder(targetFolder, warning.getNameOfSeries());
        final String content = generateCancellationNavtex(warning);
        final Path cancellationFile = Path.of(seriePath,
            String.format("%s%s_%sAD.txt", getSeriePrefix(warning.getNameOfSeries()), getNavtexCommunicationNumber(warning), getYearSuffix(warning.getYear())));
        Files.writeString(cancellationFile, content);
        return content;
    }

    public String generatePublishedNavtex(Warning warning, String targetFolder) throws IOException {
        final String seriePath = createTargetFolder(targetFolder, warning.getNameOfSeries());
        final String content = warning.getNavtex();
        final Path publishFile = Path.of(seriePath,
            String.format("%s%s_%s.txt", getSeriePrefix(warning.getNameOfSeries()), getNavtexCommunicationNumber(warning), getYearSuffix(warning.getYear())));
        Files.writeString(publishFile, content);
        return content;
    }

    public static String createTargetFolder(String rootFolder, String serieName) throws IOException {
        // we remove spaces from serieName
        final String escapedSerieName = serieName.replaceAll("[^a-zA-Z0-9]", "_");
        final Path seriePath = Path.of(rootFolder, escapedSerieName);
        // create serie target folder if doesn't exist
        if (!Files.exists(seriePath)) {
            Files.createDirectory(seriePath);
        }
        return seriePath.toString();
    }

    public String generateCancellationNavtex(Warning warning) {
        final Serie serie = warning.getSerie();
        final DiffusionNavtexSource source = serie.getNavtexSource();
        return "NAVTEX %s??%s".formatted(source, getNavtexCommunicationNumber(warning));
    }

    public String generateNavtex(Warning warning) {

        final Serie serie = warning.getSerie();

        final DiffusionNavtexSource source = serie.getNavtexSource();

        if (DiffusionNavtexSource.N == source) {
            return generateSpecificNavtex(warning);
        }

        return generateClassicNavtex(warning, source);
    }

    private String generateHeader(Warning warning, DiffusionNavtexSource source) {
        final WarningNavtexConfig warningNavtexConfig = warning.getNavtexConfig();
        final DiffusionNavtexType typeCommunication = warningNavtexConfig.getCommunicationType();
        final DiffusionNavtexEmergencyLevel emergencyLevel = warningNavtexConfig.getEmergencyLevel();
        final String communicationNumber = getNavtexCommunicationNumber(warning);
        final StringBuilder sbHeader = new StringBuilder(String.format("NAVTEX %s%s%s%s", source, typeCommunication.getSymbol(),
            emergencyLevel.getLetter(), communicationNumber));
        if (warningNavtexConfig.getEndDay() != null && warningNavtexConfig.getEndHour() != null) {
            sbHeader.append(String.format("J%sH%s", zeroLeftPad(warningNavtexConfig.getEndDay(), 2), zeroLeftPad(warningNavtexConfig.getEndHour(), 2)));
        }
        sbHeader.append(LINE_SEPARATOR).append("TXT");
        return sbHeader.toString();
    }

    private String generateSpecificNavtex(Warning warning) {
        final WarningNavtexConfig warningNavtexConfig = warning.getNavtexConfig();
        final DiffusionNavtexEmergencyLevel emergencyLevel = warningNavtexConfig.getEmergencyLevel();
        return new StringBuilder("MESSAGES CATEGORY %s".formatted(emergencyLevel.name()))
            .append(LINE_SEPARATOR)
            .append(generateKaFrequencyPart(warning, warningNavtexConfig))
            .append(LINE_SEPARATOR)
            .append(generateTaFrequencyPart(warning, warningNavtexConfig))
            .toString();
    }

    private String generateKaFrequencyPart(Warning warning, WarningNavtexConfig warningNavtexConfig) {
        StringBuilder kaBuilder = new StringBuilder(KA_FREQUENCY)
            .append(LINE_SEPARATOR)
            .append(String.format("KA %d", warningNavtexConfig.getNitonNumber()))
            .append(LINE_SEPARATOR)
            .append(generateTelexForLanguage(warning, ENGLISH.getLanguage(), false))
            .append(LINE_SEPARATOR);

        if (warningNavtexConfig.getCancelNitonNumber() != null) {
            kaBuilder
                .append(String.format("- CANCEL KA %d", warningNavtexConfig.getCancelNitonNumber()))
                .append(LINE_SEPARATOR);
        }

        return kaBuilder.toString();
    }

    private String generateTaFrequencyPart(Warning warning, WarningNavtexConfig warningNavtexConfig) {
        StringBuilder taBuilder = new StringBuilder(TA_FREQUENCY)
            .append(LINE_SEPARATOR)
            .append(String.format("TA %d", warningNavtexConfig.getNitonNumber()))
            .append(LINE_SEPARATOR)
            .append(generateTelexForLanguage(warning, FRENCH.getLanguage(), false))
            .append(LINE_SEPARATOR);

        if (warningNavtexConfig.getCancelNitonNumber() != null) {
            taBuilder.append(String.format("- ANNULER TA %d", warningNavtexConfig.getCancelNitonNumber()));
        }

        return taBuilder.toString();
    }

    private String generateClassicNavtex(Warning warning, DiffusionNavtexSource source) {
        return new StringBuilder()
            .append(generateHeader(warning, source))
            .append(LINE_SEPARATOR)
            .append(generateTelex(warning, true))
            .append(LINE_SEPARATOR)
            .append("DDDD")
            .append(LINE_SEPARATOR)
            .append("BT")
            .toString()
            .replace("\n\nDDDD\n", "\nDDDD\n");
    }

    private String generateTelex(Warning warning, boolean addHeader) {
        return warning.getSerie().getMandatoryLanguages().stream()
            .sorted()
            .map(language -> generateTelexForLanguage(warning, language, addHeader))
            .collect(Collectors.joining(LINE_SEPARATOR))
            .replace("\n\nFFFF\n", "\nFFFF\n");
    }

    private String generateTelexForLanguage(Warning warning, String language, boolean addHeader) {
        final StringBuilder generatedTelex = new StringBuilder();
        if (addHeader) {
            generatedTelex.append(getLanguageHeader(language))
                .append(LINE_SEPARATOR);
        }
        generatedTelex.append(telexUtil.generateTelex(warning, language, null));
        return StringUtils.stripAccents(generatedTelex.toString().toUpperCase())
            .replaceAll(INTERNATIONAL_TELEGRAPH_ALPHABET_2_NOT_ALLOWED_CHARS_REGEX, "");
    }

    private static String getLanguageHeader(String language) {
        final boolean internationalLanguage = isInternationalLanguage(language);
        return internationalLanguage ? "AAAA" : "FFFF";
    }

    public static String zeroLeftPad(Number number, int padSize) {
        return StringUtils.leftPad(number.toString(), padSize, "0");
    }

    public static boolean isConfigValid(WarningNavtexConfig config, DiffusionNavtexSource source) {
        if (source == null) {
            return true;
        }

        if (config == null) {
            return false;
        }

        boolean isValid = false;
        switch (source) {
            case B:
            case T:
                isValid = config.getEmergencyLevel() != null && config.getCommunicationType() != null;
                break;
            case N:
                isValid = config.getEmergencyLevel() != null && config.getNitonNumber() != null;
                break;
        }

        return isValid;
    }
}
