package com.example.miniodemo.dto;

import jakarta.validation.constraints.*;

public record PresignReq(
        @NotBlank String filename,
        @NotBlank String contentType,
        @Positive long size
) {}