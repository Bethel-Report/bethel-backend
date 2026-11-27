package com.bethelreport.backend.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(value = "storage.cleanup.enabled", matchIfMissing = true)
@EnableScheduling
public class StorageSchedulingConfig {
}
