package com.example.miniodemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.imageio.ImageIO;
import java.util.Arrays;

@SpringBootApplication
@EnableAsync
public class MinioDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinioDemoApplication.class, args);
    }
}