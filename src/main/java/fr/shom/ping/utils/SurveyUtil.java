package fr.shom.ping.utils;

import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyFileAnalysisError;
import fr.shom.ping.utils.survey.SurveyStatistics;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static fr.shom.ping.model.enums.SurveyFileAnalysisError.FAILED_TO_RETRIEVE_STATS;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.OUT_OF_BOUNDS;

@Slf4j
public final class SurveyUtil {

    public static final long DEFAULT_EXPECTED_DELETION_DATE_OFFSET_DAYS = 30L;

    public static final long EXPECTED_DELETION_DATE_OFFSET_DAYS_FOR_TRANSMITTED_DEPOSIT = 60L;

    private static final AtomicInteger BATHY_FILE_ID_COUNTER = new AtomicInteger(1);

    private SurveyUtil() {
    }

    public static Instant getDataDeletionDate(Instant lastUpdate, boolean isTransmitted) {
        return lastUpdate.plus(
            isTransmitted ? EXPECTED_DELETION_DATE_OFFSET_DAYS_FOR_TRANSMITTED_DEPOSIT : DEFAULT_EXPECTED_DELETION_DATE_OFFSET_DAYS, ChronoUnit.DAYS
        );
    }

    public static String getRealSeparator(String separator) {
        switch (separator) {
            case "SEMICOLON":
                return ";";
            case "COMMA":
                return ",";
            case "TAB":
                return "\t";
            case "SPACE":
                return " ";
            default:
                return separator;
        }
    }

    public static Point2D[] getEnvelopeFromEpsgCode(String epsg) {
        final CoordinateReferenceSystem crs;
        try {
            crs = CRS.decode(epsg);
        } catch (FactoryException e) {
            log.error("Failed to decode: " + epsg, e);
            return null;
        }
        final Envelope envelope = CRS.getEnvelope(crs);
        final Point2D.Double upperCorner = new Point2D.Double(envelope.getUpperCorner().getOrdinate(0), envelope.getUpperCorner().getOrdinate(1));
        final Point2D.Double lowerCorner = new Point2D.Double(envelope.getLowerCorner().getOrdinate(0), envelope.getLowerCorner().getOrdinate(1));
        return new Point2D[]{upperCorner, lowerCorner};
    }

    public static boolean checkBounds(String[] values, Survey survey, int currentLineNumber, Point2D[] bounds, Map<SurveyFileAnalysisError, String> errors) {
        // check X,Y in chosen bounds if projection, else lon/lat
        return survey.isGeographicFormat() ? checkGeographicFormat(values, survey, currentLineNumber, errors)
            : checkProjectionFormat(values, currentLineNumber, bounds, errors);
    }

    public static boolean checkProjectionFormat(String[] values, int currentLineNumber, Point2D[] bounds, Map<SurveyFileAnalysisError, String> errors) {
        // check if x/y in envelope
        final Point2D upperCorner = bounds[0];
        final Point2D lowerCorner = bounds[1];
        final double x = Double.parseDouble(values[0]);
        if (x > upperCorner.getX() || x < lowerCorner.getX()) {
            errors.put(OUT_OF_BOUNDS,
                "x is out of bounds at line: " + currentLineNumber + ". Current value is: " + x + ". It should be between " + lowerCorner.getX() + " and "
                    + upperCorner.getX());
            return false;
        }

        final double y = Double.parseDouble(values[1]);
        if (y > upperCorner.getY() || y < lowerCorner.getY()) {
            errors.put(OUT_OF_BOUNDS,
                "y is out of bounds at line: " + currentLineNumber + ". Current value is: " + y + ". It should be between " + lowerCorner.getY() + " and "
                    + upperCorner.getY());
            return false;
        }
        return true;
    }

