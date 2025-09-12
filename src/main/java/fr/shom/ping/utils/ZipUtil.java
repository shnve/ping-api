package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

@Slf4j
public final class ZipUtil {
    private ZipUtil() {
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public static void generateZipFileAndAttachToServletResponse(String directoryPathname, HttpServletResponse response) throws ServiceException {

        final File[] generatedFiles =
            new File(Path.of(FilenameUtils.getFullPath(directoryPathname), FilenameUtils.getName(directoryPathname)).toString()).listFiles();
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (File generatedFile : generatedFiles) {
                addFile(generatedFile, zipOut, generatedFile.getName());
            }
            zipOut.finish();
        } catch (Exception exception) {
            throw new ServiceException("Failed to zip generated files : " + exception.getMessage());
        }
    }

    private static void addFile(File generatedFile, ZipOutputStream zipOut, String zipEntryName) throws IOException, ServiceException {
        try (final FileInputStream fis = new FileInputStream(generatedFile)) {
            final ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
            zipOut.closeEntry();
        } catch (IOException e) {
            log.error("Failed to read file {}", generatedFile.getAbsolutePath());
            throw new ServiceException("Failed to read file : " + generatedFile.getAbsolutePath(), e);
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public static void generateZipFileOfDirectoriesAndAttachToServletResponse(String directoryPathname, HttpServletResponse response) throws ServiceException {

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            addDir(new File(directoryPathname), zipOut, directoryPathname);
            zipOut.finish();
        } catch (Exception exception) {
            log.error("Failed to zip generated files from " + directoryPathname + ": " + exception.getMessage(), exception);
            throw new ServiceException("Failed to zip generated files from " + directoryPathname + ": " + exception.getMessage());
        }
    }

    public static void zipDirectory(String directoryPathname, String outputZipFileLocation, String baseDirName) throws ServiceException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputZipFileLocation))) {
            addDirToBaseDirByRelativePath(new File(directoryPathname), zipOut, baseDirName);
            zipOut.finish();
        } catch (Exception exception) {
            log.error("Failed to zip folder {} to {} ", baseDirName, outputZipFileLocation, exception);
            throw new ServiceException("Failed to zip folder " + baseDirName + " to " + outputZipFileLocation, exception);
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public static void addDir(File directoryPathname, ZipOutputStream zipOut, String rootDirPath) throws IOException, ServiceException {
        final File[] listedFiles = new File(Path.of(FilenameUtils.getFullPath(directoryPathname.getAbsolutePath()),
            FilenameUtils.getName(directoryPathname.getAbsolutePath())).toString()).listFiles();
        for (File file : listedFiles) {
            if (file.isDirectory()) {
                addDir(file, zipOut, rootDirPath);
                continue;
            }

            final String name = file.getAbsolutePath().substring(rootDirPath.length() + 1);
            addFile(file, zipOut, name);
        }
    }

    /**
     * Add files from directory to a zip output stream to create a zip file at baseDirName base on relative path
     *
     * @param directoryPathname directory path
     * @param zipOut            zip output stream
     * @param baseDirName       base directory name
     * @throws IOException example: directoryPathname = /DATA/ping/api/s124/FR02/2025/124FR02C101025F/S100_ROOT/...
     *                     baseDirName = S100_ROOT
     *                     zip file will be created beginning with S100_ROOT/... in the zip file and include all files in the directory
     */
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public static void addDirToBaseDirByRelativePath(File directoryPathname, ZipOutputStream zipOut, String baseDirName) throws IOException, ServiceException {
        final File[] listedFiles = directoryPathname.listFiles();
        for (File file : listedFiles) {
            if (file.isDirectory()) {
                addDirToBaseDirByRelativePath(file, zipOut, baseDirName);
                continue;
            }

            final String relativePath = getRelativePath(file, baseDirName);
            addFile(file, zipOut, relativePath);
        }
    }

    public static String getRelativePath(File file, String baseDirName) {
        String absolutePath = file.getAbsolutePath();
        return baseDirName + File.separator + absolutePath.substring(absolutePath.indexOf(baseDirName) + baseDirName.length() + 1);
    }

    public static List<String> listFiles(String filename) throws ServiceException {
        final List<String> filesFound = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(new File(filename))) {
            filesFound.addAll(
                zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory)).map(ZipEntry::getName).toList()
            );
        } catch (IOException e) {
            log.error("Failed to list content of '{}'", filename, e);
            throw new ServiceException("Failed to list content of '" + filename + "'. Cause: " + e.getMessage());
        }
        return filesFound;
    }
}
