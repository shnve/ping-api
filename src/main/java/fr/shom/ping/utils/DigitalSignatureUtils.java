package fr.shom.ping.utils;

import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestAlgorithmIdentifierFinder;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

// https://www.baeldung.com/java-digital-signature
public class DigitalSignatureUtils {

    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String X509_CERTIFICATE_FINGERPRINT_HASH_ALGORITHM = "SHA-1";

    private DigitalSignatureUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static PrivateKey getPrivateKey(String file, char[] password, String storeType, String alias) throws KeyStoreException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException {
        final KeyStore keyStore = KeyStore.getInstance(storeType);
        final String safePath = Path.of(FilenameUtils.getFullPath(file), FilenameUtils.getName(file)).toString();
        keyStore.load(new FileInputStream(safePath), password);
        return (PrivateKey) keyStore.getKey(alias, password);
    }

    public static PublicKey getPublicKey(String file, char[] password, String storeType, String alias) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore keyStore = KeyStore.getInstance(storeType);
        final String safePath = Path.of(FilenameUtils.getFullPath(file), FilenameUtils.getName(file)).toString();
        keyStore.load(new FileInputStream(safePath), password);
        final Certificate certificate = keyStore.getCertificate(alias);
        return certificate.getPublicKey();
    }

    public static byte[] sign(byte[] message, String signingAlgorithm, PrivateKey signingKey) throws SecurityException {
        try {
            final Signature signature = Signature.getInstance(signingAlgorithm);
            signature.initSign(signingKey);
            signature.update(message);
            return signature.sign();
        } catch (GeneralSecurityException exp) {
            throw new SecurityException("Error during signature generation", exp);
        }
    }

    public static boolean verify(byte[] messageBytes, String signingAlgorithm, PublicKey publicKey, byte[] signedData) {
        try {
            final Signature signature = Signature.getInstance(signingAlgorithm);
            signature.initVerify(publicKey);
            signature.update(messageBytes);
            return signature.verify(signedData);
        } catch (GeneralSecurityException exp) {
            throw new SecurityException("Error during verifying", exp);
        }
    }

    public static byte[] signWithMessageDigestAndCipher(byte[] messageBytes, String hashingAlgorithm, PrivateKey privateKey) {
        try {
            final MessageDigest md = MessageDigest.getInstance(hashingAlgorithm);
            byte[] messageHash = md.digest(messageBytes);
            final DigestAlgorithmIdentifierFinder hashAlgorithmFinder = new DefaultDigestAlgorithmIdentifierFinder();
            final AlgorithmIdentifier hashingAlgorithmIdentifier = hashAlgorithmFinder.find(hashingAlgorithm);
            final DigestInfo digestInfo = new DigestInfo(hashingAlgorithmIdentifier, messageHash);
            final byte[] hashToEncrypt = digestInfo.getEncoded();

            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(hashToEncrypt);
        } catch (GeneralSecurityException | IOException exp) {
            throw new SecurityException("Error during signature generation", exp);
        }
    }

    public static boolean verifyWithMessageDigestAndCipher(byte[] messageBytes, String hashingAlgorithm, PublicKey publicKey, byte[] encryptedMessageHash) {
        try {
            final MessageDigest md = MessageDigest.getInstance(hashingAlgorithm);
            final byte[] newMessageHash = md.digest(messageBytes);
            final DigestAlgorithmIdentifierFinder hashAlgorithmFinder = new DefaultDigestAlgorithmIdentifierFinder();
            final AlgorithmIdentifier hashingAlgorithmIdentifier = hashAlgorithmFinder.find(hashingAlgorithm);
            final DigestInfo digestInfo = new DigestInfo(hashingAlgorithmIdentifier, newMessageHash);
            final byte[] hashToEncrypt = digestInfo.getEncoded();

            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            final byte[] decryptedMessageHash = cipher.doFinal(encryptedMessageHash);
            return MessageDigest.isEqual(decryptedMessageHash, hashToEncrypt);
        } catch (GeneralSecurityException | IOException exp) {
            throw new SecurityException("Error during verifying", exp);
        }
    }

    public static byte[] hashFile(byte[] messageBytes, String hashingAlgorithm) {
        try {
            final MessageDigest md = MessageDigest.getInstance(hashingAlgorithm);
            return md.digest(messageBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Error during file hash generation", e);
        }
    }

    public static String bytesToHex(byte[] hash) {
        final StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Compute SHA-1 thumbprint (aka fingerprint) of X509 certificate (useful for
     * @param cert the X509 certificate we want to get thumbprint
     * @return the thumbprint of the certificate
     */
    public static String getThumbprint(X509Certificate cert) {
        try {
            final MessageDigest md = MessageDigest.getInstance(X509_CERTIFICATE_FINGERPRINT_HASH_ALGORITHM);
            md.update(cert.getEncoded());
            return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new SecurityException("Error during computing thumbprint of certificate " + cert.toString(), e);
        }
    }

    /**
     * Get public key certificate encoded base64
     * @param cert the X509 certificate we want to get public key
     * @return the public key of the certificate
     */
    public static String getPublicKey(X509Certificate cert) {
        final PublicKey publicKey = cert.getPublicKey();
        final byte[] encoded = publicKey.getEncoded();
        final byte[] b64key = Base64.getEncoder().encode(encoded);
        return new String(b64key);
    }
}
