package com.xinosoft.config.mybatis;


import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {
    private static final ThreadLocal<String> DB_NAME = new ThreadLocal<>();

    public static void setDatabase(String dbName) {
        DB_NAME.set(dbName);
    }

    public static void clear() {
        DB_NAME.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return DB_NAME.get();
    }
}
