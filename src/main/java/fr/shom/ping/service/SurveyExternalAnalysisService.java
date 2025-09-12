package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.SurveyDanger;
import fr.shom.ping.model.SurveyDetection;
import fr.shom.ping.model.SurveyEnc;
import fr.shom.ping.model.SurveyFile;
import fr.shom.ping.model.SurveyFileInArchive;
import fr.shom.ping.model.SurveyProcessingResult;
import fr.shom.ping.model.dto.AttachmentCreateDto;
import fr.shom.ping.model.dto.SurveyAnalysisProgressDto;
import fr.shom.ping.model.dto.SurveyMetadataDlLinkDto;
import fr.shom.ping.model.dto.SurveyMetadataDto;
import fr.shom.ping.model.dto.SurveyResultFailureDto;
import fr.shom.ping.model.dto.SurveyResultSuccessDto;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.SurveyAnalysisProgressStatus;
import fr.shom.ping.model.enums.SurveyCoordinatesOrder;
import fr.shom.ping.model.enums.SurveyProcessingResultError;
import fr.shom.ping.model.enums.SurveyProcessingStatus;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.model.enums.SurveyZDirection;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.SurveyRepository;
import fr.shom.ping.utils.CompressedArchiveUtil;
import fr.shom.ping.utils.FileUtils;
import fr.shom.ping.utils.SurveyUtil;
import fr.shom.ping.utils.ZipUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import javax.xml.stream.XMLStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.exception.ThrowingConsumerWrapper.throwingConsumerWrapper;
import static fr.shom.ping.model.SurveyDetection.COLOR_LEVEL_CRITICAL;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.INVALID_ZIP_CONTENT;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.MALICIOUS_FILE_DETECTED;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.MISSING_FILE_ON_DISK;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.NO_ZIP_FILE_FOUND;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.PARSE_CSV_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.PARSE_GML_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.PARSE_XML_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.SRC_DIR_LIST_FILES_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.TARGET_DIR_LIST_CSV_FILES_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.TARGET_DIR_LIST_GML_FILES_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.TARGET_DIR_LIST_XML_FILES_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.UNKNOWN_IDFILE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.VIRUS_CHECK_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.WORKING_DIR_CLEAN_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.WORKING_DIR_CREATION_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.WRONG_IDFILE_PROVIDED;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.ZIP_CONTENT_FILES_LIST_FAILURE;
import static fr.shom.ping.model.enums.SurveyProcessingResultError.ZIP_FILENAME_NOT_MATCHING_PATTERN;
import static fr.shom.ping.utils.FileUtils.generateUniqueFileIdentifier;
import static fr.shom.ping.utils.FileUtils.mkDirIfNotExists;
import static fr.shom.ping.utils.SurveyUtil.getFilesRelativePaths;

@RequiredArgsConstructor
@Service
@Slf4j
public class SurveyExternalAnalysisService {
    public static final String RESULTS_FOLDER = "Results";

    private static final String BATHY_FILES_FOLDER = "Bathy";

    private static final Pattern ZIP_FILENAME_PATTERN = Pattern.compile("^\\d+_\\d{8}_\\d{4}\\.zip$");

    private final EntityManager entityManager;

    private final SurveyRepository repository;

    private final PingProperties pingProperties;

    private final ClamAvService clamAvService;

    private final SurveyResultParserService resultParserService;

    private final AttachmentService attachmentService;

    private final EmailNotificationService emailNotificationService;

    private final SseNotificationService sseNotificationService;

    @Transactional
    public boolean processAvailableResult(UUID surveyId, List<SurveyResultSuccessDto> resultsInfo) throws ServiceException {
        if (resultsInfo.isEmpty()) {
            log.warn("No available result provided to process");
            return true;
        }

        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);

        final SurveyProcessingResult processingResult = SurveyProcessingResult.builder()
            .status(SurveyProcessingStatus.PENDING)
            .processingDate(Instant.now())
            .build();

