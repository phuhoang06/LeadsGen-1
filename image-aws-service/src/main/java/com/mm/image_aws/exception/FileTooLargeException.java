package com.mm.image_aws.exception;

/**
 * Exception được throw khi file có dung lượng vượt quá giới hạn cho phép
 */
public class FileTooLargeException extends RuntimeException {
    
    private final long fileSize;
    private final long maxFileSize;
    
    public FileTooLargeException(long fileSize, long maxFileSize) {
        super(String.format("File quá lớn: %d bytes (giới hạn: %d bytes)", fileSize, maxFileSize));
        this.fileSize = fileSize;
        this.maxFileSize = maxFileSize;
    }
    
    public FileTooLargeException(String message, long fileSize, long maxFileSize) {
        super(message);
        this.fileSize = fileSize;
        this.maxFileSize = maxFileSize;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public String getFileSizeMB() {
        return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
    }
    
    public String getMaxFileSizeMB() {
        return String.format("%.2f MB", maxFileSize / (1024.0 * 1024.0));
    }
} 