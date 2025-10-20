package com.example.commentdemo.comment.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class R2dbcConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(dialect, List.of(
                new JsonNodeReadConverter(objectMapper),
                new JsonNodeWriteConverter(objectMapper)
        ));
    }

    @ReadingConverter
    static class JsonNodeReadConverter implements Converter<Json, JsonNode> {

        private final ObjectMapper mapper;

        JsonNodeReadConverter(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public JsonNode convert(Json source) {
            if (source == null) {
                return null;
            }
            try {
                return mapper.readTree(source.asString());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse JSONB column", e);
            }
        }
    }

    @WritingConverter
    static class JsonNodeWriteConverter implements Converter<JsonNode, Json> {

        private final ObjectMapper mapper;

        JsonNodeWriteConverter(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public Json convert(JsonNode source) {
            if (source == null) {
                return null;
            }
            try {
                return Json.of(mapper.writeValueAsString(source));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to serialise JSONB column", e);
            }
        }
    }
}
