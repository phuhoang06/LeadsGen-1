package com.mm.image_aws.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.png.PngDirectory;
import com.mm.image_aws.entity.ImageMetadata;
import com.mm.image_aws.entity.UploadJob;
import com.mm.image_aws.repo.ImageMetadataRepository;
import com.mm.image_aws.repo.UploadJobRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final ImageMetadataRepository imageMetadataRepository;
    private final UploadJobRepository uploadJobRepository;

    @Transactional
    public void extractAndSaveMetadata(Long jobId, String originalUrl, String cdnUrl, byte[] imageBytes, String errorMessage) {
        // Lấy một tham chiếu (reference) tới job mà không cần tải toàn bộ entity
        UploadJob jobReference = uploadJobRepository.getReferenceById(jobId);

        ImageMetadata imageMeta = new ImageMetadata();
        imageMeta.setJob(jobReference); // Đổi tên từ setUploadJob
        imageMeta.setOriginalUrl(originalUrl);
        imageMeta.setCdnUrl(cdnUrl);
        imageMeta.setCreatedAt(LocalDateTime.now());

        if (imageBytes != null && imageBytes.length > 0) {
            imageMeta.setFileSize(imageBytes.length);
        }

        if (errorMessage != null) {
            // Cắt ngắn message nếu nó quá dài so với cột trong DB
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
                    imageMeta.setErrorMessage(("Lỗi trích xuất: " + e.getMessage()).substring(0, Math.min(e.getMessage().length(), 512)));
                }
            }
        }

        imageMetadataRepository.save(imageMeta);
        log.info("Đã lưu metadata cho URL: {}", originalUrl);
    }

    private void extractDimensions(ImageMetadata imageMeta, Metadata metadata) {
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                String tagName = tag.getTagName().toLowerCase();
                int tagType = tag.getTagType();
                // So sánh với null sẽ hoạt động vì width/height giờ là Integer
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
        // Ưu tiên 1: JFIF
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

        // Ưu tiên 2: EXIF
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifDir != null && exifDir.containsTag(ExifIFD0Directory.TAG_X_RESOLUTION)) {
            try {
                double xResolution = exifDir.getRational(ExifIFD0Directory.TAG_X_RESOLUTION).doubleValue();
                int unit = exifDir.containsTag(ExifIFD0Directory.TAG_RESOLUTION_UNIT) ? exifDir.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT) : 2; // Default to inches
                if (unit == 3) { imageMeta.setDpi((int) Math.round(xResolution * 2.54)); } // Centimeters
                else { imageMeta.setDpi((int) Math.round(xResolution)); } // Inches
                return;
            } catch (Exception e) { log.warn("Lỗi đọc DPI từ EXIF: {}", e.getMessage()); }
        }

        // Ưu tiên 3: PNG
        PngDirectory pngDir = metadata.getFirstDirectoryOfType(PngDirectory.class);
        if (pngDir != null && pngDir.containsTag(PngDirectory.TAG_PIXELS_PER_UNIT_X)) {
            try {
                Integer pixelsPerUnitX = pngDir.getInteger(PngDirectory.TAG_PIXELS_PER_UNIT_X);
                Integer unitSpecifier = pngDir.getInteger(PngDirectory.TAG_UNIT_SPECIFIER);
                // 1 = meters
                if (pixelsPerUnitX != null && unitSpecifier != null && unitSpecifier == 1) {
                    imageMeta.setDpi((int) Math.round(pixelsPerUnitX * 0.0254)); // Pixels per meter to DPI
                }
            } catch (Exception e) { log.warn("Lỗi đọc DPI từ PNG: {}", e.getMessage()); }
        }
    }
}