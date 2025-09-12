package fr.shom.ping.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.S100DatasetDiscoveryMetadata;
import fr.shom.ping.model.dto.S100ExchangeCatalog;
import fr.shom.ping.model.dto.S100SECertificate;
import fr.shom.ping.model.dto.S100SESchemeAdministrator;
import fr.shom.ping.model.dto.XcCertificates;
import fr.shom.ping.model.dto.XcDatasetDiscoveryMetadata;
import fr.shom.ping.model.dto.XcDigitalSignatureValue;
import fr.shom.ping.properties.PingProperties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlException;
import org.springframework.stereotype.Component;

import static fr.shom.ping.utils.XmlUtil.prettyPrintByTransformer;
import static java.util.Collections.singletonList;

@Component
public final class S100Util {

    private static final String SCHEME_ADMINISTRATOR = "root";
    private static final XmlMapper XML_MAPPER;
    private static final String OWNER = "owner";
    public static final String ID_DIGITAL_SIGNATURE = "sig1";
    public static final String CIT_ONLINE_FUNCTION_CODELIST = "http://standards.iso.org/iso/19115/resources/Codelists/cat/codelists.xml#CI_OnLineFunctionCode";

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private S100Util() {
    }

    public void generateCatalogExchangeSetFile(File targetFile, String shomAlias, String shomCertificate, String caAlias, String caCertificate,
        LocalDate issueDate, List<String> datasetFilenameList, String datasetSignature, PingProperties.ShomOrganizationInfo shomOrganizationInfo) throws ServiceException {
        final S100ExchangeCatalog s100ExchangeCatalog = buildExchangeCatalog(shomAlias, shomCertificate, caAlias, caCertificate, issueDate, datasetSignature,
            datasetFilenameList, shomOrganizationInfo);
        writeCatalogFile(s100ExchangeCatalog, targetFile.getPath());
    }

    public void writeCatalogFile(S100ExchangeCatalog catalog, String filePath) throws ServiceException {
        try (FileOutputStream fos = new FileOutputStream(Path.of(FilenameUtils.getFullPath(filePath), FilenameUtils.getName(filePath)).toString())) {
            fos.write(prettyPrintByTransformer(XML_MAPPER.writeValueAsString(catalog), 4, false).getBytes());
        } catch (IOException | XmlException exception) {
            throw new ServiceException("Failed to create catalog file " + filePath, exception);
        }
    }

    public S100ExchangeCatalog buildExchangeCatalog(String shomAlias, String shomCertificate, String caAlias, String caCertificate, LocalDate issueDate,
        String datasetSignature, List<String> datasetFilenameList, PingProperties.ShomOrganizationInfo shomOrganizationInfo) {
        return S100ExchangeCatalog.builder()
            .certificates(
                XcCertificates.builder()
                    .schemeAdministrator(
                        S100SESchemeAdministrator.builder()
                            .id(SCHEME_ADMINISTRATOR)
                            .build()
                    )
                    .certificates(List.of(
                        S100SECertificate.builder()
                            .id(shomAlias)
                            .issuer(SCHEME_ADMINISTRATOR)
                            .value(shomCertificate)
                            .build(),
                        S100SECertificate.builder()
                            .id(caAlias)
                            .issuer(SCHEME_ADMINISTRATOR)
                            .value(caCertificate)
                            .build()
                    ))
                    .build()
            )
            .datasetDiscoveryMetadata(
                XcDatasetDiscoveryMetadata.builder()
                    .datasetDiscoveryMetadata(buildDatasetDiscoveryMetadataList(datasetFilenameList, issueDate, shomOrganizationInfo, shomAlias,
                        datasetSignature))
                    .build()
            )
            .build();
    }

