package com.example.videodemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /vod/** 映射到本地目录 storage/public/vod/ （调试时可直接通过本机播放）
        registry.addResourceHandler("/vod/**")
                .addResourceLocations("file:storage/public/vod/");
    }
}