    public static boolean checkGeographicFormat(String[] values, Survey survey, int currentLineNumber, Map<SurveyFileAnalysisError, String> errors) {
        // check if -180 <= lon <= 180 && -90 <= lat <= 90
        final boolean latLon = survey.getCoordinatesOrder() == SurveyCoordinatesOrder.LAT_LONG_Z;
        final double lat = Double.parseDouble(values[latLon ? 0 : 1]);
        if (lat > 90 || lat < -90) {
            errors.put(OUT_OF_BOUNDS,
                "lat is out of bounds at line: " + currentLineNumber + ". Current value is: " + lat + ". It should be between -90 and 90");
            return false;
        }

        final double lon = Double.parseDouble(values[latLon ? 1 : 0]);
        if (lon > 180 || lon < -180) {
            errors.put(OUT_OF_BOUNDS,
                "lon is out of bounds at line: " + currentLineNumber + ". Current value is: " + lon + ". It should be between -180 and 180");
            return false;
        }
        return true;
    }

    // idée: récupérer la 3è dimension pour les stats
    public static boolean getStatistics(String[] values, int currentLineNumber, Survey survey, SurveyStatistics statistics, Map<SurveyFileAnalysisError,
        String> errors) {
        if (values.length < 3) {
            errors.put(FAILED_TO_RETRIEVE_STATS, "z not found at line: " + currentLineNumber + " in array: " + Arrays.toString(values));
            return false;
        }
        final double probe = Double.parseDouble(values[2]);
        final boolean latLon = survey.getCoordinatesOrder() == SurveyCoordinatesOrder.LAT_LONG_Z;
        final boolean firstCoord = latLon || !survey.isGeographicFormat();
        final int xIdx = firstCoord ? 0 : 1;
        final int yIdx = firstCoord ? 1 : 0;
        double x = Double.parseDouble(values[xIdx]);
        double y = Double.parseDouble(values[yIdx]);
        statistics.updateMinMax(probe, x, y, survey.getZDirection());
        return true;
    }

    public static String getEpsgCode(String fullEpsg) {
        final int idx = fullEpsg.indexOf("\"");
        if (idx < 10) {
            return fullEpsg;
        }
        return fullEpsg.substring(0, idx - 1);
    }

    public static SurveyStatistics compileAllStatistics(List<SurveyStatistics> statisticsListInitial) {
        final List<SurveyStatistics> statisticsList = statisticsListInitial.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return SurveyStatistics.builder()
            .probesQty(statisticsList.stream().map(SurveyStatistics::getProbesQty).mapToInt(v -> v).sum())
            .maxProbeValue(statisticsList.stream().map(SurveyStatistics::getMaxProbeValue).mapToDouble(v -> v).max().orElse(Double.MAX_VALUE))
            .minProbeValue(statisticsList.stream().map(SurveyStatistics::getMinProbeValue).mapToDouble(v -> v).min().orElse(Double.MIN_VALUE))
            .maxX(statisticsList.stream().map(SurveyStatistics::getMaxX).mapToDouble(v -> v).max().orElse(Double.MAX_VALUE))
            .maxY(statisticsList.stream().map(SurveyStatistics::getMaxY).mapToDouble(v -> v).max().orElse(Double.MAX_VALUE))
            .minX(statisticsList.stream().map(SurveyStatistics::getMinX).mapToDouble(v -> v).min().orElse(Double.MIN_VALUE))
            .minY(statisticsList.stream().map(SurveyStatistics::getMinY).mapToDouble(v -> v).min().orElse(Double.MIN_VALUE))
            .build();
    }

    public static int generateBathyFileId() {
        return BATHY_FILE_ID_COUNTER.incrementAndGet();
    }

    public static Map<String, String> getFilesRelativePaths(Set<SurveyFile> surveyFiles) {
        final Map<String, String> filesRelativePath = new HashMap<>();
        surveyFiles.forEach(surveyFile -> {
            if (surveyFile.isArchive()) {
                for (Map.Entry<String, String> entry : surveyFile.getRelativePathsOfFilesInArchive().entrySet()) {
                    filesRelativePath.put(entry.getKey(), entry.getValue());
                }
            } else {
                filesRelativePath.put(surveyFile.getCartonautFileId().toString(), surveyFile.getAttachment().getFilename());
            }
        });
        return filesRelativePath;
    }
}
