package fr.shom.ping.utils;

import java.io.File;
import java.io.IOException;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public final class MimeTypeUtils {

    private MimetypesFileTypeMap mimetypesFileTypeMap;

    @Value("classpath:mime/mime.types")
    private Resource resourceFile;

    @PostConstruct
    public void init() throws IOException {
        log.info("init MimeTypeUtils");
        this.mimetypesFileTypeMap = new MimetypesFileTypeMap(resourceFile.getInputStream());
    }

    public String getMimeType(File file) {
        return mimetypesFileTypeMap.getContentType(file);
    }

    public String getMimeType(String filename) {
        return mimetypesFileTypeMap.getContentType(filename);
    }
}
