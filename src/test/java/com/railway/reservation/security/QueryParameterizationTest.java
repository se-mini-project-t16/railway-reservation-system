package com.railway.reservation.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RRS-SR-003: all database access must go through Spring Data JPA / parameterized queries.
 * This is a static check over the production sources.
 */
@DisplayName("Parameterized queries only [RRS-SR-003]")
class QueryParameterizationTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final List<String> RAW_SQL_APIS = List.of(
            "createNativeQuery(",
            "createQuery(",
            "createStatement(",
            "prepareStatement(",
            "JdbcTemplate",
            "NamedParameterJdbcTemplate",
            "java.sql.Statement"
    );

    @Test
    void productionCodeDoesNotUseRawSqlApis() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file);
                for (String api : RAW_SQL_APIS) {
                    assertThat(content).as("%s must not use %s", file, api).doesNotContain(api);
                }
            }
        }
    }

    @Test
    void everyCustomQueryUsesNamedParameters() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file);
                if (content.contains("@Query")) {
                    assertThat(content).as("%s has @Query but no @Param binding", file).contains("@Param");
                    assertThat(content).as("%s has @Query without a :named parameter", file).containsPattern(":\\w+");
                }
            }
        }
    }
}
