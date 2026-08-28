package com.example.jobaggregator.config;

import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contract.import")
public record ContractImportProperties(
        Path inputFile,
        Path partitionDirectory,
        int requestedPartitions,
        Charset charset) {
}