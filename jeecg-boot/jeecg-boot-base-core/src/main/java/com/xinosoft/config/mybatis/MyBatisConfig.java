package com.xinosoft.config.mybatis;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@MapperScan({"org.jeecg.**.mapper*", "com.xinosoft.modules.**.mapper*"})
public class MyBatisConfig {
    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration.getTypeHandlerRegistry().register(JSONObjectTypeHandler.class);
    }
    @Bean
    public SchemaInterceptor schemaInterceptor() {
        return new SchemaInterceptor();
    }
}
