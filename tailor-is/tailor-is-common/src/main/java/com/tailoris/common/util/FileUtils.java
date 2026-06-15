package com.tailoris.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public final class FileUtils {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt");
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip", "rar", "7z", "tar", "gz");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm");

    private FileUtils() {
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            return SIZE_FORMAT.format(bytes / (double) KB) + " KB";
        } else if (bytes < GB) {
            return SIZE_FORMAT.format(bytes / (double) MB) + " MB";
        } else if (bytes < TB) {
            return SIZE_FORMAT.format(bytes / (double) GB) + " GB";
        } else {
            return SIZE_FORMAT.format(bytes / (double) TB) + " TB";
        }
    }

    public static boolean isImageFile(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String ext = getFileExtension(filename).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext);
    }

    public static boolean isDocumentFile(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String ext = getFileExtension(filename).toLowerCase();
        return DOCUMENT_EXTENSIONS.contains(ext);
    }

    public static boolean isArchiveFile(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String ext = getFileExtension(filename).toLowerCase();
        return ARCHIVE_EXTENSIONS.contains(ext);
    }

    public static boolean isVideoFile(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }
        String ext = getFileExtension(filename).toLowerCase();
        return VIDEO_EXTENSIONS.contains(ext);
    }

    public static String getFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    public static String getFileNameWithoutExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    public static String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return StringUtils.isNotBlank(extension) ? uuid + "." + extension : uuid;
    }

    public static String calculateMD5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Calculate MD5 failed", e);
        }
    }

    public static String calculateMD5(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            try (InputStream is = file.getInputStream()) {
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Calculate MD5 failed", e);
        }
    }

    public static File multipartFileToFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String fileName = generateFileName(originalFilename);
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        Files.copy(multipartFile.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return path.toFile();
    }

    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        deleteFile(f);
                    }
                }
            }
            file.delete();
        }
    }

    public static void deleteFile(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException("Delete file failed", e);
            }
        }
    }

    public static long getFileSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        return file.length();
    }

    public static boolean isValidFilePath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        return !path.contains("..") && !path.contains("\\") && path.startsWith("/");
    }
}
