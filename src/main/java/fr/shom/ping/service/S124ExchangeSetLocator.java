package fr.shom.ping.service;

import fr.shom.ping.model.Warning;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.utils.NavtexUtil;
import fr.shom.ping.utils.S124DownloadUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static fr.shom.ping.utils.NavtexUtil.zeroLeftPad;
import static fr.shom.ping.utils.S124Util.INTERNATIONAL_LANGUAGE;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.cleanPath;

/**
 * PISAC: ubicación en disco de los paquetes S-124 (exchange sets) generados al publicar.
 * <p>
 * Antes esta lógica estaba solo dentro de {@link SecomService}. Se separa para que el detalle del aviso pueda comprobar si el
 * paquete existe sin depender de {@code SecomService} (que a su vez depende de {@code WarningService}).
 * <pre>
 * {generated-s124-folder}/{código OHI}/{año}/{datasetId}/{datasetId}.ZIP
 * datasetId = 124 + código OHI + identificador S-124 de la serie + nº (4 dígitos) + año (2) + idioma (E | S)
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class S124ExchangeSetLocator {

    static final String DATASET_IDENTIFIER_FORMAT = "124%s%s%s%s%s";
    static final String DEFAULT_S124_ID = "X";
    static final String LANG_IDENTIFIER_EN = "E";
    static final String LANG_IDENTIFIER_ES = "S";
    static final String ZIP_EXT = ".ZIP";

    private static final XssSanitizerService XSS_SANITIZER_SERVICE = new XssSanitizerService();

    private final PingProperties pingProperties;

    public static String langIdentifier(String lang) {
        return StringUtils.equalsIgnoreCase(lang, INTERNATIONAL_LANGUAGE) ? LANG_IDENTIFIER_EN : LANG_IDENTIFIER_ES;
    }

    public static String buildDatasetIdentifier(Warning warning, String ihoProducerFolderName, String langIdentifier) {
        return String.format(
            DATASET_IDENTIFIER_FORMAT, ihoProducerFolderName, ofNullable(warning.getSerieS124Identifier()).orElse(DEFAULT_S124_ID),
            zeroLeftPad(warning.getWarningNumber(), 4), NavtexUtil.getYearSuffix(warning.getYear()), langIdentifier
        );
    }

    public String getIhoProducerFolderName(Warning warning) {
        return XSS_SANITIZER_SERVICE.sanitize(warning.getMaritimeServiceihoProducerCode())
            .replaceAll(pingProperties.getS124sanitizeRegex(), "");
    }

    /** Ruta base del paquete, sin extensión: .../{datasetId}/{datasetId} */
    public Path getExchangeSetFullpath(Warning warning, String lang) {
        final String ihoProducerFolderName = getIhoProducerFolderName(warning);
        final String datasetIdentifier = buildDatasetIdentifier(warning, ihoProducerFolderName, langIdentifier(lang));

        final Path ihoProducerCodeDirPath =
            Path.of(cleanPath(Path.of(pingProperties.getGeneratedS124Folder(), ihoProducerFolderName).toString()));

        final Path warningNumberYearDirPath =
            Path.of(cleanPath(Path.of(ihoProducerCodeDirPath.toString(), warning.getYear().toString()).toString()));

        final Path exchangeSetRootDirPath = Path.of(warningNumberYearDirPath.toString(), datasetIdentifier);
        return Path.of(exchangeSetRootDirPath.toString(), datasetIdentifier);
    }

    /** ZIP del paquete en el idioma de la descarga pública (inglés). */
    public Path getDownloadZip(Warning warning) {
        return Path.of(getExchangeSetFullpath(warning, S124DownloadUtil.DOWNLOAD_LANGUAGE) + ZIP_EXT).normalize();
    }

    /**
     * El portal muestra el botón de descarga solo si se cumplen las reglas ({@link S124DownloadUtil#isDownloadAvailable}) y el ZIP
     * en inglés existe de verdad. Los avisos publicados antes de configurar la serie para S-124 no tienen paquete y no muestran el botón.
     */
    public boolean isDownloadZipPresent(Warning warning) {
        return S124DownloadUtil.isDownloadAvailable(warning) && Files.isRegularFile(getDownloadZip(warning));
    }
}
