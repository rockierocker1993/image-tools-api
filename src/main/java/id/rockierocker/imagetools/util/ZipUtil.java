package id.rockierocker.imagetools.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipUtil {

    /**
     * Zip a single byte array with a given filename and return as zipped byte array
     *
     * @param data     the byte array to zip
     * @param filename the name of the file inside the zip
     * @return zipped byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] zipSingleFile(byte[] data, String filename) throws IOException {
        log.debug("Zipping single file: {} ({} bytes)", filename, data.length);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
            zos.finish();

            byte[] result = baos.toByteArray();
            log.debug("Zip created successfully: {} bytes", result.length);
            return result;
        }
    }

    /**
     * Zip multiple byte arrays with their filenames and return as zipped byte array
     *
     * @param files a map of filename to byte array
     * @return zipped byte array containing all files
     * @throws IOException if an I/O error occurs
     */
    public static byte[] zipMultipleFiles(Map<String, byte[]> files) throws IOException {
        log.debug("Zipping {} files", files.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                String filename = file.getKey();
                byte[] data = file.getValue();

                log.debug("Adding file to zip: {} ({} bytes)", filename, data.length);

                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                zos.write(data);
                zos.closeEntry();
            }

            zos.finish();

            byte[] result = baos.toByteArray();
            log.debug("Zip created successfully: {} bytes", result.length);
            return result;
        }
    }

    /**
     * Zip a single byte array with compression level and return as zipped byte array
     *
     * @param data              the byte array to zip
     * @param filename          the name of the file inside the zip
     * @param compressionLevel  compression level (0-9, where 0=no compression, 9=max compression)
     * @return zipped byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] zipSingleFileWithCompression(byte[] data, String filename, int compressionLevel) throws IOException {
        log.debug("Zipping single file with compression level {}: {} ({} bytes)", compressionLevel, filename, data.length);

        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("Compression level must be between 0 and 9");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            zos.setLevel(compressionLevel);

            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
            zos.finish();

            byte[] result = baos.toByteArray();
            log.debug("Zip created successfully: {} bytes (compression ratio: {:.2f}%)",
                      result.length, (100.0 - (result.length * 100.0 / data.length)));
            return result;
        }
    }

    /**
     * Zip multiple byte arrays with compression level and return as zipped byte array
     *
     * @param files             a map of filename to byte array
     * @param compressionLevel  compression level (0-9, where 0=no compression, 9=max compression)
     * @return zipped byte array containing all files
     * @throws IOException if an I/O error occurs
     */
    public static byte[] zipMultipleFilesWithCompression(Map<String, byte[]> files, int compressionLevel) throws IOException {
        log.debug("Zipping {} files with compression level {}", files.size(), compressionLevel);

        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("Compression level must be between 0 and 9");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            zos.setLevel(compressionLevel);

            long totalSize = 0;
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                String filename = file.getKey();
                byte[] data = file.getValue();
                totalSize += data.length;

                log.debug("Adding file to zip: {} ({} bytes)", filename, data.length);

                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                zos.write(data);
                zos.closeEntry();
            }

            zos.finish();

            byte[] result = baos.toByteArray();
            log.debug("Zip created successfully: {} bytes from {} bytes (compression ratio: {:.2f}%)",
                      result.length, totalSize, (100.0 - (result.length * 100.0 / totalSize)));
            return result;
        }
    }
}
