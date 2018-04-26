package com.istresearch.galton;

import io.micrometer.core.lang.Nullable;

import java.time.Duration;

/**
 * Datadog Statsd Configuration
 */
public class DogStatsdConfig implements io.micrometer.statsd.StatsdConfig {

    private String host;

    /**
     * {@inheritDoc}
     */
    @Override
    public String host() { return host; }

    /**
     *
     * @param host
     */
    public void setHost(String host) { this.host = host; }

    /**
     * {@inheritDoc}
     */
    @Override
    public Duration step() { return Duration.ofSeconds(10); }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public String get(String k) { return null; }
}
