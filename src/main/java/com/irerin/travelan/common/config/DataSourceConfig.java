package com.irerin.travelan.common.config;

import java.util.Map;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
@Profile("!test")
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.write")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @FlywayDataSource
    @ConfigurationProperties("spring.datasource.write.hikari")
    public HikariDataSource writeDataSource() {
        return writeDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read.hikari")
    public HikariDataSource readDataSource() {
        return readDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public RoutingDataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
                DataSourceType.READ_WRITE, writeDataSource(),
                DataSourceType.READ_ONLY, readDataSource()
        ));
        routingDataSource.setDefaultTargetDataSource(writeDataSource());
        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }
}
