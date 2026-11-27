package com.bethelreport.backend.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = "storage.cleanup.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class StorageSchedulingConfig {
}
