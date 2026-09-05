package com.example.jobaggregator.config;

import java.nio.charset.Charset;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contract.import")
public record ContractImportProperties(
        String inputFile,
        Charset charset) {
}