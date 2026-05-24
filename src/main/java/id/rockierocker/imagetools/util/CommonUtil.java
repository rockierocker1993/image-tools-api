package id.rockierocker.imagetools.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CommonUtil {

    /**
     * Get file extension in lower case
     *
     * @param filename the filename
     * @return the file extension in lower case, or empty string if no extension
     */
    public static String getExtensionLower(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * Get InputStream from MultipartFile, throwing the provided RuntimeException on failure
     *
     * @param multipartFile    the MultipartFile
     * @param runtimeException the RuntimeException to throw on failure
     * @return the InputStream
     */
    public static InputStream getInputStream(MultipartFile multipartFile, RuntimeException runtimeException) {
        try {
            return multipartFile.getInputStream();
        } catch (IOException e) {
            throw runtimeException;
        }
    }


    /**
     * Get InputStream from File, throwing the provided RuntimeException on failure
     *
     * @param file             the File
     * @param runtimeException the RuntimeException to throw on failure
     * @return the InputStream
     */
    public static InputStream toInputStream(File file, RuntimeException runtimeException) {
        try {
            return java.nio.file.Files.newInputStream(file.toPath());
        } catch (IOException e) {
            throw runtimeException;
        }
    }

    public static byte[] getBytes(InputStream inputStream, RuntimeException runtimeException) {
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw runtimeException;
        }
    }

    public static byte[] getBytes(MultipartFile multipartFile, RuntimeException runtimeException) {
        return getBytes(getInputStream(multipartFile, runtimeException), runtimeException);
    }

}
