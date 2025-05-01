package com.xinosoft.config.mybatis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collections;

// @Configuration
public class DataSourceConfig {
    @Value("${spring.datasource.dynamic.datasource.master.url}")
    private String urlTemplate; // jdbc:mysql://host:port/

    @Value("${spring.datasource.dynamic.datasource.master.username}")
    private String username;

    @Value("${spring.datasource.dynamic.datasource.master.password}")
    private String password;

    @Value("${spring.datasource.dynamic.datasource.master.default-db-name}")
    private String defaultDbName;

    @Bean
    public DataSource dataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        // 默认数据源（可以指定一个默认数据库）
        DataSource defaultDataSource = DataSourceBuilder.create()
                .url(urlTemplate + defaultDbName)
                .username(username)
                .password(password)
                .build();

        dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
        dynamicDataSource.setTargetDataSources(Collections.emptyMap());
        return dynamicDataSource;
    }

    // 动态获取数据源
    public DataSource getDataSource(String dbName) {
        return DataSourceBuilder.create()
                .url(urlTemplate + dbName)
                .username(username)
                .password(password)
                .build();
    }
}
