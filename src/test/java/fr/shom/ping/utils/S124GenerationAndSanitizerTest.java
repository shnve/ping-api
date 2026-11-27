package fr.shom.ping.utils;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.controller.S124Controller;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.S124Exception;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AbstractS124ExchangeSetBaseDto;
import fr.shom.ping.model.dto.S124v2DataSetDto;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.repository.SerieRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.service.S124Service;
import fr.shom.ping.service.email.PisacEmailTemplate;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class S124GenerationAndSanitizerTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 3857);

    private static S124v2Util newS124v2Util() throws Exception {
        Constructor<S124v2Util> ctor = S124v2Util.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static TelexUtil newTelexUtil() throws Exception {
        Constructor<TelexUtil> ctor = TelexUtil.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        TelexUtil util = ctor.newInstance();
        PingApiTranslationsRegistry registry = Mockito.mock(PingApiTranslationsRegistry.class);
        when(registry.getWarningHazardTypeGeneralByEnumNameAndLang(anyString(), anyString())).thenReturn("OPERACIONES");
        when(registry.getWarningHazardTypeDetailsByEnumNameAndLang(anyString(), anyString())).thenReturn("OPERACIONES SUBACUATICAS");
        Field field = TelexUtil.class.getDeclaredField("translationsRegistry");
        field.setAccessible(true);
        field.set(util, registry);
        return util;
    }

    private static Warning buildSampleWarningWithPolygon(String esText, String enText) {
        MaritimeService service = MaritimeService.builder()
            .name("SHN")
            .ihoProducerCode("VE00")
            .build();
        Serie serie = Serie.builder()
            .name("ADVERTENCIA LOCAL VE")
            .internationalName("LOCAL WARNING VE")
            .type(SerieType.LOCAL_NAVIGATIONAL_WARNING)
            .maritimeService(service)
            .mandatoryLanguages(Set.of("es", "en"))
            .optionalLanguages(Set.of())
            .build();

        // Polígono en EPSG:3857 (zona Venezuela)
        Coordinate[] ring = new Coordinate[]{
            new Coordinate(-6688100, 1102000),
            new Coordinate(-6696000, 1099000),
            new Coordinate(-6699000, 1101000),
            new Coordinate(-6688100, 1102000)
        };
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring);

        Set<Feature> geometries = new LinkedHashSet<>();
        geometries.add(Feature.builder().geometry(polygon).build());

        Set<LocalizedText> info = new LinkedHashSet<>();
        info.add(LocalizedText.builder().language("es").text(esText).build());
        info.add(LocalizedText.builder().language("en").text(enText).build());

        FeaturePart part = FeaturePart.builder()
            .position(1)
            .geometries(geometries)
            .information(info)
            .warningHazardTypeDetails(Set.of(WarningHazardTypeDetails.UNDERWATER_OPERATIONS))
            .build();

        TreeSet<FeaturePart> parts = new TreeSet<>();
        parts.add(part);

        return Warning.builder()
            .serie(serie)
            .nameOfSeries("ADVERTENCIA LOCAL VE")
            .warningNumber(12)
            .year(2026)
            .publicationTime(Instant.parse("2026-11-20T12:00:00Z"))
            .warningHazardTypeGeneral(WarningHazardTypeGeneral.SPECIAL_OPERATIONS)
            .featureParts(parts)
            .build();
    }

    @Test
    void englishS124UsesEnglishSeriesNameInTitleAndAllGmlIdsAndLinks() throws Exception {
        S124v2Util util = newS124v2Util();
        Warning warning = buildSampleWarningWithPolygon(
            "Se informa a los navegantes que se estará efectuando un levantamiento geofísico\nárea delimitada por:\n09-51.07N 060-04.83W\n09-49.93N 060-09.33W\nNavegue con precaución.",
            "Mariners are advised that a geophysical survey will be conducted within the area bounded by the following coordinates:\n09-51.07N 060-04.83W\n09-49.93N 060-09.33W\nNavigate with caution."
        );

        S124v2DataSetDto dtoEn = util.mapWarningToS124DataSet("en", warning);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        String xmlEn = xmlMapper.writeValueAsString(dtoEn);

        assertThat(xmlEn)
            .contains("<S100:datasetTitle>LOCAL WARNING VE 12/26 - eng</S100:datasetTitle>")
            .contains("gml:id=\"NW.LOCAL_WARNING_VE.12.2026.eng\"")
            .contains("<S124:NavwarnPreamble gml:id=\"PR.LOCAL_WARNING_VE.12.2026.eng\">")
            .contains("<S124:NavwarnPart gml:id=\"NW.LOCAL_WARNING_VE.12.2026.eng.1\">")
            .contains("<S124:header xlink:href=\"#PR.LOCAL_WARNING_VE.12.2026.eng\"/>")
            .contains("<S100:Surface gml:id=\"NW.LOCAL_WARNING_VE.12.2026.eng.1.1\"")
            .contains("<S124:nameOfSeries>LOCAL WARNING VE</S124:nameOfSeries>")
            .doesNotContain("ADVERTENCIA_LOCAL_VE")
            .doesNotContain("ADVERTENCIA LOCAL VE");

        // <S124:text> sin coordenadas en inglés, pero <S124:geometry> intacta
        assertThat(xmlEn)
            .contains("<S124:geometry>")
            .contains("<S124:text>Mariners are advised that a geophysical survey will be conducted\nNavigate with caution.</S124:text>")
            .doesNotContain("09-51.07N")
            .doesNotContain("060-04.83W");

        // En español mantiene el nombre de la serie en español y también quita las coordenadas de <S124:text>
        S124v2DataSetDto dtoEs = util.mapWarningToS124DataSet("es", warning);
        String xmlEs = xmlMapper.writeValueAsString(dtoEs);
        assertThat(xmlEs)
            .contains("<S100:datasetTitle>ADVERTENCIA LOCAL VE 12/26 - spa</S100:datasetTitle>")
            .contains("<S124:NavwarnPreamble gml:id=\"PR.ADVERTENCIA_LOCAL_VE.12.2026.spa\">")
            .contains("<S124:NavwarnPart gml:id=\"NW.ADVERTENCIA_LOCAL_VE.12.2026.spa.1\">")
            .contains("<S124:header xlink:href=\"#PR.ADVERTENCIA_LOCAL_VE.12.2026.spa\"/>")
            .contains("<S100:Surface gml:id=\"NW.ADVERTENCIA_LOCAL_VE.12.2026.spa.1.1\"")
            .contains("<S124:geometry>")
            .contains("<S124:text>Se informa a los navegantes que se estará efectuando un levantamiento geofísico\nNavegue con precaución.</S124:text>")
            .doesNotContain("09-51.07N")
            .doesNotContain("060-04.83W");
    }

    @Test
    void stripCoordinatesCleansInlineAndBlockPatternsInSpanishAndEnglish() {
        assertThat(S124TextSanitizer.stripCoordinates(
            "Mariners are advised that the Punta Brava Lighthouse, located at 10-29.40N 068-00.52W (WGS-84 datum), has been restored to normal operation.\nAffected charts: VE-300, VE-303."
        )).isEqualTo("Mariners are advised that the Punta Brava Lighthouse has been restored to normal operation.\nAffected charts: VE-300, VE-303.");

        assertThat(S124TextSanitizer.stripCoordinates(
            "Se informa a los navegantes que el Faro Punta Brava en posición 10-29.40N 068-00.52W Datum WGS-84 se encuentra restaurado a lo normal.\nCartas afectadas: VE-300, VE-303."
        )).isEqualTo("Se informa a los navegantes que el Faro Punta Brava se encuentra restaurado a lo normal.\nCartas afectadas: VE-300, VE-303.");

        assertThat(S124TextSanitizer.stripCoordinates(
            "Se informa a los navegantes que se realizara actividad deportiva en el\nárea delimitada por:\n10-25.83N 064-12.44W\n10-25.84N 064-12.51W\nDATUM WGS84\nÁrea restringida\nCartas afectadas: VE-508."
        )).isEqualTo("Se informa a los navegantes que se realizara actividad deportiva\nÁrea restringida\nCartas afectadas: VE-508.");

        assertThat(S124TextSanitizer.stripCoordinates(
            "Mariners are advised that sporting activity will take place in the area bounded by:\n10-25.83N 064-12.44W\n10-25.84N 064-12.51W\nDATUM WGS84\nRestricted area\nCharts affected: VE-508."
        )).isEqualTo("Mariners are advised that sporting activity will take place\nRestricted area\nCharts affected: VE-508.");

        assertThat(S124TextSanitizer.stripCoordinates(
            "Se informa a los navegantes que el faro Punta Macolla\nen posición geográfica 12-05.73N 070-12.62W DATUMGWS-84\nse encuentra restaurado a lo normal"
        )).isEqualTo("Se informa a los navegantes que el faro Punta Macolla\nse encuentra restaurado a lo normal");

        assertThat(S124TextSanitizer.stripCoordinates(
            "Peligro reportado\nárea dentro de un radio de 2.0 MN de 10-18.74N 066-54.12W\nNavegue con precaución."
        )).isEqualTo("Peligro reportado\nNavegue con precaución.");
    }

    @Test
    void telexKeepsCoordinatesWhenPresentAndAppendsThemFromGeometryWhenMissing() throws Exception {
        TelexUtil telexUtil = newTelexUtil();

        // Caso 1: el texto ya tiene coordenadas -> se conservan sin duplicar
        Warning withCoords = buildSampleWarningWithPolygon(
            "Boya apagada en posición 10-29.40N 068-00.52W.",
            "Buoy unlit in position 10-29.40N 068-00.52W."
        );
        String telexEsExisting = telexUtil.generateTelex(withCoords, "es", null);
        assertThat(telexEsExisting)
            .contains("10-29.40N 068-00.52W")
            .doesNotContain("AREA DELIMITADA POR:");

        // Caso 2: el texto NO tiene coordenadas -> el TELEX las agrega desde la geometría
        Warning withoutCoords = buildSampleWarningWithPolygon(
            "Se efectúa levantamiento geofísico.\nNavegue con precaución.",
            "Geophysical survey underway.\nNavigate with caution."
        );
        String telexEsGenerated = telexUtil.generateTelex(withoutCoords, "es", null);
        assertThat(telexEsGenerated)
            .contains("AREA DELIMITADA POR:")
            .contains("N ")
            .contains("W");

        String telexEnGenerated = telexUtil.generateTelex(withoutCoords, "en", null);
        assertThat(telexEnGenerated)
            .contains("AREA BOUNDED BY:")
            .contains("N ")
            .contains("W");

        // Punto en EPSG:4326 también formateado correctamente
        Point pt = GEOMETRY_FACTORY.createPoint(new Coordinate(-68.0086667, 10.49));
        String formattedPt = S124TextSanitizer.formatGeometriesForText(Set.of(Feature.builder().geometry(pt).build()), false);
        assertThat(formattedPt).isEqualTo("EN POSICION 10-29.40N 068-00.52W");
    }

    @Test
    void s124ControllerPublishesOnlyVersion2() {
        assertThatCode(() -> S124Controller.checkIfversionIsSupported("v2")).doesNotThrowAnyException();
        assertThatThrownBy(() -> S124Controller.checkIfversionIsSupported("v1"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getInForceNwMessageIdentifiersWithoutSeriesReturnsAllPublishedS124Warnings() throws Exception {
        WarningRepository warningRepo = Mockito.mock(WarningRepository.class);
        SerieRepository serieRepo = Mockito.mock(SerieRepository.class);
        S124v2Util s124v2Util = newS124v2Util();
        S124Service s124Service = new S124Service(warningRepo, serieRepo, s124v2Util);
        S124Controller controller = new S124Controller(s124Service);

        Warning localWarning = buildSampleWarningWithPolygon("Boya apagada.", "Buoy unlit.");
        localWarning.setStatus(WarningStatus.PUBLISHED);
        localWarning.setCountry("VE");

        Serie coastalSerie = Serie.builder()
            .name("ADVERTENCIA COSTERA VE")
            .internationalName("COASTAL WARNING VE")
            .type(SerieType.COASTAL_NAVIGATIONAL_WARNING)
            .maritimeService(localWarning.getSerie().getMaritimeService())
            .mandatoryLanguages(Set.of("es", "en"))
            .optionalLanguages(Set.of())
            .build();
        Warning coastalWarning = buildSampleWarningWithPolygon("Faro apagado.", "Light unlit.");
        coastalWarning.setSerie(coastalSerie);
        coastalWarning.setNameOfSeries("ADVERTENCIA COSTERA VE");
        coastalWarning.setWarningNumber(5);
        coastalWarning.setYear(2026);
        coastalWarning.setStatus(WarningStatus.PUBLISHED);
        coastalWarning.setCountry("VE");

        when(serieRepo.findAll()).thenReturn(List.of(localWarning.getSerie(), coastalSerie));
        when(warningRepo.findByStatus(WarningStatus.PUBLISHED)).thenReturn(List.of(localWarning, coastalWarning));

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        AbstractS124ExchangeSetBaseDto resultEs = controller.getInForceNwMessageIdentifiers("v2", null, "es");
        String xmlEs = xmlMapper.writeValueAsString(resultEs);
        assertThat(xmlEs)
            .contains("<S124:nameOfSeries>ADVERTENCIA COSTERA VE</S124:nameOfSeries>")
            .contains("<S124:nameOfSeries>ADVERTENCIA LOCAL VE</S124:nameOfSeries>")
            .contains("<S124:noMessageOnHand>false</S124:noMessageOnHand>");

        AbstractS124ExchangeSetBaseDto resultEn = controller.getInForceNwMessageIdentifiers("v2", "", "en");
        String xmlEn = xmlMapper.writeValueAsString(resultEn);
        assertThat(xmlEn)
            .contains("<S124:nameOfSeries>COASTAL WARNING VE</S124:nameOfSeries>")
            .contains("<S124:nameOfSeries>LOCAL WARNING VE</S124:nameOfSeries>");

        when(serieRepo.findOneByNameIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
        when(serieRepo.findOneByInternationalNameIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.getInForceNwMessageIdentifiers("v2", "UNKNOWN", "es"))
            .isInstanceOf(S124Exception.class);
    }

    @Test
    void emailTemplateUsesUpdatedInstitutionalContactEmail() {
        PisacEmailTemplate template = new PisacEmailTemplate();
        String safe = template.sanitizeContent("<p>Prueba</p>");
        assertThat(template.renderHtml("Aviso", safe, false))
            .contains("mailto:shnayunav@shn.gob.ve")
            .doesNotContain("shnser@shn.gob.ve");
        assertThat(template.renderText("Aviso", safe))
            .contains("shnayunav@shn.gob.ve")
            .doesNotContain("shnser@shn.gob.ve");
    }
}
