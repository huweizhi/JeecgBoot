package com.xinosoft.config.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.sql.Connection;

/**
 * 根据租户ID动态设置数据库模式
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@Slf4j
public class SchemaInterceptor implements Interceptor {
    private static final ThreadLocal<String> SCHEMA_HOLDER = new ThreadLocal<>();

    public static void setSchema(String schema) {
        SCHEMA_HOLDER.set(schema);
    }

    public static void clearSchema() {
        SCHEMA_HOLDER.remove();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String schema = SCHEMA_HOLDER.get();
        log.info("SchemaInterceptor schema: " + schema);
        if (schema != null && !schema.isEmpty()) {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = statementHandler.getBoundSql();
            if (boundSql.getSql().contains("sys_tenant")) {
                // 处理多租户表的SQL
                String sql = boundSql.getSql();
                if (!sql.startsWith(schema + ".")) {
                    // 简单处理，实际应根据SQL复杂情况处理
                    sql = schema + "." + sql;
                    resetSql(statementHandler, sql);
                }
            }
            String sql = boundSql.getSql();
            if (!sql.startsWith(schema + ".")) {
                if (sql.contains("sys_tenant")) {
                    String newSql = sql.replaceAll("(from|into|update|join)\\s+(\\w+)",
                            "$1 " + "jeecg_boot2" + ".$2");
                    resetSql(statementHandler, newSql);
                    log.info("SchemaInterceptor sql: " + sql);
                } else {
                    // 简单处理，实际应根据SQL复杂情况处理
//                    sql = schema + "." + sql;
                    String newSql = sql.replaceAll("(from|into|update|join)\\s+(\\w+)",
                            "$1 " + schema + ".$2");
                    resetSql(statementHandler, newSql);
                }
            }
        }
        return invocation.proceed();
    }

    private void resetSql(StatementHandler statementHandler, String sql) throws NoSuchFieldException, IllegalAccessException {
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(statementHandler.getBoundSql(), sql);
    }
}
