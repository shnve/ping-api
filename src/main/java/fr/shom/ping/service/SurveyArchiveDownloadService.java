package fr.shom.ping.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.enums.SurveyActivity;
import fr.shom.ping.model.enums.SurveyBroadcastRestriction;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyVerticalRef;
import fr.shom.ping.model.enums.SurveyZDirection;
import fr.shom.ping.properties.PingProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.service.SurveyExternalAnalysisService.RESULTS_FOLDER;
import static fr.shom.ping.service.SurveyFileAnalysisService.copySurveyFileToDirectory;
import static fr.shom.ping.utils.ZipUtil.generateZipFileOfDirectoriesAndAttachToServletResponse;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyArchiveDownloadService {

    private static final DateTimeFormatter NUMBER_DATE_PATTERN = ofPattern("uuuuMMdd");

    private static final DateTimeFormatter NUMBER_DATE_PATTERN2 = ofPattern("dd/MM/uuuu");

    private final PingProperties pingProperties;

    @Getter
    private Path workingDir;

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void initWorkingDir(Survey survey) {
        this.workingDir = Paths.get(pingProperties.getGeneratedSurveyZipArchiveFolder(), survey.getId() + "-" + Instant.now().toEpochMilli());
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void generateSurveyZipArchive(Survey survey, HttpServletResponse response) throws ServiceException {
        initWorkingDir(survey);
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            log.error("Failed to create working directory: " + workingDir, e);
            throw new ServiceException("Failed to create working directory: " + workingDir, e);
        }

        final File workingDirFile = workingDir.toFile();

        // copy all survey files (bathy, technical, misc)
        copySurveyFiles(survey, workingDir);

        // create PDF file
        createPdfFile(survey, workingDirFile);

        // create CSV file
        createCsvFile(survey, workingDirFile);

        // copy Cartonaut results
        copyExternalAnalysisResultFiles(survey, Path.of(workingDir.toString(), RESULTS_FOLDER));

        generateZipFileOfDirectoriesAndAttachToServletResponse(workingDir.toString(), response);

        cleanWorkingDir(workingDirFile);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void createPdfFile(Survey survey, File targetDirFile) throws ServiceException {
        File file;
        try {
            file = Files.createFile(Path.of(targetDirFile.getAbsolutePath(), "details.pdf")).toFile();
        } catch (IOException e) {
            log.error("Failed to create pdf", e);
            throw new ServiceException("Failed to create pdf", e);
        }

        try {
            writePdfFile(file, survey);
        } catch (IOException e) {
            log.error("Failed to write pdf", e);
            throw new ServiceException("Failed to write pdf", e);
        }
    }

    public void writePdfFile(File pdfFile, Survey survey) throws IOException {
        PdfWriter writer = new PdfWriter(pdfFile);
        PdfDocument pdf = new PdfDocument(writer);
        try (Document document = new Document(pdf, PageSize.A4, false)) {
            final Text title = new Text(survey.getIdentifier() + " (Transmis)");
            final PdfFont h2Font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            final Paragraph paragraphTitle = new Paragraph(title).setFont(h2Font).setFontSize(12);

            // Creating a list
            final List list = new List();

            // Add elements to the list
            list.add("Nom du levé et localisation : " + survey.getNameAndLocation());
            list.add("Région : " + survey.getArea().getName());

            // Adding paragraph to the document
            document.add(paragraphTitle);

            // Adding list to the document
            document.add(list);

            document.add(new Paragraph(" "));

            // metadata
            final Text metadataText = new Text("Description des métadonnées générales");
            final Paragraph metadataParagraph = new Paragraph(metadataText).setFont(h2Font).setFontSize(12);

            document.add(metadataParagraph);

            final List metadataList = new List();

            metadataList.add("Objectif du levé : " + survey.getGoal());
            metadataList.add("Date de début du levé : " + LocalDate.ofInstant(survey.getStartDate(), UTC).format(NUMBER_DATE_PATTERN2));
            metadataList.add("Date de fin du levé : " + LocalDate.ofInstant(survey.getEndDate(), UTC).format(NUMBER_DATE_PATTERN2));
            metadataList.add("Nom de l'organisme maîtrise d'ouvrage : " + survey.getOwnerOrganization());
            metadataList.add("Nom de l'organisme maîtrise d'oeuvre : " + survey.getProducerOrganization());
            if (isNotBlank(survey.getOwnerEmail())) {
                metadataList.add("Contact propriétaire : " + survey.getOwnerEmail());
            }
            if (isNotBlank(survey.getTechnicalManagerEmail())) {
                metadataList.add("Contact producteur : " + survey.getTechnicalManagerEmail());
            }

            document.add(metadataList);

            document.add(new Paragraph(" "));

            // confidentiality
            final Text confidentialityText = new Text("Confidentialité");
            final Paragraph confidentialityParagraph = new Paragraph(confidentialityText).setFont(h2Font).setFontSize(12);

            document.add(confidentialityParagraph);

            final List confidentialityList = new List();

            confidentialityList.add("Activité sélectionnée : " + getActivity(survey.getActivityChoice()));
            confidentialityList.add("Restriction de diffusion : " + getBroadcastRestrictionForPdf(survey.getBroadcastRestriction()));
            confidentialityList.add("Commentaire facultatif : " + survey.getConfidentialityInformation());

            document.add(confidentialityList);

            document.add(new Paragraph(" "));

            // bathy data description
            final Text bathyDataText = new Text("Description des données bathymétriques");
            final Paragraph bathyDataParagraph = new Paragraph(bathyDataText).setFont(h2Font).setFontSize(12);

            document.add(bathyDataParagraph);

            final List bathyDataList = new List();

            bathyDataList.add("Type de coordonnées : " + survey.getEpsg());
            bathyDataList.add("Ordre des coordonnées : " + getCoordinatesOrder(survey.getCoordinatesOrder()));
            bathyDataList.add("Sens du Z : " + getZDirection(survey.getZDirection()));
            bathyDataList.add("Séparateur de fichier : " + getFileSeparator(survey.getFileSeparator()));
            bathyDataList.add("Référence verticale : " + getVerticalRef(survey.getVerticalRef()));
            bathyDataList.add("Souhaitez-vous solliciter l'assistance à la détection de l'information nautique bathymétrique ? "
                + (survey.getExternalAnalysisEnabled() ? "oui" : "non"));

            document.add(bathyDataList);
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void createCsvFile(Survey survey, File targetDirFile) throws ServiceException {
        File file;
        try {
            file = Files.createFile(Path.of(targetDirFile.getAbsolutePath(), "details.csv")).toFile();
        } catch (IOException e) {
            log.error("Failed to create csv", e);
            throw new ServiceException("Failed to create csv", e);
        }

        try {
            writeCsvFile(file, survey);
        } catch (IOException e) {
            log.error("Failed to write csv", e);
            throw new ServiceException("Failed to write csv", e);
        }
    }

    public void writeCsvFile(File csvFile, Survey survey) throws IOException {
        final String[] headers = {
            "Nom",
            "Objectif",
            "Date_debut",
            "Date_fin",
            "producteur",
            "proprietaire",
            "contact_proprietaire",
            "contact_producteur",
            "confidentialite",
            "commentaire_confidentialite",
            "EPSG"
        };

        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(headers).setDelimiter(';').build();
        try (
            BufferedWriter writer = Files.newBufferedWriter(csvFile.toPath());

            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)
        ) {
            csvPrinter.printRecord(
                survey.getNameAndLocation(),
                survey.getGoal(),
                LocalDate.ofInstant(survey.getStartDate(), UTC).format(NUMBER_DATE_PATTERN),
                LocalDate.ofInstant(survey.getEndDate(), UTC).format(NUMBER_DATE_PATTERN),
                survey.getProducerOrganization(),
                survey.getOwnerOrganization(),
                survey.getOwnerEmail(),
                survey.getTechnicalManagerEmail(),
                getBroadcastRestrictionForCsv(survey.getBroadcastRestriction()),
                survey.getConfidentialityInformation(),
                survey.getEpsg().trim()
            );

            csvPrinter.flush();
        }
    }

    public static String getBroadcastRestrictionForCsv(SurveyBroadcastRestriction broadcastRestriction) {
        if (broadcastRestriction == null) {
            return "";
        }
        switch (broadcastRestriction) {
            case TEN_YEARS:
                return "10ans";
            case THREE_YEARS:
                return "3ans";
            case WITHOUT_BROADCAST_RESTRICTION:
            default:
                return "sans_restriction";
        }
    }

    public static String getBroadcastRestrictionForPdf(SurveyBroadcastRestriction broadcastRestriction) {
        if (broadcastRestriction == null) {
            return "";
        }
        switch (broadcastRestriction) {
            case TEN_YEARS:
                return "10 ans";
            case THREE_YEARS:
                return "3 ans";
            case WITHOUT_BROADCAST_RESTRICTION:
            default:
                return "Sans restriction de diffusion";
        }
    }

    public static String getCoordinatesOrder(SurveyCoordinatesOrder coordinatesOrder) {
        if (coordinatesOrder == null) {
            return "";
        }
        switch (coordinatesOrder) {
            case XYZ:
                return "XYZ";
            case LAT_LONG_Z:
                return "Lat-Long-Z";
            case LONG_LAT_Z:
                return "Long-Lat-Z";
            default:
                return "";
        }
    }

    public static String getActivity(SurveyActivity activity) {
        if (activity == null) {
            return "";
        }

        switch (activity) {
            case EXCAVATION_GEOPHYSICAL:
                return "Activités de fouille et levé géophysique (article L413-1 du code minier)";
            case MARINE_SCIENTIFIC_RESEARCH:
                return "Activités de recherche scientifique marine (article L251-3 du code de la recherche)";
            case PUBLIC_SERVICE:
                return "Activités de service public (CRPA)";
            case PARTICIPATORY_BATHYMETRY:
                return "Bathymétrie participative";
            case OTHER:
            default:
                return "Autres activités";
        }
    }

    public static String getZDirection(SurveyZDirection zDirection) {
        if (zDirection == null) {
            return "";
        }

        switch (zDirection) {
            case ALTITUDE:
                return "Altitude (positif vers le haut)";
            case DEPTH:
                return "Profondeur (positif vers le bas)";
            default:
                return "";
        }
    }

    public static String getFileSeparator(String separator) {
        if (separator == null) {
            return "";
        }
        switch (separator) {
            case "SPACE":
                return "Espace";
            case "TAB":
                return "Tabulation";
            case "COMMA":
                return "Virgule";
            case "SEMICOLON":
                return "Point-virgule";
            default:
                return separator;
        }
    }

    public static String getVerticalRef(SurveyVerticalRef verticalRef) {
        if (verticalRef == null) {
            return "";
        }
        switch (verticalRef) {
            case AVERAGE_LEVEL:
                return "Niveau moyen";
            case INSTANT_SEA_LEVEL:
                return "Niveau instantané de la mer (non réduit de la marée)";
            case ZERO_ALTI:
                return "Zéro altimétrique";
            case ZERO_HYDRO:
                return "Zéro hydrographique";
            case OTHER:
            default:
                return "Autre";
        }
    }

    public void copySurveyFiles(Survey survey, Path targetDirPath) throws ServiceException {
        SurveyFileAnalysisService.prepareSurveyFiles(survey, targetDirPath);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public static void copyExternalAnalysisResultFiles(Survey survey, Path targetDirPath) throws ServiceException {
        fr.shom.ping.utils.FileUtils.mkDirIfNotExists(targetDirPath);
        final SurveyProcessingResult processingResult = survey.getProcessingResults().stream()
            .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).orElseThrow(NotFoundException::new);
        processingResult.getAttachments().stream()
            .map(Attachment::getLocation)
            .map(StringUtils::cleanPath)
            .map(Paths::get)
            .forEach(throwingConsumerWrapper(source -> copySurveyFileToDirectory(source, targetDirPath)));
    }

    public static void cleanWorkingDir(File workingDirFile) throws ServiceException {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(workingDirFile);
        } catch (IOException e) {
            log.error("Failed to clean " + workingDirFile, e);
            throw new ServiceException("Failed to clean " + workingDirFile, e);
        }
    }
}
