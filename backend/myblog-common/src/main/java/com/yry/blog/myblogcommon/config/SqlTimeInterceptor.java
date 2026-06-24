package com.yry.blog.myblogcommon.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, org.apache.ibatis.session.ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
@Component
@Slf4j
public class SqlTimeInterceptor implements Interceptor {

    private static final long SLOW_SQL_THRESHOLD = 1000L;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = statementHandler.getBoundSql();
            
            Configuration configuration = getConfiguration(statementHandler);
            String sql = formatSql(boundSql, configuration);
            
            if (executionTime > SLOW_SQL_THRESHOLD) {
                log.warn("[SLOW SQL] 执行时间: {}ms | SQL: {}", executionTime, sql);
            } else {
                log.debug("[SQL] 执行时间: {}ms | SQL: {}", executionTime, sql);
            }
        }
    }

    private Configuration getConfiguration(StatementHandler statementHandler) {
        try {
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            Object mappedStatement = metaObject.getValue("mappedStatement");
            if (mappedStatement != null) {
                MetaObject msMeta = SystemMetaObject.forObject(mappedStatement);
                return (Configuration) msMeta.getValue("configuration");
            }
        } catch (Exception e) {
            log.trace("获取Configuration失败: {}", e.getMessage());
        }
        return null;
    }

    private String formatSql(BoundSql boundSql, Configuration configuration) {
        String sql = boundSql.getSql();
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        if (configuration == null) {
            return sql.replaceAll("\\s+", " ").trim();
        }
        
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        
        if (parameterMappings == null || parameterMappings.isEmpty() || parameterObject == null) {
            return sql.replaceAll("\\s+", " ").trim();
        }
        
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
        } else {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            for (ParameterMapping parameterMapping : parameterMappings) {
                String propertyName = parameterMapping.getProperty();
                if (metaObject.hasGetter(propertyName)) {
                    Object obj = metaObject.getValue(propertyName);
                    sql = sql.replaceFirst("\\?", getParameterValue(obj));
                } else if (boundSql.hasAdditionalParameter(propertyName)) {
                    Object obj = boundSql.getAdditionalParameter(propertyName);
                    sql = sql.replaceFirst("\\?", getParameterValue(obj));
                }
            }
        }
        
        return sql.replaceAll("\\s+", " ").trim();
    }

    private String getParameterValue(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "'" + obj.toString().replace("'", "''") + "'";
        }
        if (obj instanceof Date) {
            return "'" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) obj) + "'";
        }
        if (obj instanceof Number) {
            return obj.toString();
        }
        return "'" + obj.toString() + "'";
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
