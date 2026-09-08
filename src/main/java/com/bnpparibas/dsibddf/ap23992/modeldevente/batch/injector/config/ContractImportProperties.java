package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "batch.injector")
public record ContractImportProperties(
        String inputFile,
        @DefaultValue("100") Integer chunkSize,
        @DefaultValue("src/main/resources/invalid-contracts.txt") String outputFile,
        @DefaultValue("UTF-8") Charset charset) {

    public ContractImportProperties {
        if (chunkSize == null || chunkSize <= 0) {
            chunkSize = 100;
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
    }
}