package com.example.treechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.treechat.mapper")
public class TreeChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(TreeChatApplication.class, args);
    }

}
