package com.railway.reservation.config;

import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.Locale;

/**
 * Small schema changes {@code ddl-auto=update} cannot make on a database created by an earlier
 * version, kept as fixed SQL scripts in {@code db/upgrade/}. Hibernate stores enums as native ENUM
 * columns on H2 and MySQL and never widens them, so a database from before online payment would
 * refuse the new payment values. The scripts are written to be safe on every start.
 *
 * <p>Taking the {@link EntityManagerFactory} makes this run after Hibernate has updated the schema
 * and before anything else uses the database.</p>
 */
@Component
public class SchemaUpgrades implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrades.class);

    private final DataSource dataSource;

    public SchemaUpgrades(DataSource dataSource, EntityManagerFactory schemaReady) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        String product;
        try {
            product = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        } catch (MetaDataAccessException e) {
            log.warn("Could not identify the database, schema upgrades skipped: {}", e.getMessage());
            return;
        }
        String name = product == null ? "" : product.toLowerCase(Locale.ROOT);
        String script = name.contains("h2") ? "db/upgrade/h2.sql" : name.contains("mysql") ? "db/upgrade/mysql.sql" : null;
        if (script == null) return;
        new ResourceDatabasePopulator(new ClassPathResource(script)).execute(dataSource);
        log.info("Schema upgrades applied ({})", script);
    }
}
