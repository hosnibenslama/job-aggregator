package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.config;

import java.nio.charset.Charset;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contract.import")
public record ContractImportProperties(
        String inputFile,
        Charset charset) {
}