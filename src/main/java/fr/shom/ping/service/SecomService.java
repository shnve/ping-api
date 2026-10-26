package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.S124Exception;
import fr.shom.ping.exception.SecomException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.GetResponseObjectDto;
import fr.shom.ping.model.predicate.SecomGetResponseObjectSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.SerieRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.utils.DigitalSignatureUtils;
import fr.shom.ping.utils.NavtexUtil;
import fr.shom.ping.utils.S100Util;
import fr.shom.ping.utils.S124v2Util;
import fr.shom.ping.utils.ZipUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.CapabilityResponseObject;
import org.grad.secom.core.models.DataResponseObject;
import org.grad.secom.core.models.DigitalSignatureValue;
import org.grad.secom.core.models.ImplementedInterfaces;
import org.grad.secom.core.models.PaginationObject;
import org.grad.secom.core.models.SECOM_ExchangeMetadataObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static fr.shom.ping.utils.NavtexUtil.zeroLeftPad;
import static fr.shom.ping.utils.S124Util.INTERNATIONAL_LANGUAGE;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.cleanPath;

@Slf4j
@RequiredArgsConstructor
@Service
public class SecomService {

    public static final String SECOM_HASH_ALGORITHM = "SHA-256";
    public static final String SECOM_SIGNING_ALGORITHM = "SHA256withDSA";

    public static final String S100_HASH_ALGORITHM = "SHA-384";
    public static final String S100_SIGNING_ALGORITHM = "SHA384withECDSA";

    private static final String KEY_STORE_TYPE = "JKS";

    private static final XssSanitizerService XSS_SANITIZER_SERVICE = new XssSanitizerService();

    private static final String DATASET_IDENTIFIER_FORMAT = "124%s%s%s%s%s";

    private static final String DATASET_FILE_NAME_FORMAT = "%s.gml";

    private static final String DEFAULT_S124_ID = "X";

    private static final String DEFAULT_LANG_IDENTIFIER_ES = "S";

    private static final String DEFAULT_LANG_IDENTIFIER_EN = "E";
    public static final String ZIP_SHA_256_EXT = ".sha256";
    public static final String DATASET_SHA_384_EXT = ".sha384";
    public static final String SIGN_EXT = ".SIGN";
    public static final String SIGN_HEX_EXT = ".SIGN.hex";
    public static final String CATALOG_XML_FILENAME = "CATALOG.XML";
    public static final String ZIP_EXT = ".ZIP";
    public static final String ZIP_BASE_64_EXT = ".ZIP.base64";
    public static final String PROTECTION_SCHEME = "SECOM";
    public static final DigitalSignatureAlgorithmEnum SECOM_DIGITAL_SIGNATURE_ALGORITHM = DigitalSignatureAlgorithmEnum.DSA;

    private final PingProperties pingProperties;

    private PrivateKey signingPrivateKey;

    private PublicKey signingPublicKey;

    private final S124v2Util s124v2Util;

    private final S100Util s100Util;

    private final SerieRepository serieRepository;

    private final WarningService warningService;

    private final WarningRepository warningRepository;

    private static final URL PRODUCT_SCHEMA_URL;
    private static final String PRODUCT_SCHEMA_URI = "https://registry.iho.int/productspec/list.do";
    public static final String SERVICE_VERSION = "1.0.0";
    public static final String PRODUCT_VERSION = "1.5.0";

    static {
        try {
            PRODUCT_SCHEMA_URL = new URI(PRODUCT_SCHEMA_URI).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new SecomException("Failed to init PRODUCT_SCHEMA_URL", e);
        }
    }

    public CapabilityResponseObject getCapability(String serviceInstanceId) throws NotFoundException {
        checkInstanceServiceExists(serviceInstanceId);

        // Populate the implemented SECOM interfaces
        ImplementedInterfaces implementedInterfaces = getImplementedInterfaces();

        // Start building the capability entry
        CapabilityObject capabilityObject = new CapabilityObject();
        capabilityObject.setDataProductType(SECOM_DataProductType.S124);
        capabilityObject.setContainerType(ContainerTypeEnum.S100_ExchangeSet);
        capabilityObject.setProductSchemaUrl(PRODUCT_SCHEMA_URL);

        capabilityObject.setImplementedInterfaces(implementedInterfaces);

        // Start building the capability response
        CapabilityResponseObject capabilityResponseObject = new CapabilityResponseObject();

        capabilityResponseObject.setCapability(singletonList(capabilityObject));
        capabilityObject.setServiceVersion(SERVICE_VERSION);

        // And return the Capability Response Object
        return capabilityResponseObject;
    }

