package com.railway.reservation.config;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** A database created before online payment had ENUM columns that could not hold the new payment values. */
@SpringBootTest
class SchemaUpgradesTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;

    private String column(String column, String what) {
        return jdbc.queryForObject("select " + what + " from information_schema.columns where table_name = 'PAYMENTS' and column_name = ?",
                String.class, column);
    }

    @AfterEach
    void restore() {
        new SchemaUpgrades(dataSource, emf).afterPropertiesSet();
    }

    @Test
    @DisplayName("TC-PAY-17: Old ENUM payment columns are widened on start, and a second start changes nothing")
    void widensOldEnumColumns() {
        jdbc.execute("alter table payments alter column status set data type enum('SUCCESS','FAILED')");
        jdbc.execute("alter table payments alter column method set data type enum('UPI','CARD','NETBANKING')");
        assertThat(column("STATUS", "data_type")).isEqualTo("ENUM");

        new SchemaUpgrades(dataSource, emf).afterPropertiesSet();
        assertThat(column("STATUS", "data_type")).isEqualTo("CHARACTER VARYING");
        assertThat(column("METHOD", "data_type")).isEqualTo("CHARACTER VARYING");
        assertThat(column("STATUS", "is_nullable")).isEqualTo("NO");

        new SchemaUpgrades(dataSource, emf).afterPropertiesSet();
        assertThat(column("STATUS", "data_type")).isEqualTo("CHARACTER VARYING");
    }
}