        final Set<String> availableZipResults =
            resultsInfo.stream().map(SurveyResultSuccessDto::getDlLink).filter(Objects::nonNull).collect(Collectors.toSet());

        // check if all dlLink are matching idPing_yyyyMMdd_hhmm.zip
        final Predicate<String> validFilenamePattern = ZIP_FILENAME_PATTERN.asPredicate();
        final boolean allDlLinkPatternValid = availableZipResults.stream().allMatch(validFilenamePattern);
        if (!allDlLinkPatternValid) {
            handleServiceException(ZIP_FILENAME_NOT_MATCHING_PATTERN, "At least one dlLink doesn't match pattern idPing_yyyyMMdd_hhmm.zip",
                processingResult, survey);
        }

        // if at least one success = false with not null dlLink, then we have a partial result so final status won't be FINISHED but SUCCESS_WITH_ERRORS
        final boolean successWithErrors = resultsInfo.stream()
            .anyMatch(surveyResultSuccessDto -> !surveyResultSuccessDto.getSuccess() && surveyResultSuccessDto.getDlLink() != null);

        // check if at least one result zip is available
        resultsInfo.stream().filter(surveyResultDto -> surveyResultDto.getMessages() != null && !surveyResultDto.getMessages().isEmpty())
            .forEach(surveyResultDto -> processingResult.getDetectedObjects().put(surveyResultDto.getId(), surveyResultDto.getMessages()));
        if (availableZipResults.isEmpty()) {
            log.warn("No available result with not null dlLink to process");
            processingResult.setStatus(SurveyProcessingStatus.FINISHED);
            survey.getProcessingResults().add(processingResult);
            repository.save(survey);
            return true;
        }

        // get result files from <cartonautSftp>/{surveyId}/Results/*.zip
        // for each file:
        // antivirus analysis
        // check if zip archive contains 4 required files:
        //  - idFile_enc.xml
        //  - idFile_detections.csv
        //  - idFile_dangers.gml
        //  - idFile_CALHYPSO_Detect.000
        //
        // if not then add error to binding result on archive file name

        // if all ok, then extract to <surveysWorkingDir>/{surveyId}/Results/
        // read each file type and aggregate content in DB (i.e. 1_enc.xml + 2_enc.xml -> TOTAL_enc.xml)
        // store geometries, metadata etc...
        // update statuses

        // (1) retrieve files
        final String resultsDirectorySource = StringUtils.cleanPath(
            Paths.get(pingProperties.getCartonautSftp(), surveyId.toString(), RESULTS_FOLDER).toString()
        );

        Set<Path> filesWithZipExtension = newHashSet();
        try {
            filesWithZipExtension = FileUtils.listFilesPerExtension(resultsDirectorySource, "zip");
        } catch (IOException e) {
            handleServiceException(SRC_DIR_LIST_FILES_FAILURE, "Failed to list files in source directory " + resultsDirectorySource, processingResult, survey,
                e);
        }

        if (filesWithZipExtension.isEmpty()) {
            handleServiceException(NO_ZIP_FILE_FOUND, "No .zip file found in source directory: " + resultsDirectorySource, processingResult, survey);
        }

        // keep only expected files from provided zip list
        filesWithZipExtension = filesWithZipExtension.stream()
            .filter(path -> availableZipResults.contains(Path.of(StringUtils.cleanPath(path.toString())).getFileName().toString()))
            .collect(Collectors.toSet());

        // check if each provided zip file has an existing file on disk
        final Set<String> fileNames = filesWithZipExtension.stream()
            .map(Path::toString)
            .map(StringUtils::cleanPath)
            .map(Paths::get)
            .map(Path::getFileName)
            .map(Path::toString)
            .collect(Collectors.toSet());
        final boolean allZipFilesKnown = availableZipResults.stream().allMatch(fileNames::contains);

