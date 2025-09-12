package fr.shom.ping.utils;

import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;

public final class CompressedArchiveUtil {
    private CompressedArchiveUtil() {
    }

    public static Set<String> listFilesAndDirectories(String dir, int depth, Set<String> allowedExtensions) throws IOException {
        // https://www.baeldung.com/java-list-directory-files
        try (Stream<Path> stream = Files.walk(Path.of(dir), depth)) {
            return stream
                .filter(file -> !Files.isDirectory(file) && allowedExtensions.contains(FilenameUtils.getExtension(file.toFile().getName()).toLowerCase()))
                .map(Path::toString)
                .collect(Collectors.toSet());
        }
    }

    public static void decompressZip(Path source, Path target) throws IOException {
        // https://www.baeldung.com/java-compress-and-uncompress
        final byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                final File newFile = newFile(target.toFile(), zipEntry.getName());
                if (!zipEntry.isDirectory()) {
                    // fix for Windows-created archives
                    final File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                } else if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    public static void decompressRar(Path source, Path target) throws RarException, IOException {
        final List<File> files = Junrar.extract(source.toFile(), target.toFile());
        if (files.isEmpty()) {
            throw new RarException("No file extracted from RAR archive");
        }
    }

    // https://security.snyk.io/research/zip-slip-vulnerability
    public static File newFile(File destinationDir, String entryName) throws IOException {
        final File destFile = new File(destinationDir, entryName);

        final String destDirPath = destinationDir.getCanonicalPath();
        final String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + entryName);
        }

        return destFile;
    }

    public static void decompress7z(Path source, Path target) throws IOException {
        // https://itsallbinary.com/apache-commons-compress-sevenzip-unsevenzip-examples-7z/
        try (SevenZFile sevenZFile = new SevenZFile(source.toFile())) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                final File newFile = newFile(target.toFile(), entry.getName());
                final File parent = newFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                // Stream file content
                final byte[] content = new byte[(int) entry.getSize()];
                sevenZFile.read(content);
                Files.write(newFile.toPath(), content);
            }
        }
    }

    public static void decompressGzip(Path source, Path target, boolean tar) throws IOException {
        if (!tar) {
            try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(source.toFile()));
                 FileOutputStream fos = new FileOutputStream(target.toFile())) {

                // copy GZIPInputStream to FileOutputStream
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } else {
            try (InputStream fi = Files.newInputStream(source);
                 BufferedInputStream bi = new BufferedInputStream(fi);
                 GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
                 TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

                ArchiveEntry entry;
                while ((entry = ti.getNextEntry()) != null) {
                    // output file/folder
                    final Path newPath = newFile(target.toFile(), entry.getName()).toPath();

                    // copy inputstream content to disk
                    Files.copy(ti, newPath);
                }
            }
        }

    }
}
