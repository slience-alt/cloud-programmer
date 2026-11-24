package com.cloud.programmer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author 周梓强 2026/9/22
 */

@SpringBootApplication
@MapperScan({
        "com.cloud.programmer.runtime.mapper"
})
@ConfigurationPropertiesScan(basePackages = "com.cloud.programmer")
public class CloudProgrammerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudProgrammerAgentApplication.class, args);
    }
}
