package com.example.miniodemo.entity;

import lombok.Data;

@Data
public class ImageVariant {
    private Long id;
    private Long imageId;
    private String variant;   // w1024 / thumb 等
    private Integer width;
    private Integer height;
    private String format;    // webp/jpg
    private String storageKey;
    private Long sizeBytes;
}