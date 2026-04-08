package com.crowdfunding.dynomite;

import com.crowdfunding.dynomite.config.DynomiteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DynomiteProperties.class)
public class DynomiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynomiteApplication.class, args);
    }
}

