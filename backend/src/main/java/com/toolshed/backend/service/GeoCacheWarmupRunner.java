package com.toolshed.backend.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GeoCacheWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeoCacheWarmupRunner.class);

    private final IGeoApiService geoApiService;

    @Value("${GEO_WARM_CACHE:false}")
    private boolean warmCacheEnabled;

    public GeoCacheWarmupRunner(IGeoApiService geoApiService) {
        this.geoApiService = geoApiService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!warmCacheEnabled) {
            return;
        }

        long startedAt = System.nanoTime();
        log.info("Geo warm-cache enabled; warming districts");

        List<String> districts = geoApiService.getAllDistricts();
        if (districts.isEmpty()) {
            log.warn("Geo warm-cache: no districts available (likely rate-limited). Skipping warm-up.");
            return;
        }

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        log.info("Geo warm-cache finished ({} districts) in {}ms", districts.size(), elapsedMs);
    }
}