    public static List<S100DatasetDiscoveryMetadata> buildDatasetDiscoveryMetadataList(List<String> datasetFilenameList, LocalDate issueDate,
        PingProperties.ShomOrganizationInfo shomOrganizationInfo, String shomAlias, String datasetSignature) {
        final List<S100DatasetDiscoveryMetadata> result = new ArrayList<>();
        datasetFilenameList.forEach(datasetFilename ->
            result.add(
                S100DatasetDiscoveryMetadata.builder()
                    .issueDate(issueDate)
                    .filename(datasetFilename)
                    .digitalSignatureValue(singletonList(
                        XcDigitalSignatureValue.builder()
                            .s100SEDigitalSignature(
                                XcDigitalSignatureValue.S100SEDigitalSignature.builder()
                                    .certificateRef(shomAlias)
                                    .id(ID_DIGITAL_SIGNATURE)
                                    .value(Base64.getEncoder().encodeToString(datasetSignature.getBytes(StandardCharsets.UTF_8)))
                                    .build()
                            )
                            .build()
                    ))
                    .productSpecification(
                        S100DatasetDiscoveryMetadata.XcProductSpecification.builder().build()
                    )
                    .producingAgency(
                        S100DatasetDiscoveryMetadata.XcProducingAgency.builder()
                            .citCIResponsibility(
                                S100DatasetDiscoveryMetadata.CitCIResponsibility.builder()
                                    .citRole(
                                        S100DatasetDiscoveryMetadata.CitRole.builder()
                                            .roleCodes(singletonList(
                                                S100DatasetDiscoveryMetadata.CitCIRoleCode.builder()
                                                    .value(OWNER)
                                                    .build()
                                            ))
                                            .build()
                                    )
                                    .citPartyList(singletonList(
                                            S100DatasetDiscoveryMetadata.CitParty.builder()
                                                .citOrganisation(
                                                    S100DatasetDiscoveryMetadata.CitOrganisation.builder()
                                                        .gcoCharacterString(
                                                            S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                                                                .gcoCharacterString(shomOrganizationInfo.getName())
                                                                .build()
                                                        )
                                                        .citContactInfo(buildContactInfo(shomOrganizationInfo)).build()
                                                ).build()
                                        )
                                    ).build()
                            ).build()
                    ).build()
            )
        );

        return result;
    }

    public static S100DatasetDiscoveryMetadata.CitContactInfo buildContactInfo(PingProperties.ShomOrganizationInfo shomOrganizationInfo) {
        return S100DatasetDiscoveryMetadata.CitContactInfo.builder()
            .citContact(
                S100DatasetDiscoveryMetadata.CitContactInfo.CitContact.builder()
                    .citAddressInfo(buildContactAdressinfo(shomOrganizationInfo))
                    .citOnlineResourceInfo(buildContactOnlineResourceInfo(shomOrganizationInfo)).build()
            ).build();
    }

    private static S100DatasetDiscoveryMetadata.CitAddressInfo buildContactAdressinfo(PingProperties.ShomOrganizationInfo shomOrganizationInfo) {
        return S100DatasetDiscoveryMetadata.CitAddressInfo.builder()
            .citAddress(
                S100DatasetDiscoveryMetadata.CitAddressInfo.CitAddress.builder()
                    .citAdministrativeArea(
                        S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                            .gcoCharacterString(shomOrganizationInfo.getAddress())
                            .build())
                    .citCountry(
                        S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                            .gcoCharacterString(shomOrganizationInfo.getCountry())
                            .build())
                    .citElectronicMailAddress(
                        S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                            .gcoCharacterString(shomOrganizationInfo.getElectronicMailAddress())
                            .build())
                    .build()
            ).build();
    }

    private static S100DatasetDiscoveryMetadata.CitOnlineResourceInfo buildContactOnlineResourceInfo(PingProperties.ShomOrganizationInfo shomOrganizationInfo) {
        return S100DatasetDiscoveryMetadata.CitOnlineResourceInfo.builder()
            .citOnlineResource(
                S100DatasetDiscoveryMetadata.CitOnlineResourceInfo.CitOnlineResource.builder()
                    .citLinkage(
                        S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                            .gcoCharacterString(shomOrganizationInfo.getLinkage())
                            .build()
                    )
                    .citName(
                        S100DatasetDiscoveryMetadata.GcoCharacterString.builder()
                            .gcoCharacterString(shomOrganizationInfo.getResourceName())
                            .build()
                    )
                    .citFunction(
                        S100DatasetDiscoveryMetadata.CitOnlineResourceInfo.CitOnlineResource.CitOnLineFunctionCodeInfo.builder()
                            .citOnLineFunctionCode(
                                S100DatasetDiscoveryMetadata.CitCIRoleCode.builder()
                                    .codeList(CIT_ONLINE_FUNCTION_CODELIST)
                                    .codeListValue("information")
                                    .value(shomOrganizationInfo.getFunction())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ).build();
    }

    public static String toString(S100ExchangeCatalog catalog) throws JsonProcessingException {
        return XML_MAPPER.writeValueAsString(catalog);
    }

    public static String getLanguageCode(String lang) {
        return Locale.of(lang).getISO3Language();
    }
}