    private static ImplementedInterfaces getImplementedInterfaces() {
        final ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
        implementedInterfaces.setUpload(false);
        implementedInterfaces.setUploadLink(false);
        implementedInterfaces.setGet(true);
        implementedInterfaces.setGetByLink(false);
        implementedInterfaces.setGetSummary(false);
        implementedInterfaces.setSubscription(false);
        implementedInterfaces.setAccess(false);
        implementedInterfaces.setEncryptionKey(false);
        return implementedInterfaces;
    }

    public void checkInstanceServiceExists(String serviceInstanceId) throws NotFoundException {
        // Check if the service instance exists
        boolean serviceInstanceIsAvailable = serieRepository.checkIfServiceInstanceIsAvailable(serviceInstanceId);
        if (!serviceInstanceIsAvailable) {
            throw new NotFoundException("Service instance " + serviceInstanceId + " does not exist");
        }
    }

    private static void deleteWorkingDirectory(Path workingDir) throws IOException {
        FileUtils.deleteDirectory(workingDir.toFile());
    }

    public void loadPrivateKey(String alias) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        final char[] password = trustStoreConfiguration.getPassword().toCharArray();
        signingPrivateKey = (PrivateKey) buildKeystore().getKey(alias, password);
    }

    public void loadPublicKey(String alias) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final Certificate certificate = buildKeystore().getCertificate(alias);
        signingPublicKey = certificate.getPublicKey();
    }

    public String getShomCertificateContent() throws ServiceException {
        return getCertificateContent(getShomAlias());
    }

    public String getCaCertificateContent(String caAlias) throws ServiceException {
        return getCertificateContent(caAlias);
    }

    private X509Certificate getCertificate(String alias) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return (X509Certificate) buildKeystore().getCertificate(alias);
    }

    public String getCertificateContent(String alias) throws ServiceException {
        try {
            final X509Certificate certificate = getCertificate(alias);
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new ServiceException("failed to get certificate content for alias " + alias, e);
        }
    }

    public String getSecomAlias() {
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        return trustStoreConfiguration.getSecom().getAliasSecom();
    }

    public String getShomAlias() {
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        return trustStoreConfiguration.getS100().getAliasShom();
    }

    public String getSecomCaAlias() {
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        return trustStoreConfiguration.getSecom().getAliasCa();
    }

    public String getS100CaAlias() {
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        return trustStoreConfiguration.getS100().getAliasCa();
    }

    private KeyStore buildKeystore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        final PingProperties.KeyStoreConfiguration trustStoreConfiguration = pingProperties.getKeystore();
        final char[] password = trustStoreConfiguration.getPassword().toCharArray();
        final String path = trustStoreConfiguration.getPath();
        final String safePath = Path.of(FilenameUtils.getFullPath(path), FilenameUtils.getName(path)).toString();
        keyStore.load(new ClassPathResource(safePath).getInputStream(), password);
        return keyStore;
    }

    public File createSignatureFilesWithHex(File sourceFile, File targetFolder, String alias, String signingAlgorithm, String hashAlgorithm,
        String controlFileExtension) {
        return createSignatureFiles(sourceFile, targetFolder, true, alias, signingAlgorithm, hashAlgorithm, controlFileExtension);
    }

    public File createSignatureFilesWithoutHex(File sourceFile, File targetFolder, String alias, String signingAlgorithm, String hashAlgorithm,
        String controlFileExtension) {
        return createSignatureFiles(sourceFile, targetFolder, false, alias, signingAlgorithm, hashAlgorithm, controlFileExtension);
    }

    public File createSignatureFiles(File sourceFile, File targetFolder, boolean generateHexFromSignature, String alias, String signingAlgorithm,
        String hashAlgorithm, String controlFileExtension) {
        // (1) load private and public keys
        try {
            loadPrivateKey(alias);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new SecomException("Failed to load private key", e);
        }

        try {
            loadPublicKey(alias);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new SecomException("Failed to load public key", e);
        }

        // (2) create file sum control and save it as .<controlFileExtension> file
        final String baseName = FilenameUtils.getBaseName(sourceFile.toPath().toString());
        final Path targetFolderPath = targetFolder.toPath();
        final String targetFolderFullPath = Path.of(FilenameUtils.getFullPath(targetFolderPath.toString()),
            FilenameUtils.getName(targetFolderPath.toString())).toString();
        final File sumControlFile = Paths.get(targetFolderFullPath, baseName + controlFileExtension).toFile();
        try {
            generateSumControlFile(sourceFile, sumControlFile, hashAlgorithm);
        } catch (IOException e) {
            throw new SecomException("Failed to generate sum control file from " + sourceFile, e);
        }

        // (3) sign sum control .controlFileExtension file and produce .SIGN file
        final File signatureFile = Paths.get(targetFolderFullPath, baseName + SIGN_EXT).toFile();
        try {
            generateSumControlSignatureFile(sumControlFile, signatureFile, signingPrivateKey, signingAlgorithm);
        } catch (IOException e) {
            throw new SecomException("Failed to sign sum control file " + sumControlFile, e);
        }

        // (4) verify produced signature
        final boolean signatureOk;
        try {
            signatureOk = verifySignature(sumControlFile, signatureFile, signingAlgorithm);
        } catch (IOException e) {
            throw new SecomException("Failed to verify signature " + signatureFile + " with " + sumControlFile, e);
        }
        if (!signatureOk) {
            throw new SecomException("Signature verification failed");
        }

        // (5) generate hexadecimal version of .SIGN file
        if (!generateHexFromSignature) {
            return signatureFile;
        }

        final File signatureFileToHex = Paths.get(targetFolderFullPath, baseName + SIGN_HEX_EXT).toFile();
        try {
            generateHexFile(signatureFile, signatureFileToHex);
        } catch (IOException e) {
            throw new SecomException("Failed to generate hex file from " + signatureFile, e);
        }
        return signatureFileToHex;
    }

    public void clean(Path directory) {
        try {
            deleteWorkingDirectory(directory);
        } catch (IOException e) {
            log.warn("Failed to delete working directory: {}", directory, e);
        }
    }

    private static byte[] createSumControl(File sourceFile, String hashAlgorithm) throws IOException {
        final byte[] messageBytes = Files.readAllBytes(sourceFile.toPath());
        return DigitalSignatureUtils.hashFile(messageBytes, hashAlgorithm);
    }

    private static void generateSumControlFile(File sourceFile, File targetFile, String hashAlgorithm) throws IOException {
        final byte[] fileSumControl = createSumControl(sourceFile, hashAlgorithm);
        FileUtils.writeByteArrayToFile(targetFile, fileSumControl);
    }

    public static void encodeBase64(File sourceFile, File targetFile) throws IOException {
        byte[] encodedFileBytes = Base64.getEncoder().encode(Files.readAllBytes(sourceFile.toPath()));
        FileUtils.writeByteArrayToFile(targetFile, encodedFileBytes);
    }

    private static void generateSumControlSignatureFile(File sourceFile, File targetFile, PrivateKey signingKey, String signingAlgorithm) throws IOException {
        final byte[] message = FileUtils.readFileToByteArray(sourceFile);
        final byte[] signature = DigitalSignatureUtils.sign(message, signingAlgorithm, signingKey);
        FileUtils.writeByteArrayToFile(targetFile, signature);
    }

    private static void generateHexFile(File sourceFile, File targetFile) throws IOException {
        final byte[] message = FileUtils.readFileToByteArray(sourceFile);
        final String hexContent = DigitalSignatureUtils.bytesToHex(message);
        FileUtils.writeStringToFile(targetFile, hexContent, Charset.defaultCharset());
    }

    private boolean verifySignature(File fileNotSigned, File fileSigned, String signingAlgorithm) throws IOException {
        final byte[] messageNotSigned = FileUtils.readFileToByteArray(fileNotSigned);
        final byte[] messageSigned = FileUtils.readFileToByteArray(fileSigned);
        return DigitalSignatureUtils.verify(messageNotSigned, signingAlgorithm, signingPublicKey, messageSigned);
    }

    private static void createS100DirectoriesIfNotExists(Path... s100DirPath) throws ServiceException {
        for (Path path : s100DirPath) {
            fr.shom.ping.utils.FileUtils.mkDirIfNotExists(path);
        }
    }

    private Path prepareRequiredS100Folders(Path s124RootDirPath, String issuingAgencyName, Integer warningNumberYear, String exchangeSetIdentifier)
        throws ServiceException {
        // s124/
        //    FR01OHI/      <---- IHO producer code
        //       2024/      <---- warning year number
        //          124FR01OHIS12412323E/  <---- zip directory root containing all zip exchange set files, one per exchange set per lang, CCCC = OHI Producer
        //          Code
        // All following folders are created if not exists but temporarily. Will be removed after the zip file is created
        //             S100_ROOT/
        //                CATALOG.XML       <---- catalog for EN lang
        //                CATALOG.SIGN      <---- signed with SHA384withECDSA
        //                S-124/
        //                   DATASET_FILES/
        //                      124FR01OHIS12412323E.gml
        //                      124FR01OHIS12412323E.sha384
        //                      124FR01OHIS12412323E.SIGN <---- signed with SHA384withECDSA
        //                      124FR01OHIS12412323E.SIGN.hex
        //          124FR01OHIS12412323F/
        //             S100_ROOT/
        //                CATALOG.XML       <---- catalog for FR lang
        //                CATALOG.SIGN
        //                S-124/
        //                   DATASET_FILES/
        //                      124FR01OHIS12412323F.gml
        //                      124FR01OHIS12412323F.sha384
        //                      124FR01OHIS12412323F.SIGN <---- signed with SHA384withECDSA
        //                      124FR01OHIS12412323F.SIGN.hex

        // (0) list required folders containing zip files
        final Path ihoProducerCodeDirPath =
            Path.of(cleanPath(Path.of(s124RootDirPath.toString(), issuingAgencyName).toString()));

        final Path warningNumberYearDirPath =
            Path.of(cleanPath(Path.of(ihoProducerCodeDirPath.toString(), warningNumberYear.toString()).toString()));

        final Path exchangeSetRootDirPath = Path.of(warningNumberYearDirPath.toString(), exchangeSetIdentifier);

        // (1) list required folders containing temporary folders/files
        final Path s100RootDirPath =
            Path.of(cleanPath(Path.of(exchangeSetRootDirPath.toString(), pingProperties.getS100RootFolderName()).toString()));

        final Path s124ExchangeSetDirPath =
            Path.of(cleanPath(Path.of(s100RootDirPath.toString(), pingProperties.getS124FolderName()).toString()));

        final Path datasetFilesDirPath =
            Path.of(cleanPath(Path.of(s124ExchangeSetDirPath.toString(), pingProperties.getDatasetFilesFolderName()).toString()));

        // (2) create all required folders if not exists
        createS100DirectoriesIfNotExists(s124RootDirPath, ihoProducerCodeDirPath, warningNumberYearDirPath, exchangeSetRootDirPath, s100RootDirPath,
            s124ExchangeSetDirPath, datasetFilesDirPath);


        return datasetFilesDirPath;
    }

    public void generatePublishedExchangeSetFile(String targetFolder, Warning warning) throws IOException, ServiceException, SecomException {
        // Prepare issuing agency folder : sanitize folder name and remove special characters like /, \, :, *, ?, ", <, >, |, etc.
        final String ihoProducerFolderName = getIhoProducerFolderName(warning);

        warning.getSerie().getMandatoryLanguages().forEach(lang -> {
            final String langIdentifier = StringUtils.equalsIgnoreCase(lang, INTERNATIONAL_LANGUAGE) ? DEFAULT_LANG_IDENTIFIER_EN : DEFAULT_LANG_IDENTIFIER_ES;
            final String datasetIdentifier = buildDatasetIdentifier(warning, ihoProducerFolderName, langIdentifier);
            final Path issuingAgencyDirPath;
            try {
                issuingAgencyDirPath = prepareRequiredS100Folders(Path.of(targetFolder), ihoProducerFolderName, warning.getYear(), datasetIdentifier);
            } catch (ServiceException e) {
                log.error("Failed to prepare required S-100 folders", e);
                throw new S124Exception("Failed to prepare required S-100 folders: " + e.getMessage());
            }

            final Path datasetFile = Path.of(issuingAgencyDirPath.toString(), String.format(DATASET_FILE_NAME_FORMAT, datasetIdentifier));

            // (1) build warning dataset file
            try {
                s124v2Util.generateGmlDatasetFile(datasetFile.toFile(), warning, lang);
            } catch (ServiceException e) {
                log.error("Failed to generate GML dataset file {}", datasetFile, e);
                throw new S124Exception("Failed to generate GML dataset file " + e.getMessage());
            }

            // (2) sign dataset file
            final File targetFolderForDatasetSignatureGeneration = Path.of(FilenameUtils.getFullPath(datasetFile.toString())).toFile();
            final File datasetSignatureFile =
                createSignatureFilesWithHex(
                    datasetFile.toFile(), targetFolderForDatasetSignatureGeneration, getShomAlias(),
                    S100_SIGNING_ALGORITHM, S100_HASH_ALGORITHM, DATASET_SHA_384_EXT
                );
            final String signature = readFile(datasetSignatureFile);

            // (3) build catalog exchange set (generate content, including certificate, save it as file)
            final Path ihoProducerCodeDirPath = Path.of(cleanPath(Path.of(targetFolder, ihoProducerFolderName).toString()));
            final Path warningNumberYearDirPath = Path.of(cleanPath(Path.of(ihoProducerCodeDirPath.toString(), warning.getYear().toString()).toString()));
            final Path exchangeSetRootDirPath = Path.of(warningNumberYearDirPath.toString(), datasetIdentifier);
            final Path s100RootDirPath = Path.of(cleanPath(Path.of(exchangeSetRootDirPath.toString(), pingProperties.getS100RootFolderName()).toString()));
            final Path catalogFile = Path.of(s100RootDirPath.toString(), CATALOG_XML_FILENAME);

            final String shomCertificate;
            try {
                shomCertificate = getShomCertificateContent();
            } catch (SecomException | ServiceException e) {
                log.error("Failed to get shom certificate content", e);
                throw new SecomException("Failed to get shom certificate content", e);
            }

            final String caCertificate;
            try {
                caCertificate = getCaCertificateContent(getS100CaAlias());
            } catch (SecomException | ServiceException e) {
                log.error("Failed to get CA certificate content", e);
                throw new SecomException("Failed to get CA certificate content", e);
            }

            final String datasetFileRelativePath =
                "file:/" + datasetFile.toString().substring(datasetFile.toString().indexOf(pingProperties.getS124FolderName()))
                    .replaceAll("\\\\", "/");

            try {
                s100Util.generateCatalogExchangeSetFile(catalogFile.toFile(), getShomAlias(), shomCertificate, getS100CaAlias(), caCertificate,
                    LocalDate.now(), singletonList(datasetFileRelativePath), signature, pingProperties.getShomOrganizationInfo());
            } catch (ServiceException e) {
                log.error("Failed to generate exchange set xml catalog {}", catalogFile, e);
                throw new SecomException("Failed to generate exchange set xml catalog " + catalogFile, e);
            }

            // (4) sign catalog file
            final File targetFolderForCatalogSignatureGeneration = Path.of(FilenameUtils.getFullPath(catalogFile.toString())).toFile();
            createSignatureFilesWithoutHex(
                catalogFile.toFile(), targetFolderForCatalogSignatureGeneration, getShomAlias(),
                S100_SIGNING_ALGORITHM, S100_HASH_ALGORITHM, DATASET_SHA_384_EXT
            );

            // (5) remove unnecessary files we don't want to be in zip file
            // CATALOG.sha384
            // S-124/DATASET_FILES/
            //                      124IHOTESTZ100624E.SIGN
            //                      124IHOTESTZ100624E.SIGN.hex
            //                      124IHOTESTZ100624E.sha384
            cleanCatalogFiles(catalogFile, targetFolderForCatalogSignatureGeneration);
            cleanDatasetFiles(datasetFile);

            // (6) create ZIP file (for example: of 124FR01OHIS12412323E/ and 124FR01OHIS12412323F/)
            final Path zipFile = Path.of(exchangeSetRootDirPath.toString(), datasetIdentifier + ZIP_EXT);
            try {
                ZipUtil.zipDirectory(s100RootDirPath.toString(), zipFile.toString(), pingProperties.getS100RootFolderName());
            } catch (ServiceException e) {
                log.error("Failed to zip directory {}", s100RootDirPath, e);
                throw new SecomException("Failed to zip directory " + s100RootDirPath, e);
            }

            // (7) encode zip file to base64
            final File zipHashFile = Path.of(exchangeSetRootDirPath.toString(), datasetIdentifier + ZIP_BASE_64_EXT).toFile();
            try {
                encodeBase64(zipFile.toFile(), zipHashFile);
            } catch (IOException e) {
                log.error("Failed to encode zip file to base64 {}", zipFile, e);
                throw new SecomException("Failed to encode zip file to base64 " + zipFile, e);
            }

            // (8) sign zip file
            final File targetFolderForZipSignatureGeneration = Path.of(FilenameUtils.getFullPath(zipFile.toString())).toFile();
            createSignatureFilesWithHex(
                zipFile.toFile(), targetFolderForZipSignatureGeneration, getSecomAlias(),
                SECOM_SIGNING_ALGORITHM, SECOM_HASH_ALGORITHM, ZIP_SHA_256_EXT
            );

            // (9) delete S100_ROOT/ directory
            clean(s100RootDirPath);
        });
    }

    private static String buildDatasetIdentifier(Warning warning, String ihoProducerFolderName, String langIdentifier) {
        return String.format(
            DATASET_IDENTIFIER_FORMAT, ihoProducerFolderName, ofNullable(warning.getSerieS124Identifier()).orElse(DEFAULT_S124_ID),
            zeroLeftPad(warning.getWarningNumber(), 4), NavtexUtil.getYearSuffix(warning.getYear()), langIdentifier
        );
    }

    private static void cleanCatalogFiles(Path catalogFile, File targetFolderForCatalogSignatureGeneration) {
        final String baseName = FilenameUtils.getBaseName(catalogFile.toString());
        final Path targetFolderPath = targetFolderForCatalogSignatureGeneration.toPath();
        final String targetFolderFullPath = Path.of(FilenameUtils.getFullPath(targetFolderPath.toString()),
            FilenameUtils.getName(targetFolderPath.toString())).toString();
        final File catalogSumControlFile = Paths.get(targetFolderFullPath, baseName + DATASET_SHA_384_EXT).toFile();
        try {
            FileUtils.delete(catalogSumControlFile);
        } catch (IOException e) {
            log.error("Failed to delete file {}", catalogFile, e);
            throw new SecomException("Failed to delete file " + catalogFile, e);
        }
    }

    private static void cleanDatasetFiles(Path datasetFile) {
        final String targetFolderFullPath = FilenameUtils.getFullPath(datasetFile.toString());
        final String baseName = FilenameUtils.getBaseName(datasetFile.toString());
        final File datasetSumControlFile = Paths.get(targetFolderFullPath, baseName + DATASET_SHA_384_EXT).toFile();
        delete(datasetSumControlFile);

        final File datasetSignFile = Paths.get(targetFolderFullPath, baseName + SIGN_EXT).toFile();
        delete(datasetSignFile);

        final File datasetSignHexFile = Paths.get(targetFolderFullPath, baseName + SIGN_HEX_EXT).toFile();
        delete(datasetSignHexFile);
    }

    private static void delete(File file) {
        try {
            FileUtils.delete(file);
        } catch (IOException e) {
            log.error("Failed to delete file {}", file, e);
            throw new SecomException("Failed to delete file " + file, e);
        }
    }

    public GetResponseObjectDto getResponseObject(String serviceInstanceId, SecomGetResponseObjectSearchCriteria searchCriteria)
        throws NotFoundException, BadRequestException, ForbiddenException {
        checkInstanceServiceExists(serviceInstanceId);

        final int secomPageRequest = ofNullable(searchCriteria.getPage()).orElse(1);

        // if page is 0 when data reference requested, return a data response object with no data
        final List<Warning> warnings = new ArrayList<>();

        if (secomPageRequest >= 1) {
            // if the pageSize parameter is not present, the server must return the maximum number of elements per page it supports
            final int secomPageSizeRequest = ofNullable(searchCriteria.getPageSize())
                .orElse(searchCriteria.getDataReference() != null ? 1 : pingProperties.getSecomPageSizeMax());

            warnings.addAll(getWarnings(serviceInstanceId, searchCriteria.getDataReference(),
                secomPageRequest - 1, secomPageSizeRequest));
        }

        final List<DataResponseObject> allObjectsReturned = getDataObjects(warnings, serviceInstanceId);

        // filter list to remove AN with errors (i.e. zip or signature not found)
        final Predicate<DataResponseObject> dataResponseObjectPredicateWithError = o -> new String(o.getData()).startsWith("ERROR");
        final List<DataResponseObject> objectsReturned = allObjectsReturned.stream()
            .filter(dataResponseObjectPredicateWithError.negate())
            .toList();

        final List<DataResponseObject> errorsFound = allObjectsReturned.stream()
            .filter(dataResponseObjectPredicateWithError)
            .toList();

        // Create a pagination object
        final PaginationObject paginationObject = new PaginationObject();
        // must always be set to maxPageSize
        paginationObject.setMaxItemsPerPage(pingProperties.getSecomPageSizeMax());
        // must always be set to the total number of warning in force in the instance service
        final int totalItems = searchCriteria.getDataReference() != null ?
            1 : warningRepository.countByStatusAndSerie(PUBLISHED, getSerieFromServiceInstanceId(serviceInstanceId));
        paginationObject.setTotalItems(totalItems);

        final GetResponseObjectDto responseObject = new GetResponseObjectDto();
        responseObject.setDataResponseObject(objectsReturned);
        responseObject.setPagination(paginationObject);

        // send the list of AN
        responseObject.setResponseText(
            errorsFound.isEmpty() ? null : errorsFound.stream().map(DataResponseObject::getData).map(String::new).collect(Collectors.joining(";"))
        );

        return responseObject;
    }

    private List<DataResponseObject> getDataObjects(List<Warning> warnings, String serviceInstanceId) {
        final List<DataResponseObject> objectsReturned = new ArrayList<>();
        final String lang = getLang(serviceInstanceId);
        warnings.forEach(warning -> {

            final String errorMsg = "ERROR for " + warning.formattedWarningFullNumber() + " with id " + warning.getId();

            // Create a digital signature value
            final DigitalSignatureValue digitalSignatureValue = new DigitalSignatureValue();
            digitalSignatureValue.setPublicRootCertificateThumbprint(getRootCertificateThumbprint(getSecomCaAlias()));     // ping_root thumbprint
            digitalSignatureValue.setPublicCertificate(getPingSecomCertificatePublicKey());               // ping_secom certificate public key
            try {
                digitalSignatureValue.setDigitalSignature(getEchangeSetSignHexData(warning, lang));       // ping_secom control signature sum hex
            } catch (SecomException e) {
                digitalSignatureValue.setDigitalSignature(errorMsg);
                log.error("Failed to get digital has signature for warning {} with id {}", warning.formattedWarningFullNumber(), warning.getId(), e);
            }

            // Create SECOM exchange metadata
            final SECOM_ExchangeMetadataObject exchangeMetadata = new SECOM_ExchangeMetadataObject();
            exchangeMetadata.setDataProtection(Boolean.FALSE);
            exchangeMetadata.setProtectionScheme(PROTECTION_SCHEME);
            exchangeMetadata.setDigitalSignatureReference(SECOM_DIGITAL_SIGNATURE_ALGORITHM);
            exchangeMetadata.setDigitalSignatureValue(digitalSignatureValue);
            exchangeMetadata.setCompressionFlag(Boolean.TRUE);

            // Create a digital signature value
            final DataResponseObject dataResponseObject = new DataResponseObject();
            try {
                dataResponseObject.setData(getEchangeSetBase64Data(warning, lang).getBytes(StandardCharsets.UTF_8));
            } catch (SecomException e) {
                dataResponseObject.setData(errorMsg.getBytes(StandardCharsets.UTF_8));
                log.error("Failed to get EchangeSet Base64 Data for warning {} with id {}", warning.formattedWarningFullNumber(), warning.getId(), e);
            }
            dataResponseObject.setExchangeMetadata(exchangeMetadata);
            dataResponseObject.setAckRequest(AckRequestEnum.NO_ACK_REQUESTED);
            objectsReturned.add(dataResponseObject);
        });
        return objectsReturned;
    }

    private String getEchangeSetBase64Data(Warning warning, String lang) {
        final File base64zip = getEchangeSetBase64ZipFullpath(warning, lang);
        return readFile(base64zip);
    }

    private String getEchangeSetSignHexData(Warning warning, String lang) {
        final File base64zip = getEchangeSetSignHexFullpath(warning, lang);
        return readFile(base64zip);
    }

    private String readFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new SecomException("Failed to read file " + file, e);
        }
    }

    private File getEchangeSetBase64ZipFullpath(Warning warning, String lang) {
        return Path.of(getExchangeSetFullpath(warning, lang) + ZIP_BASE_64_EXT).toFile();
    }

    private File getEchangeSetSignHexFullpath(Warning warning, String lang) {
        return Path.of(getExchangeSetFullpath(warning, lang) + SIGN_HEX_EXT).toFile();
    }

    private Path getExchangeSetFullpath(Warning warning, String lang) {
        final String langIdentifier = StringUtils.equalsIgnoreCase(lang, INTERNATIONAL_LANGUAGE) ? DEFAULT_LANG_IDENTIFIER_EN : DEFAULT_LANG_IDENTIFIER_ES;
        final String ihoProducerFolderName = getIhoProducerFolderName(warning);
        final String datasetIdentifier = buildDatasetIdentifier(warning, ihoProducerFolderName, langIdentifier);

        final Path ihoProducerCodeDirPath =
            Path.of(cleanPath(Path.of(pingProperties.getGeneratedS124Folder(), ihoProducerFolderName).toString()));

        final Path warningNumberYearDirPath =
            Path.of(cleanPath(Path.of(ihoProducerCodeDirPath.toString(), warning.getYear().toString()).toString()));

        final Path exchangeSetRootDirPath = Path.of(warningNumberYearDirPath.toString(), datasetIdentifier);
        return Path.of(exchangeSetRootDirPath.toString(), datasetIdentifier);
    }

    private String getIhoProducerFolderName(Warning warning) {
        return XSS_SANITIZER_SERVICE.sanitize(warning.getMaritimeServiceihoProducerCode())
            .replaceAll(pingProperties.getS124sanitizeRegex(), "");
    }

    private List<Warning> getWarnings(String serviceInstanceId, UUID dataReference, int page, int size)
        throws NotFoundException, BadRequestException, ForbiddenException {
        final List<Warning> warnings = new ArrayList<>();
        // get all warnings from serie and lang if no dataReference, else get only one warning of serie/lang with this dataReference id
        if (dataReference != null) {
            warnings.add(getWarning(serviceInstanceId, dataReference));
        } else {
            warnings.addAll(getPublishedWarningsInSerie(getSerieFromServiceInstanceId(serviceInstanceId), page, size));
        }
        return warnings;
    }

    public Warning getWarning(String serviceInstanceId, UUID dataReference) throws NotFoundException, BadRequestException, ForbiddenException {
        // look for warning existence, if published, if in the serie with the lang provided
        final Warning found;

        try {
            found = warningService.getWarningById(dataReference);
        } catch (NotFoundException e) {
            throw new NotFoundException("Warning with id " + dataReference + " not found");
        }

        final Serie serieFound = getSerieFromServiceInstanceId(serviceInstanceId);

        if (found.getSerie().getId() != serieFound.getId()) {
            throw new BadRequestException("serie " + serieFound.getName() + " found from serviceInstanceId " + serviceInstanceId + " does not match warning " +
                "serie " + found.getSerie().getName());
        }

        if (found.getStatus() != PUBLISHED) {
            throw new NotFoundException("Status of warning with id " + dataReference + " is not published");
        }


        return found;
    }

    private static String getLang(String serviceInstanceId) {
        return serviceInstanceId.substring(serviceInstanceId.lastIndexOf("_") + 1);
    }

    public Serie getSerieFromServiceInstanceId(String serviceInstanceId) throws NotFoundException, BadRequestException, ForbiddenException {
        if (!serviceInstanceId.endsWith("_fr") && !serviceInstanceId.endsWith("_en")) {
            throw new BadRequestException("serviceInstanceId must end with _fr or _en");
        }

        final String serieNamePart = serviceInstanceId.substring(0, serviceInstanceId.lastIndexOf("_"));
        final String languagePart = getLang(serviceInstanceId);
        final String serieNameFound = serieNamePart.replace("_", " ");

        final Optional<Serie> expectedFromServiceInstanceId = serieRepository.findOneByNameIgnoreCase(serieNameFound);
        if (expectedFromServiceInstanceId.isEmpty()) {
            throw new NotFoundException(serieNameFound + " is unknown");
        }

        final Serie serieFound = expectedFromServiceInstanceId.get();
        if (!serieFound.getMandatoryLanguages().contains(languagePart)) {
            throw new BadRequestException("language " + languagePart + " is not mandatory for serie " + serieFound.getName());
        }

        if (StringUtils.isBlank(serieFound.getS124Identifier())) {
            throw new ForbiddenException("Serie " + serieFound.getName() + " has no S124 identifier and is not allowed to produce S124 data.");
        }

        return serieFound;
    }

    private List<Warning> getPublishedWarningsInSerie(Serie serie, int page, int size) {
        return warningService.getPublishedWarnings(serie, page, size).getContent();
    }

    private String getRootCertificateThumbprint(String caAlias) {
        try {
            return DigitalSignatureUtils.getThumbprint(getCertificate(caAlias));
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new SecomException("Failed to get root certificate thumbprint with alias " + caAlias, e);
        }
    }

    private String getPingSecomCertificatePublicKey() {
        try {
            return DigitalSignatureUtils.getPublicKey(getCertificate(getSecomAlias()));
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new SecomException("Failed to get certificate public key with alias " + getSecomAlias(), e);
        }
    }
}
