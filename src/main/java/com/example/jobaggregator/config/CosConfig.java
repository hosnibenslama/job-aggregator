package com.example.jobaggregator.config;

import com.example.jobaggregator.storage.CloudObjectStorageService;
import com.example.jobaggregator.storage.LocalStagingCosService;
import com.example.jobaggregator.storage.S3CompatibleCosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(com.example.jobaggregator.config.CosProperties.class)
public class CosConfig {

    private static final Logger log = LoggerFactory.getLogger(CosConfig.class);

    @Bean
    public CloudObjectStorageService cloudObjectStorageService(
            CosProperties cosProperties,
            ContractImportProperties importProperties,
            ResourceLoader resourceLoader) {
        if (cosProperties.enabled()) {
            log.info("Initializing production S3CompatibleCosService for endpoint: {}, bucket: {}",
                    cosProperties.endpoint(), cosProperties.bucket());
            return new S3CompatibleCosService(cosProperties);
        } else {
            log.info("COS is disabled (cos.enabled=false). Using LocalStagingCosService simulation.");
            return new LocalStagingCosService(
                    cosProperties.stagingDirectory(),
                    resourceLoader,
                    importProperties.inputFile());
        }
    }
}
