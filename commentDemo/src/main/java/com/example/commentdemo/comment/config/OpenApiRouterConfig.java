package com.example.commentdemo.comment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class OpenApiRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> commentOpenApiRouter() {
        var resource = new ClassPathResource("comment.yaml");
        return RouterFunctions.route(GET("/comment.yaml"),
                request -> ServerResponse.ok()
                        .contentType(MediaType.valueOf("application/yaml"))
                        .bodyValue(resource));
    }
}
