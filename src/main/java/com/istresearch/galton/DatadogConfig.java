package com.istresearch.galton;

import io.micrometer.core.lang.Nullable;

import java.time.Duration;

/**
 * Datadog Configuration
 */
public class DatadogConfig implements io.micrometer.datadog.DatadogConfig {

    private String apiKey;
    private String applicationKey;

    /**
     * {@inheritDoc}
     */
    @Override
    public String apiKey() { return apiKey; }

    /**
     *
     * @param apiKey
     */
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    /**
     * {@inheritDoc}
     */
    @Override
    public String applicationKey() { return applicationKey; }

    /**
     *
     * @param applicationKey
     */
    public void setApplicationKey(String applicationKey) { this.applicationKey  = applicationKey; }

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration step() { return Duration.ofSeconds(1); }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public String get(String k) { return null; }
}


