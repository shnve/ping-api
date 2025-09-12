package fr.shom.ping.utils.survey;

import com.epam.parso.impl.SasFileParser;
import fr.shom.ping.exception.ConvertException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.SurveyFileInArchive;
import fr.shom.ping.model.dto.BoundingBoxDto;
import fr.shom.ping.model.dto.Point2dDto;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyFileStatisticFields;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import fr.shom.ping.model.enums.SurveyZDirection;
import fr.shom.ping.utils.SurveyUtil;
import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import static fr.shom.ping.model.enums.SurveyFileStatisticFields.DEEPEST_PROBE;
import static fr.shom.ping.model.enums.SurveyFileStatisticFields.NUMBER_OF_PROBES;
import static fr.shom.ping.model.enums.SurveyFileStatisticFields.NW_CORNER;
import static fr.shom.ping.model.enums.SurveyFileStatisticFields.SE_CORNER;
import static fr.shom.ping.model.enums.SurveyFileStatisticFields.SHORTEST_PROBE;
import static fr.shom.ping.utils.GeoUtil.convertBoundingBoxToPolygon;

@Data
@Builder
public class SurveyStatistics {
    @Builder.Default
    private int probesQty = 0;

    @Builder.Default
    private double minProbeValue = Double.MAX_VALUE;

    @Builder.Default
    private double maxProbeValue = -1 * Double.MAX_VALUE;

    @Builder.Default
    private double maxY = -1 * Double.MAX_VALUE;

    @Builder.Default
    private double minX = Double.MAX_VALUE;

    @Builder.Default
    private double minY = Double.MAX_VALUE;

    @Builder.Default
    private double maxX = -1 * Double.MAX_VALUE;

    public static SurveyStatistics create(SurveyZDirection zDirection) {
        return SurveyStatistics.builder()
            .minProbeValue(zDirection == SurveyZDirection.DEPTH ? Double.MAX_VALUE : -1 * Double.MAX_VALUE)
            .maxProbeValue(zDirection == SurveyZDirection.DEPTH ? -1 * Double.MAX_VALUE : Double.MAX_VALUE)
            .build();
    }

    public void updateMinMax(double value, double x, double y, SurveyZDirection zDirection) {
        if (zDirection == SurveyZDirection.DEPTH) {
            minProbeValue = Double.min(minProbeValue, value);
            maxProbeValue = Double.max(maxProbeValue, value);
        } else {
            minProbeValue = Double.max(minProbeValue, value);
            maxProbeValue = Double.min(maxProbeValue, value);
        }
        maxX = Double.max(maxX, x);
        maxY = Double.max(maxY, y);
        minX = Double.min(minX, x);
        minY = Double.min(minY, y);
        probesQty++;
    }

    public void updateSurveyFile(SurveyFile surveyFile) {
        surveyFile.setStatistics(buildStatsAsMap());
        surveyFile.setFileStats(this);
    }

    public void updateSurvey(Survey survey) throws ServiceException {
        survey.setStatistics(buildStatsAsMap());
        final boolean latLon = survey.getCoordinatesOrder() == SurveyCoordinatesOrder.LAT_LONG_Z;
        final boolean coordinatesXY = latLon || !survey.isGeographicFormat();
        final String sourceEpsg = SurveyUtil.getEpsgCode(survey.getEpsg()).trim();

        final BoundingBoxDto boundingBox = SurveyStatistics.getBoundingBox(survey.getStatistics());
        final Geometry geometry;
        try {
            geometry = convertBoundingBoxToPolygon(sourceEpsg, coordinatesXY, boundingBox);
        } catch (ConvertException e) {
            throw new ServiceException("Failed to create bounding box geometry from computed statistics: " + boundingBox + " from source epsg " + sourceEpsg +
                " and with coordinatesXY=" + coordinatesXY, e);
        }
        survey.setBoundingBox(geometry);
    }

    public Map<SurveyFileStatisticFields, String> buildStatsAsMap() {
        final Map<SurveyFileStatisticFields, String> stats = new HashMap<>();
        stats.put(NW_CORNER, "[" + maxY + ";" + minX + "]");
        stats.put(SE_CORNER, "[" + minY + ";" + maxX + "]");
        stats.put(NUMBER_OF_PROBES, String.valueOf(probesQty));
        stats.put(SHORTEST_PROBE, String.valueOf(minProbeValue));
        stats.put(DEEPEST_PROBE, String.valueOf(maxProbeValue));
        return stats;
    }

    private static Double[] getCornerValues(String valuesFromStats) {
        return Arrays.stream(valuesFromStats.replace("[", "").replace("]", "").split(";"))
            .map(Double::parseDouble)
            .toArray(Double[]::new);
    }

    public static BoundingBoxDto getBoundingBox(Map<SurveyFileStatisticFields, String> stats) {
        final Double[] nwCornerValues = getCornerValues(stats.get(NW_CORNER));
        final Double[] seCornerValues = getCornerValues(stats.get(SE_CORNER));
        return BoundingBoxDto.builder()
            .nwCorner(Point2dDto.builder().y(nwCornerValues[0]).x(nwCornerValues[1]).build())
            .seCorner(Point2dDto.builder().y(seCornerValues[0]).x(seCornerValues[1]).build())
            .build();
    }

    public void updateSurveyFileInArchive(SurveyFileInArchive surveyFileInArchive) {
        surveyFileInArchive.setStatistics(buildStatsAsMap());
        surveyFileInArchive.setFileStats(this);
    }
}