        if (!allZipFilesKnown) {
            handleServiceException(MISSING_FILE_ON_DISK, "At least one zip file not found in directory " + resultsDirectorySource, processingResult, survey);
        }

        // check if each zip file id matches idFile extracted from not null dlLink
        final Set<String> availableZipResultsId = resultsInfo.stream().map(SurveyResultSuccessDto::getId).collect(Collectors.toSet());
        final Set<String> availableZipResultsIdFileExtracted = resultsInfo.stream().map(SurveyResultSuccessDto::getDlLink)
            .filter(Objects::nonNull)
            .map(SurveyExternalAnalysisService::getIdFile)
            .collect(Collectors.toSet());
        final boolean allIdFileFound = availableZipResultsId.containsAll(availableZipResultsIdFileExtracted);

        if (!allIdFileFound) {
            handleServiceException(WRONG_IDFILE_PROVIDED, "At least one zip file has a different id from idFile extracted from dlLink", processingResult,
                survey);
        }


        // check if each zip file has an existing idFile in db
        final Set<String> idFileFromSurveyFiles = survey.getFiles().stream()
            .map(SurveyFile::getCartonautFileId)
            .map(String::valueOf)
            .collect(Collectors.toSet());
        final Set<String> idFileFromSurveyFilesInArchive = survey.getFiles().stream()
            .map(SurveyFile::getFilesInArchive)
            .flatMap(Collection::stream)
            .map(SurveyFileInArchive::getCartonautFileId)
            .map(String::valueOf)
            .collect(Collectors.toSet());
        final Set<String> idFileFromSurveyList = Stream.of(idFileFromSurveyFiles, idFileFromSurveyFilesInArchive)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        final boolean allIdFileKnown = idFileFromSurveyList.containsAll(availableZipResultsId);

        if (!allIdFileKnown) {
            handleServiceException(UNKNOWN_IDFILE,
                "At least one zip file has an unknown idFile provided (not found in db survey_file && survey_file_in_archive) in directory " +
                resultsDirectorySource + "\navailableZipResultsId=" + String.join(",", availableZipResultsId)
                + "\nidFileFromSurveyList=" + String.join(",", idFileFromSurveyList), processingResult, survey);
        }

        final String resultsDirectoryTarget = StringUtils.cleanPath(
            Paths.get(pingProperties.getSurveysWorkingDir(), surveyId.toString(), RESULTS_FOLDER).toString()
        );

