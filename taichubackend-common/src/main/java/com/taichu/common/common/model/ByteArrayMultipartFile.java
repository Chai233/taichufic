package com.taichu.common.common.model;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final long size;

    public ByteArrayMultipartFile(byte[] content, String name,
                                  String originalFilename, String contentType) {
        this.content = content != null ? content.clone() : new byte[0];
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = this.content.length;
    }

    // 便利构造器
    public ByteArrayMultipartFile(byte[] content, String originalFilename, String contentType) {
        this(content, "file", originalFilename, contentType);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return this.content.clone();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(this.content);
            bos.flush();
        }
    }

    @Override
    public String toString() {
        return "EnhancedByteArrayMultipartFile{" +
                "name='" + name + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                '}';
    }
}