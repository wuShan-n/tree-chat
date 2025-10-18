package com.example.miniodemo.dto;

import java.util.Map;

public record PresignResp(String id, String uploadUrl, Map<String, String> requiredHeaders) {}