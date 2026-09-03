package com.example.jobaggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcRepositories(basePackages = "com.example.jobaggregator.persistence")
public class JobAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobAggregatorApplication.class, args);
    }
}
