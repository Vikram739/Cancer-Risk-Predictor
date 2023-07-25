package com.cancerrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CancerRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(CancerRiskApplication.class, args);
    }
}
