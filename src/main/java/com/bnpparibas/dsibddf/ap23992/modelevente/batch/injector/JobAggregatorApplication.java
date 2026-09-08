package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcRepositories(basePackages = "com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.persistence")
public class JobAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobAggregatorApplication.class, args);
    }
}
