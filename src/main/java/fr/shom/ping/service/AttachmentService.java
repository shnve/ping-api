package fr.shom.ping.service;

import com.google.common.base.Throwables;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.AbstractAttachment;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.AttachmentChunk;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.MaritimeEntityReference;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningAttachment;
import fr.shom.ping.model.dto.AttachmentCreateDto;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.NotionType;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.AttachmentChunkRepository;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.validation.ValidFileExtensionValidator;
import fr.shom.ping.validation.ValidFileSizeValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {
    private static final Integer MB = 1024 * 1024;
    private static final Integer GB = MB * 1024;

    private static final String CHUNK_SEPARATOR = "-";

    private final AttachmentRepository repository;
    private final WarningRepository warningRepository;
    private final GeoregulationRepository georegulationRepository;
    private final AttachmentChunkRepository attachmentChunkRepository;
    private final PingProperties pingProperties;
    private final ClamAvService clamAvService;

    private final fr.shom.ping.utils.FileUtils fileUtils;

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    @Transactional
    public UUID create(AttachmentCreateDto attachmentCreateDto) throws ServiceException, IOException {
        Attachment attachment = Attachment.fromDto(attachmentCreateDto);

        final String identifier = StringUtils.cleanPath(attachment.getIdentifier());
        String folder = pingProperties.getUploadFolder() + File.separator + identifier;
        String filename = StringUtils.cleanPath(attachment.getFilename());
        String file = folder + File.separator + filename;
        final Path filePath = Path.of(file);

        try {
            Files.createDirectories(filePath.getParent());
            Files.deleteIfExists(filePath);
            Files.createFile(filePath);
        } catch (IOException e) {
            log.error("Can't create file " + file, e);
            throw new ServiceException("Can't create file " + file);
        }

        try (Stream<Path> stream = Files.list(Path.of(folder))) {
            stream.filter(path -> !path.getFileName().toString().equals(filename))
                .sorted((firstFile, secondFile) -> {
                    String firstFilename = firstFile.getFileName().toString();
                    String secondFilename = secondFile.getFileName().toString();
                    int firstIdentifier = firstFilename.lastIndexOf(CHUNK_SEPARATOR);
                    int secondIdentifier = secondFilename.lastIndexOf(CHUNK_SEPARATOR);
                    return Integer.valueOf(secondFilename.substring(secondIdentifier)).compareTo(Integer.valueOf(firstFilename.substring(firstIdentifier)));
                })
                .forEach(path -> {
                    try {
                        Files.write(filePath, Files.readAllBytes(path), StandardOpenOption.APPEND);
                        Files.delete(path);
                    } catch (IOException e) {
                        Throwables.throwIfUnchecked(e);
                        throw new RuntimeException(e);
                    }
                });
        } catch (IOException e) {
            throw new ServiceException("Fail to concat chunk for file " + file);
        }

        attachmentChunkRepository.deleteByIdentifier(attachment.getIdentifier());

        // check file extension from local list
        if (!checkFileExtension(filename, attachmentCreateDto.getIsSurvey(), attachmentCreateDto.getIsBathy(), attachmentCreateDto.getIsTechnicalMetadata())) {
            Files.deleteIfExists(filePath);
            throw new ServiceException("Extension not allowed. Upload aborted for : " + file);
        }

        // check file content and extension
        final boolean validFileMimeType = fileUtils.validateMimeType(new File(folder, FilenameUtils.getName(filename)));
        if (!validFileMimeType) {
            Files.deleteIfExists(filePath);
            throw new ServiceException("File extension and content don't match. Seems to be a suspicious file. Upload aborted for : " + file);
        }

        // Antivirus analysis
        boolean scanResult = clamAvService.hasVirus(file);
        if (scanResult) {
            log.warn("Malicious file detected. Upload aborted for : " + file);
            throw new ServiceException("clamav.detection.positive");
        }

        String newIdentifier = identifier.replace(identifier.substring(0, identifier.indexOf("-")), String.valueOf(Instant.now().toEpochMilli()));

        String location = pingProperties.getUploadFolder() + File.separator + newIdentifier + File.separator + filename;

        FileUtils.moveDirectory(new File(folder), new File(pingProperties.getUploadFolder() + File.separator + newIdentifier));

        attachment.setIdentifier(newIdentifier);
        attachment.setLocation(location);
        return repository.save(attachment).getId();
    }

    @Transactional
    public UUID saveAttachment(AttachmentCreateDto attachmentCreateDto) throws ServiceException, IOException {
        final Attachment attachment = Attachment.fromDto(attachmentCreateDto);

        final String identifier = StringUtils.cleanPath(attachment.getIdentifier());
        final String folder = pingProperties.getUploadFolder() + File.separator + identifier;
        final String filename = StringUtils.cleanPath(attachment.getFilename());
        final String file = folder + File.separator + filename;

        // check file extension from local list
        if (!checkFileExtension(filename, attachmentCreateDto.getIsSurvey(), attachmentCreateDto.getIsBathy(), attachmentCreateDto.getIsTechnicalMetadata())) {
            throw new ServiceException("Extension not allowed. Upload aborted for : " + file);
        }

        // check file content and extension
        final boolean validFileMimeType = fileUtils.validateMimeType(new File(file));
        if (!validFileMimeType) {
            throw new ServiceException("File extension and content don't match. Seems to be a suspicious file. Upload aborted for : " + file);
        }

        // Antivirus analysis
        boolean scanResult = clamAvService.hasVirus(file);
        if (scanResult) {
            log.warn("Malicious file detected. Upload aborted for : " + file);
            throw new ServiceException("clamav.detection.positive");
        }

        final String location = pingProperties.getUploadFolder() + File.separator + identifier + File.separator + filename;
        attachment.setLocation(location);
        return repository.save(attachment).getId();
    }

    @Transactional
    public void saveChunk(AttachmentChunk chunk) throws ServiceException {
        MultipartFile file = chunk.getFile();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceException();
        }
        StringBuilder path = new StringBuilder();
        path.append(pingProperties.getUploadFolder()).append(File.separator).append(StringUtils.cleanPath(chunk.getIdentifier()));

        final Path pathDir = Path.of(path.toString());
        if (!Files.isWritable(pathDir)) {
            try {
                Files.createDirectories(pathDir);
            } catch (FileAlreadyExistsException e) {
                throw new ServiceException("Directory " + path + " already exists");
            } catch (IOException e) {
                throw new ServiceException("Can't create directory " + path);
            }
        }

        path.append(File.separator).append(StringUtils.cleanPath(chunk.getFilename())).append(CHUNK_SEPARATOR).append(chunk.getChunkNumber());

        Path systemPath = Path.of(path.toString());
        try {
            Files.write(systemPath, bytes);
        } catch (FileAlreadyExistsException e) {
            throw new ServiceException("File " + path + " already exists");
        } catch (IOException e) {
            throw new ServiceException("Can't create file " + path);
        }

        attachmentChunkRepository.save(chunk);
    }

    public boolean checkChunk(String identifier, Integer chunkNumber) {
        Specification<AttachmentChunk> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("identifier"), identifier));
            predicates.add(criteriaBuilder.equal(root.get("chunkNumber"), chunkNumber));

            return criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
        };

        return attachmentChunkRepository.findOne(specification).orElse(null) == null;
    }

    @Transactional
    public void deleteFromDisk(UUID id) throws ServiceException {
        Attachment attachment = repository.findById(id).orElseThrow(() -> new ServiceException("Attachment with id " + id + " does not exists."));
        deleteAttachmentFile(attachment);
    }

    @Transactional
    public void deleteFromDiskAndDatabase(UUID id) throws ServiceException {
        deleteFromDisk(id);
        repository.deleteById(id);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void deleteAttachmentFile(Attachment attachment) throws ServiceException {
        File attachmentDirectory = new File(pingProperties.getUploadFolder(), FilenameUtils.getName(StringUtils.cleanPath(attachment.getIdentifier())));
        try {
            FileUtils.deleteDirectory(attachmentDirectory);
        } catch (IOException e) {
            log.error("Failed to delete attachment with id " + attachment.getId() + " and directory " + attachmentDirectory, e);
        }
    }

    public List<Attachment> findAllByIdIn(List<UUID> ids) {
        return Optional.ofNullable(ids)
            .map(this.repository::findAllByIdIn)
            .orElseGet(ArrayList::new);
    }

    public ResponseEntity<byte[]> getFile(UUID id) throws ServiceException {
        final Attachment attachment = repository.findById(id).orElseThrow(() -> new ServiceException("Attachment with id " + id + " does not exists."));
        final File attachmentFile = Path.of(pingProperties.getUploadFolder(), attachment.getIdentifier(), attachment.getFilename()).toFile();
        final byte[] fileContentAsByteArray;
        try {
            fileContentAsByteArray = IOUtils.toByteArray(new FileInputStream(attachmentFile));
        } catch (IOException e) {
            final String errorMsg = "Attachment with id " + id + " cannot be read from filesystem.";
            log.error(errorMsg, e);
            throw new ServiceException(errorMsg);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Expose-Headers", "filename");
        headers.add("filename", attachment.getFilename());
        headers.add("Content-Type", attachment.getType());
        return (new ResponseEntity<>(fileContentAsByteArray, headers, HttpStatus.OK));
    }

    public Attachment clone(Attachment attachment) throws ServiceException {
        String attachmentIdentifier = StringUtils.cleanPath(attachment.getIdentifier());
        String attachmentDirectory = pingProperties.getUploadFolder() + File.separator + attachmentIdentifier;
        String attachmentLocation = attachmentDirectory + File.separator + StringUtils.cleanPath(attachment.getFilename());

        String clonedAttachmentIdentifier = attachmentIdentifier
            .replace(attachmentIdentifier.substring(0, attachmentIdentifier.indexOf("-")), UUID.randomUUID().toString()
                .replace("-", "")
            );
        String clonedAttachmentDirectory = pingProperties.getUploadFolder() + File.separator + clonedAttachmentIdentifier;
        String clonedAttachmentLocation = clonedAttachmentDirectory + File.separator + StringUtils.cleanPath(attachment.getFilename());

        try {
            Files.createDirectory(Path.of(clonedAttachmentDirectory));
            Files.copy(Path.of(attachmentLocation), Path.of(clonedAttachmentLocation));
        } catch (IOException e) {
            throw new ServiceException("Can't copy " + attachment.getFilename());
        }

        return Attachment.builder()
            .creationDate(attachment.getCreationDate())
            .filename(attachment.getFilename())
            .identifier(clonedAttachmentIdentifier)
            .totalSize(attachment.getTotalSize())
            .type(attachment.getType())
            .location(clonedAttachmentLocation)
            .build();
    }

    public String checkFileRequirements(String filename, Long fileSize, Boolean isBathy, Boolean isSurvey, Boolean isTechnicalMetadata) {
        if (!checkFileSize(fileSize, isSurvey)) {
            return ValidFileSizeValidator.FIELD_ERROR_MESSAGE;
        }
        if (!checkFileExtension(filename, isSurvey, isBathy, isTechnicalMetadata)) {
            return ValidFileExtensionValidator.FIELD_ERROR_MESSAGE;
        }
        return "";
    }

    public boolean checkFileSize(Long fileSize, boolean isSurvey) {
        if (fileSize == null) {
            return true;
        }
        final long maxSize = isSurvey ? (long) pingProperties.getMaxUploadFileSizeInGigabytesForSurvey() * GB :
            (long) pingProperties.getMaxUploadFileSizeInMegabytes() * MB;
        return fileSize < maxSize;
    }

    public boolean checkFileExtension(String filename, boolean isSurvey, boolean isBathy, boolean isTechnicalMetadata) {
        if (isTechnicalMetadata) {
            return "pdf".equalsIgnoreCase(FilenameUtils.getExtension(filename));
        }
        final List<String> surveyExtensions = isBathy ? pingProperties.getUploadExtensionsForSurveyBathy() : pingProperties.getUploadExtensionsForSurveyMisc();
        final List<String> extensions = isSurvey ? surveyExtensions : pingProperties.getUploadExtensions();
        return extensions.stream().map(String::toLowerCase).collect(Collectors.toList()).contains(FilenameUtils.getExtension(filename).toLowerCase());
    }

    public ResponseEntity<byte[]> getPublicFileByNotionType(UUID id, NotionType notionType, UUID notionId) throws ServiceException {
        if (!newHashSet(NotionType.WARNING, NotionType.WARNING_TP, NotionType.GEOREGULATION).contains(notionType)) {
            throw new ForbiddenException();
        }

        boolean fileIsPresent;
        boolean notionStatusIsDraft;

        if (newHashSet(NotionType.WARNING, NotionType.WARNING_TP).contains(notionType)) {
            final Warning warning = warningRepository.findById(notionId).orElseThrow(NotFoundException::new);
            notionStatusIsDraft = warning.getStatus() == WarningStatus.DRAFT;
            Set<UUID> attachmentIdsOfWarningAsPublicReference = warning.getAttachments().stream()
                .filter(WarningAttachment::getIsPublicReference)
                .map(WarningAttachment::getAttachment)
                .map(AbstractAttachment::getId)
                .collect(toSet());
            Set<UUID> attachmentIdsOfEntityReferenceAsPublicReference = warning.getEntityReferences().stream()
                .filter(maritimeEntityReference -> maritimeEntityReference.isPublic() && maritimeEntityReference.getAttachment() != null)
                .map(MaritimeEntityReference::getAttachment)
                .map(AbstractAttachment::getId)
                .collect(toSet());

            fileIsPresent = attachmentIdsOfWarningAsPublicReference.contains(id) || attachmentIdsOfEntityReferenceAsPublicReference.contains(id);
        } else {
            final Georegulation georegulation = georegulationRepository.findById(notionId).orElseThrow(NotFoundException::new);
            notionStatusIsDraft = georegulation.getStatus() == GeoregulationStatus.DRAFT;
            fileIsPresent = georegulation.getAttachments().stream()
                .map(AbstractAttachment::getId)
                .collect(toSet()).contains(id);
        }

        if (notionStatusIsDraft) {
            throw new ForbiddenException("this attachment is in a notion which status is draft and cannot be downloaded");
        }

        if (!fileIsPresent) {
            throw new NotFoundException();
        }

        return getFile(id);
    }
}
