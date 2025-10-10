package com.example.treechat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
@MapperScan("com.example.treechat.mapper")
public class MyBatisPlusConfig {
    // MyBatis-Plus 自动配置已足够,无需额外配置
}
