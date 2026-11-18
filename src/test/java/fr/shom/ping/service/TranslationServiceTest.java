package fr.shom.ping.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** PISAC (18-nov-2026): preparación del texto en español antes de la traducción automática. */
class TranslationServiceTest {

    @Test
    void detectsUpperCaseLines() {
        assertThat(TranslationService.isMostlyUpperCase("BOYA DE RECALADA APAGADA EN 12-05.73N 070-12.62W.")).isTrue();
        assertThat(TranslationService.isMostlyUpperCase("Boya de recalada apagada.")).isFalse();
        assertThat(TranslationService.isMostlyUpperCase("12-05.73N")).isFalse();
    }

    @Test
    void lowersUpperCaseKeepingAcronymsCodesAndToponyms() {
        assertThat(TranslationService.prepareSpanishLine("ADVERTENCIA COSTERA VE 012/26", true))
            .isEqualTo("Advertencia costera VE 012/26");
        assertThat(TranslationService.prepareSpanishLine("GOLFO DE VENEZUELA. PUNTA MACOLLA.", true))
            .isEqualTo("Golfo de Venezuela. Punta Macolla.");
        assertThat(TranslationService.prepareSpanishLine("CANCELAR ESTE AVISO 301200 UTC NOV 26.", true))
            .isEqualTo("cancel this message 301200 UTC nov 26.");
    }

    @Test
    void fixesGenericEnglishTerms() {
        assertThat(TranslationService.applyEnglishFixes(
            "Sailors are informed that the fishing vessel \"La Lupita\" is stranded in position 11-21.81N 071-56.56W"))
            .isEqualTo("Mariners are advised that the fishing vessel \"La Lupita\" is aground in position 11-21.81N 071-56.56W");
        assertThat(TranslationService.applyEnglishFixes("Sail with caution")).isEqualTo("Navigate with caution");
    }

    @Test
    void appliesNauticalGlossary() {
        assertThat(TranslationService.prepareSpanishLine("BOYA DE RECALADA APAGADA EN 12-05.73N 070-12.62W.", true))
            .isEqualTo("landfall buoy unlit en 12-05.73N 070-12.62W.");
        assertThat(TranslationService.prepareSpanishLine("Se recomienda navegar con precaución.", false))
            .isEqualTo("mariners are advised to navigate with caution.");
    }
}
