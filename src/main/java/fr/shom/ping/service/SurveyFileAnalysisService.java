package fr.shom.ping.service;

import com.github.junrar.exception.RarException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.SurveyFileInArchive;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.SurveyFileAnalysisError;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.enums.SurveyTransmissionStatus;
import fr.shom.ping.model.enums.SurveyVerticalRef;
import fr.shom.ping.model.enums.SurveyZDirection;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.utils.CompressedArchiveUtil;
import fr.shom.ping.utils.SurveyUtil;
import fr.shom.ping.utils.survey.IDecompressMethod;
import fr.shom.ping.utils.survey.SurveyStatistics;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.EXCEED_ATTEMPTS_OR_WRONG_SEPARATOR;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.FAILED_TO_ANALYZE;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.FAILED_TO_ANALYZE_FILE_IN_ARCHIVE;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.NOT_NUMERIC_VALUE;
import static fr.shom.ping.model.enums.SurveyFileAnalysisError.WRONG_NB_OF_SEPARATOR;
import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.COMPLIANT;
import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.NOT_COMPLIANT;
import static fr.shom.ping.model.enums.SurveyFileComplianceAnalysisState.PENDING;
import static fr.shom.ping.utils.CompressedArchiveUtil.decompressGzip;
import static fr.shom.ping.utils.CompressedArchiveUtil.listFilesAndDirectories;
import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
@Service
@Slf4j
public class SurveyFileAnalysisService {

    public static final String BATHY_FILES_FOLDER = "Bathy";

    public static final String OTHER_FILES_FOLDER = "Autres";

    public static final String METADATA_FILES_FOLDER = "Metadata";

    private final SseNotificationService sseNotificationService;

    private final SurveyRepository repository;

    private final PingProperties pingProperties;

    private int currentCount = 0;

    private int previousCount = 0;

    private final AtomicInteger linesCounter = new AtomicInteger(0);

    @Getter
    private Path workingDir;

    public void initWorkingDir(Survey survey) {
        this.workingDir = Paths.get(pingProperties.getTempFolderForArchiveFiles(), survey.getIdentifier() + "-" + Instant.now().toEpochMilli());
    }

    @Async
    @Transactional
    public void analyze(Survey survey) {
        analyzeSurvey(survey);
    }

    public boolean analyzeSync(Survey survey) {
        return analyzeSurvey(survey);
    }

    public boolean analyzeSurvey(Survey survey) {
        log.info("Analyzing survey " + survey.getId() + "...");
        survey.setCompliant(PENDING);
        initWorkingDir(survey);
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            log.error("Failed to create working directory: " + workingDir, e);
            survey.setCompliant(NOT_COMPLIANT);
            sseNotificationService.sendSurveyNotification(survey, NotificationType.SURVEY_ANALYSIS_FAILURE);
            return false;
        }

        sseNotificationService.sendSurveyNotification(survey, NotificationType.SURVEY_ANALYSIS_IN_PROGRESS);
        survey.getFiles().forEach(surveyFile -> {
            try {
                surveyFile.setCompliant(PENDING);
                analyzeFile(survey, surveyFile);
            } catch (ServiceException | IOException e) {
                log.warn("Failed to process: " + surveyFile.getPath(), e);
                surveyFile.setCompliant(NOT_COMPLIANT);
                surveyFile.getCompliantAnalysisResults().put(FAILED_TO_ANALYZE, "Failed to analyze file: " + surveyFile.getPath());
            }
        });

        try {
            deleteWorkingDirectory(workingDir);
        } catch (IOException e) {
            log.warn("Failed to delete working directory: " + workingDir, e);
        }

        final boolean atLeastOneFileIsNotCompliant = survey.getFiles().stream().anyMatch(surveyFile -> surveyFile.getCompliant() == NOT_COMPLIANT);
        survey.setCompliant(atLeastOneFileIsNotCompliant ? NOT_COMPLIANT : COMPLIANT);

        if (survey.getFiles().stream().anyMatch(surveyFile -> surveyFile.getCompliant() == COMPLIANT)
            || survey.getFiles().stream().map(SurveyFile::getFilesInArchive).flatMap(Collection::stream)
            .anyMatch(surveyFileInArchive -> surveyFileInArchive.getCompliant() == COMPLIANT)
        ) {
            // compile stats for survey
            final SurveyStatistics surveyStatistics = SurveyUtil.compileAllStatistics(
                survey.getFiles().stream().map(SurveyFile::getFileStats).filter(Objects::nonNull).collect(Collectors.toList())
            );
            try {
                surveyStatistics.updateSurvey(survey);
            } catch (ServiceException e) {
                log.error("Failed to update survey with stats", e);
            }
        }

