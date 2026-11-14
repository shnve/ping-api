package fr.shom.ping.utils;

import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.S124v2OhiWarningType;
import org.apache.commons.lang3.StringUtils;

import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;

/**
 * PISAC: reglas de la descarga pública del paquete S-124 (exchange set firmado, .ZIP) de un aviso.
 * <p>
 * El paquete se genera al publicar (ver {@code WarningAdminService.generateExchangeSet}), uno por idioma obligatorio
 * de la serie. La descarga desde el portal entrega siempre el paquete en INGLÉS, idioma de intercambio internacional,
 * igual que el portal PING del SHOM. Por eso solo está disponible si la serie tiene el inglés como idioma obligatorio.
 */
public final class S124DownloadUtil {

    /** Idioma del paquete que se descarga desde el portal. */
    public static final String DOWNLOAD_LANGUAGE = S124Util.INTERNATIONAL_LANGUAGE;

    private S124DownloadUtil() {
    }

    /**
     * Mismas condiciones con las que se genera el paquete: organismo productor de datos S-124, serie con identificador S-124
     * y serie de advertencias (S-53). Los avisos a los navegantes nunca se exportan (escenario B).
     */
    public static boolean isS124Producible(Warning warning) {
        if (warning == null || warning.getSerie() == null || warning.getSerie().getMaritimeService() == null) {
            return false;
        }
        return warning.getSerie().getMaritimeService().isS124DataProducer()
            && StringUtils.isNotBlank(warning.getSerieS124Identifier())
            && S124v2OhiWarningType.isS124Exportable(warning);
    }

    /** El portal ofrece la descarga: aviso publicado, producible en S-124 y con el inglés como idioma obligatorio de la serie. */
    public static boolean isDownloadAvailable(Warning warning) {
        if (warning == null || warning.getStatus() != PUBLISHED || !isS124Producible(warning)) {
            return false;
        }
        final Serie serie = warning.getSerie();
        return serie.getMandatoryLanguages() != null && serie.getMandatoryLanguages().contains(DOWNLOAD_LANGUAGE);
    }
}
