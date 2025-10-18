package com.example.miniodemo.entity;

import lombok.Data;

@Data
public class Image {
    private Long id;
    private Long ownerId;
    private String originalName;
    private String contentType;
    private String storageKey;
    private Long sizeBytes;
    private Integer width;
    private Integer height;
    private String sha256Hex;
    private String status; // UPLOADING | READY | FAILED
}