        final Path resultsDirectoryTargetPath = Path.of(resultsDirectoryTarget);
        try {
            Files.createDirectories(resultsDirectoryTargetPath);
        } catch (IOException e) {
            handleServiceException(WORKING_DIR_CREATION_FAILURE, "Failed to create working directory " + resultsDirectoryTargetPath, processingResult, survey,
                e);
        }
        filesWithZipExtension.stream()
            .map(Path::toString)
            .map(StringUtils::cleanPath)
            .map(Paths::get)
            .forEach(throwingConsumerWrapper(path ->
                Files.copy(path, Path.of(resultsDirectoryTargetPath.toString(), path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING)
            ));

        // build files list in target directory
        final Set<Path> zipFilesInTargetDirectory = filesWithZipExtension.stream()
            .map(Path::getFileName)
            .map(Path::toString)
            .map(fileName -> Path.of(resultsDirectoryTarget, fileName))
            .collect(Collectors.toSet());

        // (2) analyze files
        final boolean anyVirusFound = zipFilesInTargetDirectory.stream().map(Path::toString).anyMatch(file -> {
            try {
                return clamAvService.hasVirus(file);
            } catch (ServiceException e) {
                handleRuntimeException(VIRUS_CHECK_FAILURE, "Failed to check virus in file " + file, processingResult, survey, e);
            }
            return false;
        });

        if (anyVirusFound) {
            handleServiceException(MALICIOUS_FILE_DETECTED, "A malicious file could have been found in source directory: " + resultsDirectorySource
                + ". Check logs for details.", processingResult, survey);
        }

        // (3) check if zip archive contains 4 required files:
        //  - idFile_enc.xml
        //  - idFile_detections.csv
        //  - idFile_dangers.gml
        //  - idFile_CALHYPSO_Detect.000

        final List<String> suffixes = List.of("_enc.xml", "_detections.csv", "_dangers.gml", "_CALHYPSO_Detect.000");

        // map association between zip name (aka dlLink) and result id
        final Map<String, String> mapZipNameIdFile = resultsInfo.stream().collect(Collectors.toMap(
            SurveyResultSuccessDto::getDlLink, SurveyResultSuccessDto::getId
        ));
        zipFilesInTargetDirectory.stream()
            .map(Path::getFileName)
            .map(Path::toString)
            .forEach(zip -> {
                final String idFileZip = mapZipNameIdFile.get(zip);
                final String fullZipPath = resultsDirectoryTarget + File.separator + zip;
                List<String> files = new ArrayList<>();
                try {
                    files = ZipUtil.listFiles(fullZipPath);
                } catch (ServiceException e) {
                    handleRuntimeException(ZIP_CONTENT_FILES_LIST_FAILURE, "Failed to list files in zip " + fullZipPath, processingResult, survey, e);
                }

                final List<String> expectedFiles = suffixes.stream().map(s -> idFileZip + s).collect(Collectors.toList());
                final boolean validZipContent = files.stream().allMatch(expectedFiles::contains);
                if (!validZipContent) {
                    handleRuntimeException(INVALID_ZIP_CONTENT, "Zip file not valid. Check content of " + fullZipPath, processingResult, survey,
                        new ServiceException("Zip file not valid. Check content of " + fullZipPath));
                }
            });

        // (4) unzip all .zip files in target directory
        zipFilesInTargetDirectory.forEach(throwingConsumerWrapper(path -> CompressedArchiveUtil.decompressZip(path, resultsDirectoryTargetPath)));

        // (5) read files
        // (5.1)  ENC xml
        parseEncFile(survey, processingResult, resultsDirectoryTarget);

        // (5.2) detections csv
        parseDetectionsFile(survey, processingResult, resultsDirectoryTarget);

        // (5.3) dangers gml
        if (pingProperties.isEnableBathyDangersIntegration()) {
            parseDangersFile(survey, processingResult, resultsDirectoryTarget);
        }

        // (6) persist data

        // .zip files are correct and without virus so we can attach them to processingResult
        attachResultZipFilesToProcessingResult(processingResult, zipFilesInTargetDirectory, mapZipNameIdFile);

        processingResult.setStatus(successWithErrors ? SurveyProcessingStatus.SUCCESS_WITH_ERRORS : SurveyProcessingStatus.FINISHED);
        survey.getProcessingResults().add(processingResult);
        repository.save(survey);

        // (7) clean working directory
        cleanWorkingDir(resultsDirectoryTargetPath, processingResult, survey);

        // (8) notify user
        notifyUserAboutExternalAnalysisEnding(survey);

        return true;
    }

    @Async
    public void notifyUserAboutExternalAnalysisEnding(Survey survey) {
        final SurveyProcessingResult processingResult = getLastProcessingResult(survey);
        final NotificationType notificationType = processingResult.getStatus() == SurveyProcessingStatus.FAILURE ?
            NotificationType.SURVEY_EXTERNAL_ANALYSIS_END_WITH_FAILURE :
            getSuccessfulSurveyExternalAnalysisNotificationType(processingResult);

        // SSE notification
        sseNotificationService.sendSurveyNotification(survey, notificationType);

        // send email
        emailNotificationService.sendSurveyExternalAnalysisNotificationEmail(survey, notificationType);
    }

