package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class FileUtils {

    private static final String FILE_IDENTIFIER_REGEX = "[^0-9a-zA-Z_-]";

    @Autowired
    private MimeTypeUtils mimeTypeUtils;

    public static String getRealMimeType(File file) {
        AutoDetectParser parser = new AutoDetectParser();
        Detector detector = parser.getDetector();
        try (InputStream is = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            TikaInputStream stream = TikaInputStream.get(is);
            MediaType mediaType = detector.detect(stream, metadata);
            return mediaType.toString();
        } catch (IOException e) {
            return MimeTypes.OCTET_STREAM;
        }
    }

    public boolean validateMimeType(File file) {
        final String detectedFromContent = getRealMimeType(file);
        final String detectedFromExtension = mimeTypeUtils.getMimeType(file);
        final boolean valid = StringUtils.equalsIgnoreCase(detectedFromContent, detectedFromExtension)
            || "application/x-rar-compressed; version=4".equalsIgnoreCase(detectedFromContent)
            && "application/x-rar-compressed".equalsIgnoreCase(detectedFromExtension)
            || "application/octet-stream".equalsIgnoreCase(detectedFromContent)
            && "application/zip".equalsIgnoreCase(detectedFromExtension);
        if (!valid) {
            log.warn("Detected from content: " + detectedFromContent + " vs detected from extension: " + detectedFromExtension);
        }
        return valid;
    }

    public static Set<Path> listFilesPerExtension(String dir, String... extensions) throws IOException {
        try (Stream<Path> stream = Files.list(Path.of(dir))) {
            return stream
                .filter(file -> !Files.isDirectory(file) && Arrays.stream(extensions).anyMatch(ext -> StringUtils.equals(ext,
                    FilenameUtils.getExtension(file.getFileName().toString()))))
                .collect(Collectors.toSet());
        }
    }

    public static void mkDirIfNotExists(Path dirPath) throws ServiceException {
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                throw new ServiceException("Can't create directory " + dirPath, e);
            }
        }
    }

    public static void cleanDir(Path dirPath) throws ServiceException {
        try {
            org.apache.commons.io.FileUtils.cleanDirectory(dirPath.toFile());
        } catch (IOException e) {
            throw new ServiceException("Can't clean directory " + dirPath, e);
        }
    }

    public static String generateUniqueFileIdentifier(Path path) {
        final String filename = org.springframework.util.StringUtils.cleanPath(path.getFileName().toString());
        return Instant.now().toEpochMilli() + "-" + filename.replaceAll(FILE_IDENTIFIER_REGEX, "");
    }
}
