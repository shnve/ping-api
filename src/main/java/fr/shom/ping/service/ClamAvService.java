package fr.shom.ping.service;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.properties.ClamAvProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.ScanFailureException;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClamAvService {

    private final ClamavClient clamavClient;

    private final ClamAvProperties properties;

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public boolean hasVirus(String file) throws ServiceException {
        try {
            clamavClient.ping();
        } catch (Exception clamavException) {
            log.error("Unreachable ClamAv server instance: " + properties.getHost() + ":" + properties.getPort(), clamavException);
            return false;
        }
        // according to file size threshold, launch sync analysis
        //    C:/tmp -> /mnt/c/tmp
        final String fileWithFixedPath = file.replace(properties.getClamavReplacedPath(), properties.getClamavReplacementPath());
        final File analyzedFile = new File(StringUtils.cleanPath(fileWithFixedPath));
        if (analyzedFile.length() > 1024L * 1024L * properties.getSyncAnalysisThreshold()) {
            log.info("File size exceeds threshold, analysis will be executed later: " + fileWithFixedPath);
            return false;
        }
        ScanResult scanResult;
        try (FileInputStream analyzedFileInputStream = new FileInputStream(analyzedFile)) {
            scanResult = clamavClient.scan(analyzedFileInputStream);
        } catch (ScanFailureException | IOException scanFailureException) {
            log.error("Failed to analyze: " + analyzedFile);
            log.error(scanFailureException.getMessage(), scanFailureException);
            throw new ServiceException("clamav.detection.failure");
        }
        if (scanResult instanceof ScanResult.OK) {
            return false;
        } else if (scanResult instanceof ScanResult.VirusFound found) {
            final Map<String, Collection<String>> viruses = found.getFoundViruses();
            final String result = viruses.entrySet().stream().map(item -> "Virus(es) found in file: " + item.getKey() + "\n"
                + String.join("\n", item.getValue())).collect(Collectors.joining("\n"));
            log.warn(result);
        }
        return true;
    }
}