        if (canBeAnalyzedByExternalProcess(survey)) {
            // prepare files to be provided to SFTP
            try {
                prepareFilesForExternalAnalysis(survey);
            } catch (ServiceException e) {
                log.error("Failed to prepare files for external analysis", e);
            }
        }

        repository.save(survey);
        final NotificationType notificationType = survey.getCompliant() == COMPLIANT ? NotificationType.SURVEY_ANALYSIS_SUCCESS :
            NotificationType.SURVEY_ANALYSIS_FAILURE;
        try {
            sseNotificationService.sendSurveyNotification(survey, notificationType);
        } catch (Exception e) {
            log.warn("Sse notification", e);
        }
        return true;
    }

    public boolean canBeAnalyzedByExternalProcess(Survey survey) {
        return ((survey.getState() == SurveyState.TRANSMITTED || survey.getTransmissionStatus() == SurveyTransmissionStatus.PENDING)
            && survey.getExternalAnalysisEnabled()
            || survey.getState() == SurveyState.TEST)
            && survey.getVerticalRef() == SurveyVerticalRef.ZERO_HYDRO
            && survey.getCompliant() == COMPLIANT;
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    //@Async
    public void prepareFilesForExternalAnalysis(Survey survey) throws ServiceException {
        final String surveyId = survey.getId().toString();
        final Path surveyRootDirPath = Paths.get(StringUtils.cleanPath(Paths.get(pingProperties.getCartonautSftp(), surveyId).toString()));
        prepareSurveyFiles(survey, surveyRootDirPath);
    }

    public static void prepareSurveyFiles(Survey survey, Path surveyRootDirPath) throws ServiceException {
        // SURVEY_ID/
        //    Bathy/
        //        bathy files
        //    Autres/
        //        misc files
        //    Metadata/
        //        PDF file
        //    Results/
        //        CALHYPSO result files (not managed by PING)
        // (0) list required folders
        final Path bathyFilesDirPath = Path.of(StringUtils.cleanPath(Path.of(surveyRootDirPath.toString(), BATHY_FILES_FOLDER).toString()));
        final Path otherFilesDirPath = Path.of(StringUtils.cleanPath(Path.of(surveyRootDirPath.toString(), OTHER_FILES_FOLDER).toString()));
        final Path metadataFilesDirPath = Path.of(StringUtils.cleanPath(Path.of(surveyRootDirPath.toString(), METADATA_FILES_FOLDER).toString()));

        // (1) create required folders if not exists
        createDirectoriesIfNotExists(surveyRootDirPath, bathyFilesDirPath, otherFilesDirPath, metadataFilesDirPath);

        // (2) clean folders if they previously exists
        cleanDir(bathyFilesDirPath, otherFilesDirPath, metadataFilesDirPath);

        // (3) copy files to SFTP repo
        copySurveyFilesToExternalRepo(survey, bathyFilesDirPath, otherFilesDirPath, metadataFilesDirPath);
    }

    public static void copySurveyFilesToExternalRepo(Survey survey, Path bathyFilesDirPath, Path otherFilesDirPath, Path metadataFilesDirPath)
        throws ServiceException {
        // (1) copy bathy files
        survey.getFiles().stream()
            .map(SurveyFile::getAttachment)
            .map(Attachment::getLocation)
            .map(StringUtils::cleanPath)
            .map(Paths::get)
            .forEach(throwingConsumerWrapper(source -> copySurveyFileToDirectory(source, bathyFilesDirPath)));

        // (2) copy misc files
        survey.getMiscAttachments().stream()
            .map(Attachment::getLocation)
            .map(StringUtils::cleanPath)
            .map(Paths::get)
            .forEach(throwingConsumerWrapper(source -> copySurveyFileToDirectory(source, otherFilesDirPath)));

        // (3) copy metadata PDF file
        if (survey.getTechnicalMetadataFile() != null) {
            copySurveyFileToDirectory(Path.of(StringUtils.cleanPath(survey.getTechnicalMetadataFile().getLocation())), metadataFilesDirPath);
        }
    }

    public static void copySurveyFileToDirectory(Path source, Path target) throws ServiceException {
        try {
            log.debug("Copying " + source + " to " + target + "...");
            Files.copy(source, target.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Can't copy " + source + " to " + target, e);
            throw new ServiceException("Can't copy " + source + " to " + target, e);
        }
    }

    public static void createDirectoriesIfNotExists(Path surveyRootDirPath, Path bathyFilesDirPath, Path otherFilesDirPath, Path metadataFilesDirPath)
        throws ServiceException {
        // (1) create ID_LEVE/ folder if not exists
        fr.shom.ping.utils.FileUtils.mkDirIfNotExists(surveyRootDirPath);

        // (2) create Bathy/ if required
        fr.shom.ping.utils.FileUtils.mkDirIfNotExists(bathyFilesDirPath);

        // (3) create Autres/ if required
        fr.shom.ping.utils.FileUtils.mkDirIfNotExists(otherFilesDirPath);

        // (4) create Metadata/ if required
        fr.shom.ping.utils.FileUtils.mkDirIfNotExists(metadataFilesDirPath);
    }

    private static void cleanDir(Path bathyFilesDirPath, Path otherFilesDirPath, Path metadataFilesDirPath) throws ServiceException {
        // (1) clean Bathy/
        fr.shom.ping.utils.FileUtils.cleanDir(bathyFilesDirPath);

        // (2) clean Autres/
        fr.shom.ping.utils.FileUtils.cleanDir(otherFilesDirPath);

        // (3) clean Metadata/
        fr.shom.ping.utils.FileUtils.cleanDir(metadataFilesDirPath);
    }

    public static void deleteWorkingDirectory(Path workingDir) throws IOException {
        FileUtils.deleteDirectory(workingDir.toFile());
    }

    public void analyzeFile(Survey survey, SurveyFile surveyFile) throws ServiceException, IOException {
        final Path source = surveyFile.getPath();
        if (Files.notExists(source)) {
            throw new ServiceException("File " + source + " not found");
        }
        final String sourceFileName = source.getFileName().toString();
        final String foundExtension = FilenameUtils.getExtension(sourceFileName).toLowerCase();
        switch (foundExtension) {
            case "gz":
                analyzeGzipFile(survey, surveyFile, source);
                break;
            case "zip":
                analyzeZipFile(survey, surveyFile, source);
                break;
            case "rar":
                analyzeRarFile(survey, surveyFile, source);
                break;
            case "7z":
                analyze7zFile(survey, surveyFile, source);
                break;
            default:
                analyzeSeparatorForSingleFile(survey, surveyFile);
        }
    }

    public void analyzeCompressedArchive(Survey survey, SurveyFile surveyFile, Path source, IDecompressMethod decompressMethod) throws ServiceException {
        final String sourceFileName = source.getFileName().toString();
        final String foundExtension = FilenameUtils.getExtension(sourceFileName);
        final String targetFolderName = sourceFileName.substring(0, sourceFileName.length() - foundExtension.length() - 1);
        final Path target = Path.of(workingDir.toString(), targetFolderName);

        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            log.error("Failed to decompress file '" + source + "' in folder '" + target + "' as it can be created", e);
            throw new ServiceException("Failed to decompress file '" + source + "' in folder '" + target + "' as it can be created");
        }

        try {
            decompressMethod.decompress(source, target);
            analyzeArchiveContent(survey, surveyFile, target);
        } catch (IOException | RarException e) {
            log.error("Failed to decompress file '" + source + "' as '" + target + "'", e);
            throw new ServiceException("Failed to decompress file '" + source + "' as '" + target + "'");
        }
    }

    public void analyzeZipFile(Survey survey, SurveyFile surveyFile, Path source) throws ServiceException {
        final IDecompressMethod zipMethod = CompressedArchiveUtil::decompressZip;
        analyzeCompressedArchive(survey, surveyFile, source, zipMethod);
    }

    public void analyzeRarFile(Survey survey, SurveyFile surveyFile, Path source) throws ServiceException {
        final IDecompressMethod zipMethod = CompressedArchiveUtil::decompressRar;
        analyzeCompressedArchive(survey, surveyFile, source, zipMethod);
    }

    public void analyze7zFile(Survey survey, SurveyFile surveyFile, Path source) throws ServiceException {
        final IDecompressMethod archive7zMethod = CompressedArchiveUtil::decompress7z;
        analyzeCompressedArchive(survey, surveyFile, source, archive7zMethod);
    }

    public Set<String> getAllowedExtensions() {
        return pingProperties.getUploadExtensionsForSurveyBathy().stream()
            .filter(ext -> !List.of("gz", "tar", "zip", "rar", "7z").contains(ext))     // remove archives from deeper analysis
            .collect(toSet());
    }

    public void analyzeGzipFile(Survey survey, SurveyFile surveyFile, Path source) throws ServiceException, IOException {
        final String sourceFileName = source.getFileName().toString();
        String foundExtension = FilenameUtils.getExtension(sourceFileName);
        String targetFileName = sourceFileName.substring(0, sourceFileName.length() - foundExtension.length() - 1);

        boolean tar = "tar".equalsIgnoreCase(FilenameUtils.getExtension(targetFileName));
        if (tar) {
            foundExtension = FilenameUtils.getExtension(targetFileName);
            targetFileName = targetFileName.substring(0, targetFileName.length() - foundExtension.length() - 1);
        }
        final Path target = Path.of(workingDir.toString(), targetFileName);
        if (tar) {
            Files.createDirectories(target);
        }
        try {
            decompressGzip(source, target, tar);
            if (!tar) {
                analyzeSeparator(survey, surveyFile, target);
            } else {
                analyzeArchiveContent(survey, surveyFile, target);
            }
        } catch (IOException e) {
            log.error("Failed to decompress gzip file '" + source + "' as '" + target + "'", e);
            throw new ServiceException("Failed to decompress gzip file '" + source + "' as '" + target + "'");
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    private void analyzeArchiveContent(Survey survey, SurveyFile surveyFile, Path target) throws IOException {
        surveyFile.setArchive(true);
        surveyFile.setFilesInArchive(newHashSet());
        final Set<String> tarContent = listFilesAndDirectories(target.toFile().getAbsolutePath(), MAX_VALUE, getAllowedExtensions());
        tarContent.forEach(file -> analyzeSeparator(survey, surveyFile, Path.of(file)));

        final boolean atLeastOneFileIsNotCompliant =
            surveyFile.getFilesInArchive().stream().anyMatch(surveyFileInArchive -> surveyFileInArchive.getCompliant() == NOT_COMPLIANT);
        surveyFile.setCompliant(atLeastOneFileIsNotCompliant ? NOT_COMPLIANT : COMPLIANT);

        final SurveyStatistics surveyFileStatistics =
            SurveyUtil.compileAllStatistics(surveyFile.getFilesInArchive().stream().map(SurveyFileInArchive::getFileStats).collect(Collectors.toList()));
        surveyFileStatistics.updateSurveyFile(surveyFile);
    }

    public void analyzeSeparatorForSingleFile(Survey survey, SurveyFile surveyFile) {
        final Path filePath = surveyFile.getPath();
        analyzeSeparator(survey, surveyFile, filePath);
    }

    public String getSurveyFileInArchiveFilename(SurveyFile surveyFile, String filePath, String workingDir) {
        final String archiveFileName = surveyFile.getAttachment().getFilename();
        final String parent = Path.of(workingDir, archiveFileName.replace("." + FilenameUtils.getExtension(archiveFileName), "")).toString()
            .replace("\\", "/");
        String filename = filePath.replace("\\", "/").replace(parent, "");
        if (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
        return filename;
    }

    public void analyzeSeparator(Survey survey, SurveyFile surveyFile, Path filePath) {
        final SurveyFileInArchive surveyFileInArchive = surveyFile.isArchive() ?
            SurveyFileInArchive.builder()
                .filename(getSurveyFileInArchiveFilename(surveyFile, filePath.toString(), workingDir.toString()))
                .cartonautFileId(SurveyUtil.generateBathyFileId())
                .build()
            : null;

        final Point2D[] bounds = !survey.isGeographicFormat() ? SurveyUtil.getEnvelopeFromEpsgCode(SurveyUtil.getEpsgCode(survey.getEpsg())) : null;
        final Map<SurveyFileAnalysisError, String> errors = new HashMap<>();
        final SurveyStatistics statistics = SurveyStatistics.create(survey.getZDirection());
        try (Stream<String> lines = Files.lines(filePath)) {
            linesCounter.set(0);
            boolean fileOk = lines.allMatch(line -> isLineCompliant(line, survey.getFileSeparator(), survey, bounds, statistics, errors));
            if (surveyFile.isArchive() && surveyFileInArchive != null) {
                surveyFileInArchive.setCompliant(fileOk ? COMPLIANT : NOT_COMPLIANT);
            } else {
                surveyFile.setCompliant(fileOk ? COMPLIANT : NOT_COMPLIANT);
            }

            if (!fileOk) {
                survey.setCompliant(NOT_COMPLIANT);
                // set error msg to file
                if (surveyFile.isArchive()) {
                    surveyFile.setCompliant(NOT_COMPLIANT);
                    log.warn("archive " + surveyFile.getPath() + " has " + errors.size() + " errors in file " + surveyFileInArchive);
                } else {
                    surveyFile.setCompliantAnalysisResults(errors);
                    log.warn("file " + surveyFile.getPath() + " has " + errors.size() + " errors");
                }
            }
            if (surveyFile.isArchive()) {
                log.debug("File analyzed: " + filePath + " from archive: " + surveyFile.getPath());
            } else {
                log.debug("File analyzed: " + filePath);
            }
        } catch (IOException e) {

            surveyFile.setCompliant(NOT_COMPLIANT);
            survey.setCompliant(NOT_COMPLIANT);

            if (surveyFile.isArchive() && surveyFileInArchive != null) {
                log.error("Failed to analyze file: " + filePath + " from archive: " + surveyFile.getPath(), e);
                errors.put(FAILED_TO_ANALYZE_FILE_IN_ARCHIVE, "Failed to analyze file: " + filePath + " from archive: " + surveyFile.getPath());
                surveyFileInArchive.setCompliant(NOT_COMPLIANT);
            } else {
                log.error("Failed to analyze file: " + filePath, e);
                errors.put(FAILED_TO_ANALYZE, "Failed to analyze file: " + filePath);
                surveyFile.setCompliantAnalysisResults(errors);
            }
        } finally {
            currentCount = 0;
            previousCount = 0;
            linesCounter.set(0);
            if (surveyFile.isArchive() && surveyFileInArchive != null) {
                surveyFileInArchive.setCompliantAnalysisResults(errors);
                statistics.updateSurveyFileInArchive(surveyFileInArchive);
                surveyFile.getFilesInArchive().add(surveyFileInArchive);
            } else {
                statistics.updateSurveyFile(surveyFile);
            }
        }
    }

    public boolean isLineCompliant(String line, String expectedSeparator, Survey survey, Point2D[] bounds, SurveyStatistics statistics,
        Map<SurveyFileAnalysisError, String> errors) {
        final int linesRead = linesCounter.incrementAndGet();
        final int offset = pingProperties.getSurveyFileAnalysisLineCompliantHeaderReadOffset();
        // loop until max attempts reached
        final String[] splitted = line.split(SurveyUtil.getRealSeparator(expectedSeparator));
        if (splitted.length < 2) {
            if (linesRead <= offset) {
                return true;
            }
            currentCount = 0;
            errors.put(EXCEED_ATTEMPTS_OR_WRONG_SEPARATOR,
                "no separator found, exceed attempts to find separator. Maybe header is too long or separator is wrong.");
            return false;
        } else if (linesRead <= offset && !isNumericValuesArray(sanitizeValuesArray(splitted))) {    // case when header such as: long(DD),lat(DD),depth(m -
            // down positive - LAT)
            return true;
        }

        previousCount = currentCount;
        currentCount = splitted.length;
        if (currentCount == previousCount || linesRead == 1 || linesRead <= offset) {
            return checkValues(sanitizeValuesArray(splitted), survey, bounds, statistics, errors);
        }
        errors.put(WRONG_NB_OF_SEPARATOR, "wrong number of separators found. Maybe a value is missing or a separator is wrong.");
        return false;
    }

    public boolean checkValues(String[] values, Survey survey, Point2D[] bounds, SurveyStatistics statistics, Map<SurveyFileAnalysisError, String> errors) {
        // check each value is a number
        final boolean numericValues = isNumericValuesArray(values);
        final int currentLineNumber = linesCounter.get();
        if (!numericValues) {
            errors.put(NOT_NUMERIC_VALUE, "not numeric value detected at line " + currentLineNumber);
            return false;
        }

        // check X,Y in chosen bounds if projection, else lon/lat
        return SurveyUtil.checkBounds(values, survey, currentLineNumber, bounds, errors)
            && SurveyUtil.getStatistics(values, currentLineNumber, survey, statistics, errors);
    }

    public static String[] sanitizeValuesArray(String[] values) {
        return Arrays.stream(values).map(value -> value.replace("\"", "")).toArray(String[]::new);
    }

    public static boolean isNumericValuesArray(String[] values) {
        return Arrays.stream(values).allMatch(NumberUtils::isParsable);
    }
}
