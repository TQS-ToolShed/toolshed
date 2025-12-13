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

    @Value("${GEO_WARM_CACHE_DELAY_MS:250}")
    private long delayMs;

    @Value("${GEO_WARM_CACHE_RETRIES:3}")
    private int retries;

    @Value("${GEO_WARM_CACHE_BACKOFF_MS:1000}")
    private long backoffMs;

    @Value("${GEO_WARM_CACHE_MAX_DISTRICTS:0}")
    private int maxDistricts;

    public GeoCacheWarmupRunner(IGeoApiService geoApiService) {
        this.geoApiService = geoApiService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!warmCacheEnabled) {
            return;
        }

        long startedAt = System.nanoTime();
        log.info("Geo warm-cache enabled; warming districts + municipalities");

        List<String> districts = geoApiService.getAllDistricts();
        if (districts.isEmpty()) {
            log.warn("Geo warm-cache: no districts available (likely rate-limited). Skipping warm-up.");
            return;
        }

        int limit = maxDistricts > 0 ? Math.min(maxDistricts, districts.size()) : districts.size();
        for (int i = 0; i < limit; i++) {
            String district = districts.get(i);
            if (district == null || district.isBlank()) {
                continue;
            }

            boolean warmed = warmMunicipalitiesWithRetry(district);
            if (!warmed) {
                log.warn("Geo warm-cache: failed to warm municipalities for '{}'", district);
            }

            sleepQuietly(delayMs);
        }

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        log.info("Geo warm-cache finished ({} districts) in {}ms", limit, elapsedMs);
    }

    private boolean warmMunicipalitiesWithRetry(String district) {
        for (int attempt = 1; attempt <= Math.max(1, retries); attempt++) {
            List<String> municipalities = geoApiService.getMunicipalitiesByDistrict(district);

            // If it's already cached (disk/in-memory), this will return immediately with data.
            if (municipalities != null && !municipalities.isEmpty()) {
                return true;
            }

            if (attempt < retries) {
                long sleep = backoffMs * (1L << (attempt - 1));
                log.info("Geo warm-cache: retry {}/{} for '{}' after {}ms", attempt, retries, district, sleep);
                sleepQuietly(sleep);
            }
        }
        return false;
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
