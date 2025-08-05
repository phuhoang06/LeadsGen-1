package com.mm.image_aws.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.png.PngDirectory;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.repo.ImageJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final ImageJobRepository imageJobRepository;

    public void extractAndSaveMetadata(String jobId, String username, String originalUrl, String cdnUrl, String s3Key, byte[] imageBytes, String errorMessage) {

        ImageMetadata imageMeta = new ImageMetadata();
        imageMeta.setJobId(jobId);
        imageMeta.setUsername(username);
        imageMeta.setOriginalUrl(originalUrl);
        imageMeta.setCdnUrl(cdnUrl);
        imageMeta.setS3Key(s3Key);
        imageMeta.setCreatedAt(Instant.now().toString());

        if (imageBytes != null && imageBytes.length > 0) {
            imageMeta.setFileSize((long) imageBytes.length);
        }

        if (errorMessage != null) {
            imageMeta.setErrorMessage(errorMessage.substring(0, Math.min(errorMessage.length(), 512)));
        }

        if (imageBytes != null && imageBytes.length > 0) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
                extractDimensions(imageMeta, metadata);
                extractDpi(imageMeta, metadata);
            } catch (Exception e) {
                log.error("Lỗi khi trích xuất metadata cho URL {}: {}", originalUrl, e.getMessage());
                if (imageMeta.getErrorMessage() == null) {
                    String errorMsg = "Lỗi trích xuất: " + e.getMessage();
                    imageMeta.setErrorMessage(errorMsg.substring(0, Math.min(errorMsg.length(), 512)));
                }
            }
        }

        imageJobRepository.saveMetadata(imageMeta);
        log.info("Đã lưu metadata cho URL: {}", originalUrl);
    }

    private void extractDimensions(ImageMetadata imageMeta, Metadata metadata) {
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName().toLowerCase();
                int tagType = tag.getTagType();
                if (imageMeta.getWidth() == null && tagName.contains("width")) {
                    try { imageMeta.setWidth(directory.getInteger(tagType)); } catch (Exception e) { /* Bỏ qua */ }
                }
                if (imageMeta.getHeight() == null && tagName.contains("height")) {
                    try { imageMeta.setHeight(directory.getInteger(tagType)); } catch (Exception e) { /* Bỏ qua */ }
                }
            }
        }
    }

    private void extractDpi(ImageMetadata imageMeta, Metadata metadata) {
        JfifDirectory jfifDir = metadata.getFirstDirectoryOfType(JfifDirectory.class);
        if (jfifDir != null) {
            try {
                if (jfifDir.containsTag(JfifDirectory.TAG_RESX) && jfifDir.getInt(JfifDirectory.TAG_RESX) > 0) {
                    int resX = jfifDir.getInt(JfifDirectory.TAG_RESX);
                    int units = jfifDir.getInt(JfifDirectory.TAG_UNITS);
                    if (units == 1) { imageMeta.setDpi(resX); return; } // DPI
                    if (units == 2) { imageMeta.setDpi((int) Math.round(resX * 2.54)); return; } // DPCm
                }
            } catch (Exception e) { log.warn("Lỗi đọc DPI từ JFIF: {}", e.getMessage()); }
        }

        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifDir != null && exifDir.containsTag(ExifIFD0Directory.TAG_X_RESOLUTION)) {
            try {
                double xResolution = exifDir.getRational(ExifIFD0Directory.TAG_X_RESOLUTION).doubleValue();
                int unit = exifDir.containsTag(ExifIFD0Directory.TAG_RESOLUTION_UNIT) ? exifDir.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT) : 2;
                if (unit == 3) { imageMeta.setDpi((int) Math.round(xResolution * 2.54)); }
                else { imageMeta.setDpi((int) Math.round(xResolution)); }
                return;
            } catch (Exception e) { log.warn("Lỗi đọc DPI từ EXIF: {}", e.getMessage()); }
        }

        PngDirectory pngDir = metadata.getFirstDirectoryOfType(PngDirectory.class);
        if (pngDir != null && pngDir.containsTag(PngDirectory.TAG_PIXELS_PER_UNIT_X)) {
            try {
                Integer pixelsPerUnitX = pngDir.getInteger(PngDirectory.TAG_PIXELS_PER_UNIT_X);
                Integer unitSpecifier = pngDir.getInteger(PngDirectory.TAG_UNIT_SPECIFIER);
                if (pixelsPerUnitX != null && unitSpecifier != null && unitSpecifier == 1) {
                    imageMeta.setDpi((int) Math.round(pixelsPerUnitX * 0.0254));
                }
            } catch (Exception e) { log.warn("Lỗi đọc DPI từ PNG: {}", e.getMessage()); }
        }
    }
}
