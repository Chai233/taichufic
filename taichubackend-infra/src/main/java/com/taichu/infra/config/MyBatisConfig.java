package com.taichu.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {"com.taichu.infra.persistance.mapper"})
public class MyBatisConfig {
}
