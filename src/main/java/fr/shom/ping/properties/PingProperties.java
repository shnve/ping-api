package fr.shom.ping.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PingProperties {
    private String uploadFolder;
    private String generatedShapefileFolder;
    private String generatedGpxFolder;
    private String generatedNavtexFolder;
    private String generatedS124Folder;
    private String s124sanitizeRegex;
    private String s100RootFolderName;
    private String s124FolderName;
    private String datasetFilesFolderName;
    private Long georegulationMaxGlobalPeriodDurationInDays;
    private Integer maxUploadFileSizeInMegabytes;
    private List<String> uploadExtensions;
    private Integer maxUploadFileSizeInGigabytesForSurvey;
    private List<String> uploadExtensionsForSurveyMisc;
    private List<String> uploadExtensionsForSurveyBathy;
    private String tempFolderForArchiveFiles;
    private Integer surveyFileAnalysisLineCompliantHeaderReadOffset;
    private Integer maxPolygonVerticesForIsBathy;
    private KeyStoreConfiguration keystore;
    private Integer secomPageSizeMax;

    @Builder.Default
    private boolean bypassExternalAnalysisFingerprint = false;

    @Builder.Default
    private boolean bypassExternalSearchFingerprint = false;

    @Builder.Default
    private List<String> authorizedCertificateFingerprints = new ArrayList<>();

    private String cartonautSftp;

    private String surveysWorkingDir;

    private String generatedSurveyZipArchiveFolder;

    private String cartonautSftpRootRelativePath;

    @Builder.Default
    private boolean enableBathyDangersIntegration = false;

    private ShomOrganizationInfo shomOrganizationInfo;

    @Getter
    @Setter
    @Builder
    public static class KeyStoreConfiguration {
        private String path;
        private String password;
        private S100CertificateConfiguration s100;
        private SECOMCertificateConfiguration secom;
    }

    @Getter
    @Setter
    @Builder
    public static class S100CertificateConfiguration {
        private String aliasCa;
        private String aliasShom;
    }

    @Getter
    @Setter
    @Builder
    public static class SECOMCertificateConfiguration {
        private String aliasCa;
        private String aliasSecom;
    }

    @Getter
    @Builder
    @Setter
    public static class ShomOrganizationInfo {
        private String name;
        private String address;
        private String country;
        private String electronicMailAddress;
        private String function;
        private String linkage;
        private String resourceName;
    }
}
