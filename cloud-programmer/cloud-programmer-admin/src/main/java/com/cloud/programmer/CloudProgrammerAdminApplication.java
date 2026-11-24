package com.cloud.programmer;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author 周梓强 2026/9/22
 */

@SpringBootApplication
@MapperScan({
        "com.cloud.programmer.user.mapper",
        "com.cloud.programmer.agent.mapper"
})
@ConfigurationPropertiesScan(basePackages = "com.cloud.programmer")
public class CloudProgrammerAdminApplication {


    public static void main(String[] args) {

        SpringApplication.run(CloudProgrammerAdminApplication.class, args);
    }
}