    public NotificationType getSuccessfulSurveyExternalAnalysisNotificationType(SurveyProcessingResult processingResult) {
        return lookForCriticalProbes(processingResult) ? NotificationType.SURVEY_EXTERNAL_ANALYSIS_END_WITH_CRITICAL_PROBES :
            NotificationType.SURVEY_EXTERNAL_ANALYSIS_END_WITHOUT_CRITICAL_PROBE;
    }

    public static boolean lookForCriticalProbes(SurveyProcessingResult processingResult) {
        // check if any detection found and if at least one is CRITICAL (red color == 3)
        return processingResult.getDetections().stream().filter(surveyDetection -> surveyDetection.getColor() != null)
            .map(SurveyDetection::getColor).anyMatch(color -> color == COLOR_LEVEL_CRITICAL);
    }

    public void attachResultZipFilesToProcessingResult(SurveyProcessingResult processingResult, Set<Path> zipFilesInTargetDirectory,
        Map<String, String> mapIdFileZipName) {
        final List<UUID> attachmentIds = new ArrayList<>();
        zipFilesInTargetDirectory.forEach(path -> {
            final String identifier = generateUniqueFileIdentifier(path);
            final AttachmentCreateDto attachmentCreateDto = AttachmentCreateDto.builder()
                .filename(StringUtils.cleanPath(path.getFileName().toString()))
                .identifier(identifier)
                .totalSize(path.toFile().length())
                .type("application/zip")
                .build();

            // (1) copy file to upload folder
            final String target = pingProperties.getUploadFolder() + File.separator + identifier;
            try {
                mkDirIfNotExists(Path.of(target));
            } catch (ServiceException e) {
                throw new RuntimeException(e);
            }

            try {
                Files.copy(path, Path.of(target, attachmentCreateDto.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(new ServiceException("Failed to upload file " + attachmentCreateDto.getFilename() + " to " + target, e));
            }

            // (2) create attachment in database
            try {
                final UUID createdAttachmentId = attachmentService.saveAttachment(attachmentCreateDto);
                attachmentIds.add(createdAttachmentId);
            } catch (ServiceException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(new ServiceException("Failed to create attachment for file " + attachmentCreateDto.getFilename(), e));
            }
        });

        // (3) link all created attachments to processing result
        processingResult.setAttachments(attachmentService.findAllByIdIn(attachmentIds));
    }

    @Async
    public void cleanWorkingDir(Path workingDir, SurveyProcessingResult processingResult, Survey survey) {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(workingDir.toFile());   // /Results
            org.apache.commons.io.FileUtils.deleteDirectory(workingDir.getParent().toFile());   // /{survey_id}
        } catch (IOException e) {
            handleRuntimeException(WORKING_DIR_CLEAN_FAILURE, "Failed to clean working dir and its parent " + workingDir, processingResult, survey, e);
        }
    }

    public void parseDangersFile(Survey survey, SurveyProcessingResult processingResult, String resultsDirectoryTarget) throws ServiceException {
        Set<Path> filesWithGmlExtension = newHashSet();
        try {
            filesWithGmlExtension = FileUtils.listFilesPerExtension(resultsDirectoryTarget, "gml");
        } catch (IOException e) {
            handleServiceException(TARGET_DIR_LIST_GML_FILES_FAILURE, "Failed to list dangers GML files in target directory " + resultsDirectoryTarget,
                processingResult, survey, e);
        }

        final Set<SurveyDanger> surveyDangerSet = filesWithGmlExtension.stream()
            .map(path -> {
                try {
                    return resultParserService.parseDangerGmlFile(path.toString());
                } catch (XMLStreamException | IOException e) {
                    handleRuntimeException(PARSE_GML_FAILURE, "Failed to parse dangers GML file " + path, processingResult, survey, e);
                }
                return new HashSet<SurveyDanger>();
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        processingResult.setDangers(surveyDangerSet);
    }

    public void parseDetectionsFile(Survey survey, SurveyProcessingResult processingResult, String resultsDirectoryTarget) throws ServiceException {
        Set<Path> filesWithCsvExtension = newHashSet();
        try {
            filesWithCsvExtension = FileUtils.listFilesPerExtension(resultsDirectoryTarget, "csv");
        } catch (IOException e) {
            handleServiceException(TARGET_DIR_LIST_CSV_FILES_FAILURE, "Failed to list detections CSV files in target directory " + resultsDirectoryTarget,
                processingResult, survey, e);
        }

        final Set<SurveyDetection> surveyDetectionSet = filesWithCsvExtension.stream()
            .map(path -> {
                try {
                    return resultParserService.parseDetectionCsvFile(path.toString());
                } catch (IOException e) {
                    handleRuntimeException(PARSE_CSV_FAILURE, "Failed to parse detections CSV file " + path, processingResult, survey, e);
                }
                return new HashSet<SurveyDetection>();
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        processingResult.setDetections(surveyDetectionSet);
    }

    public void parseEncFile(Survey survey, SurveyProcessingResult processingResult, String resultsDirectoryTarget) throws ServiceException {
        Set<Path> filesWithXmlExtension = newHashSet();
        try {
            filesWithXmlExtension = FileUtils.listFilesPerExtension(resultsDirectoryTarget, "xml");
        } catch (IOException e) {
            handleServiceException(TARGET_DIR_LIST_XML_FILES_FAILURE, "Failed to list enc XML files in target directory " + resultsDirectoryTarget,
                processingResult, survey, e);
        }

        final Set<SurveyEnc> surveyEncSet = filesWithXmlExtension.stream()
            .map(path -> {
                try {
                    return resultParserService.parseEncXmlFile(path.toString());
                } catch (IOException | XMLStreamException e) {
                    handleRuntimeException(PARSE_XML_FAILURE, "Failed to parse enc XML file " + path, processingResult, survey, e);
                }
                return new HashSet<SurveyEnc>();
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        processingResult.setEnc(surveyEncSet);
    }


    public void handleServiceException(SurveyProcessingResultError errorType, String errorMsg, SurveyProcessingResult processingResult, Survey survey,
        Exception e) throws ServiceException {
        saveErrors(errorType, errorMsg, processingResult, survey);
        throw new ServiceException(errorMsg, e);
    }

    public void handleServiceException(SurveyProcessingResultError errorType, String errorMsg, SurveyProcessingResult processingResult, Survey survey)
        throws ServiceException {
        handleServiceException(errorType, errorMsg, processingResult, survey, null);
    }

    public void handleRuntimeException(SurveyProcessingResultError errorType, String errorMsg, SurveyProcessingResult processingResult, Survey survey,
        Throwable e) {
        saveErrors(errorType, errorMsg, processingResult, survey);
        throw new RuntimeException(e);
    }

    @Transactional
    public void saveErrors(SurveyProcessingResultError errorType, String errorMsg, SurveyProcessingResult processingResult, Survey survey) {
        final Map<SurveyProcessingResultError, String> errors = new HashMap<>();
        errors.put(errorType, errorMsg);
        processingResult.setErrors(errors);
        processingResult.setStatus(SurveyProcessingStatus.FAILURE);
        survey.getProcessingResults().add(processingResult);
        repository.save(survey);
        notifyUserAboutExternalAnalysisEnding(survey);
        log.error(errorMsg);
    }

    public static String getIdFile(String filename) {
        final int idx = filename.indexOf("_");
        return idx > 0 ? filename.substring(0, idx) : null;
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public SurveyMetadataDlLinkDto getMetadataAndDownloadLink(UUID surveyId) {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);

        final Set<UUID> result = findAvailableSurveyForAnalysis();
        if (!result.contains(survey.getId())) {
            throw new BadRequestException("Survey not ready for external analysis");
        }

        final String bathyFilesFtpDownloadDirectory = StringUtils.cleanPath(
            Paths.get(pingProperties.getCartonautSftpRootRelativePath(), surveyId.toString()).toString()
        );
        return SurveyMetadataDlLinkDto.builder()
            .dlLink(bathyFilesFtpDownloadDirectory)
            .isTest(SurveyState.TEST == survey.getState())
            .metadata(
                SurveyMetadataDto.builder()
                    .epsg(Integer.parseInt(SurveyUtil.getEpsgCode(survey.getEpsg()).replace("EPSG:", "").trim()))
                    .zUp(SurveyZDirection.ALTITUDE == survey.getZDirection())
                    .latLon(SurveyCoordinatesOrder.LAT_LONG_Z == survey.getCoordinatesOrder())
                    .build()
            )
            .files(getFilesRelativePaths(survey.getFiles()))
            .build();
    }

    @Transactional
    public void setProcessingProgressStatus(UUID surveyId, SurveyAnalysisProgressDto progressDto) throws ServiceException {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        final SurveyProcessingResult processingResult = getLastProcessingResult(survey);
        final SurveyProcessingStatus oldStatus = processingResult.getStatus();
        final SurveyProcessingStatus newStatus = SurveyAnalysisProgressDto.getSurveyProcessingStatus(progressDto.getStatus());

        final boolean processingJustStarted = oldStatus == SurveyProcessingStatus.NOT_AVAILABLE
            && SurveyAnalysisProgressDto.getSurveyAnalysisProgressStatus(progressDto.getStatus()) == SurveyAnalysisProgressStatus.STARTED;
        if (processingJustStarted) {
            processingResult.setProcessingDate(Instant.now());
        }

        processingResult.setStatus(newStatus);
        processingResult.setExternalAnalysisStatus(SurveyAnalysisProgressDto.getSurveyAnalysisProgressStatus(progressDto.getStatus()));
        processingResult.setExternalAnalysisProgress(progressDto.getProgress());
        repository.save(survey);
    }

    @Transactional
    public void processAnalysisFailure(UUID surveyId, SurveyResultFailureDto resultFailureDto) throws ServiceException {
        final Survey survey = repository.findById(surveyId).orElseThrow(NotFoundException::new);
        final SurveyProcessingResult processingResult = getLastProcessingResult(survey);
        processingResult.setStatus(SurveyProcessingStatus.FAILURE);
        processingResult.getDetectedObjects().put("message", resultFailureDto.getMessage());
        processingResult.setProcessingDate(Instant.now());
        repository.save(survey);
        notifyUserAboutExternalAnalysisEnding(survey);
    }

    public static SurveyProcessingResult getLastProcessingResult(Survey survey) {
        return survey.getProcessingResults().stream()
            .max(Comparator.comparing(SurveyProcessingResult::getProcessingDate)).orElseThrow(NotFoundException::new);
    }

    public Set<UUID> findAvailableSurveyForAnalysis() {
        final Query query = entityManager.createNativeQuery("""
            SELECT s.id FROM survey s
            WHERE ((s.state = 'TRANSMITTED' AND s.external_analysis_enabled = true) OR s.state = 'TEST')
            AND s.vertical_ref = 'ZERO_HYDRO'
            AND s.compliant = 'COMPLIANT'
            AND s.id NOT IN (
                SELECT last_survey_processing_status_by_date.survey_id FROM (
                    SELECT DISTINCT sprl.survey_id, max(spr.processing_date) as last_processing_date, spr.status from survey_processing_result_list sprl
                    LEFT JOIN survey_processing_result spr on sprl.processing_result_id = spr.id
                    WHERE spr.status IN ('FINISHED', 'FAILURE')
                    GROUP BY sprl.survey_id, spr.status) as last_survey_processing_status_by_date
            )
            """);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        final List<UUID> surveyIds = query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
        return new HashSet<>(surveyIds);
    }
}
