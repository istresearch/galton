package com.istresearch.galton;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extends the Logback AppenderBase to publish metrics to registries configured in logback.xml.
 */
public class MetricsAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsAppender.class);

    /**
     * Collection of gauges configured in logback.xml
     */
    private Map<String, String> gauges = new HashMap<>();

    /**
     * Collection of counters configured in logback.xml
     */
    private List<String> counters = new ArrayList<>();

    /**
     * Prefix used in log marker entry to indicate the type is a gauge.
     */
    private String gaugePrefix = "";

    /**
     * Prefix used in log marker entry to indicate the type is a counter.
     */
    private String counterPrefix = "";

    /**
     * Prefix is prepended to the metric names prior to shipping to its destination.
     */
    private String appPrefix = "";

    /**
     * List of MeterRegistryConfigs in logback.xml
     */
    private List<MeterRegistryConfig> configs = new ArrayList<>();

    /**
     * List of MeterRegistry types
     */
    private List<MeterRegistry> registries = new ArrayList<>();

    /**
     * DatadogConfig from logback.xml
     */
    private DatadogConfig datadogConfig;

    /**
     *
     * @return
     */
    public Map<String, String> getGauges() { return gauges; }

    /**
     *
     * @param gauge
     */
    public void addGauge(Gauge gauge) { gauges.put(gauge.getKey(), gauge.getValue()); }

    /**
     *
     * @param counter
     */
    public void addCounter(String counter) { counters.add(counter); }

    /**
     *
     * @return
     */
    public List<String> getCounters() { return counters; }

    /**
     *
     * @return String
     */
    public String getGaugePrefix() { return gaugePrefix; }

    /**
     *
     * @param gaugePrefix
     */
    public void setGaugePrefix(String gaugePrefix) { this.gaugePrefix = gaugePrefix; }

    /**
     *
     * @return String
     */
    public String getCounterPrefix() { return counterPrefix; }

    /**
     *
     * @param counterPrefix
     */
    public void setCounterPrefix(String counterPrefix) { this.counterPrefix = counterPrefix; }

    /**
     *
     * @return String
     */
    public String getAppPrefix() { return !appPrefix.isEmpty() ? appPrefix + "." : appPrefix; }

    /**
     *
     * @param appPrefix
     */
    public void setAppPrefix(String appPrefix) { this.appPrefix = appPrefix; }

    /**
     *
     * @return DatadogConfig
     */
    public DatadogConfig getDatadogConfig() { return datadogConfig; }

    /**
     *
     * @param datadogConfig
     */
    public void setDatadogConfig(DatadogConfig datadogConfig)
    {
        this.datadogConfig = datadogConfig;
        configs.add(this.datadogConfig);
    }

    /**
     * @return List
     */
    public List<MeterRegistryConfig> getConfigs() {
        return configs;
    }

    /**
     * Lazy loader of all MeterRegistry types configured in logback.xml
     * @return List
     */
    public List<MeterRegistry> getRegistries() {
        if(this.registries.isEmpty()) {
            for(MeterRegistryConfig config : getConfigs()) {
                MeterRegistry r = MeterRegistryFactory.createInstance(config);
                if(r != null) this.registries.add(r);
            }
        }
        return this.registries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(final ILoggingEvent event) {
        Marker marker = event.getMarker();
        String metricName = event.getMessage().toLowerCase().replace(" ", "_");
        if(getCounters().contains(metricName)) { publishCounterMetric(metricName); }

        if(marker != null) {
            Map<String, String> markerMap = convertMarkerToMap(marker);
            if(getGauges().containsKey(metricName)) {
                //obtains the marker key specified in the logback.xml that correlates w/ the metricName (message)
                publishGaugeMetric(metricName, Integer.valueOf(markerMap.get(getGauges().get(metricName))));
            }
            for(String key : markerMap.keySet()) {
                //checks each key to see if it contains the prefix
                if(key.contains(getCounterPrefix())) { publishCounterMetric(markerMap.get(key)); }
                else if(key.contains(getGaugePrefix())) {
                    publishGaugeMetric(key.replace(getGaugePrefix(), ""),
                            Integer.valueOf(markerMap.get(key)));
                }
                if(getCounters().contains(key)) { publishCounterMetric(key); }
                if(getGauges().containsKey(key)) { publishGaugeMetric(metricName, Integer.valueOf(markerMap.get(key))); }
            }
        }
    }

    /**
     * Sort of a hack to obtain the key/value pairs in the marker
     * @param marker
     * @return Map
     */
    private Map<String, String> convertMarkerToMap(Marker marker) {
        String markerStr = marker.toString();
        Map<String, String> markerMap = new HashMap<>();
        for(String m : markerStr.split(", ")) {
            m = m.replace("{", "");
            m = m.replace("}", "");
            String [] s = m.split("=");
            markerMap.put(s[0], s[1]);
        }
        return markerMap;
    }

    /**
     * Publishes the counter to all meter registries
     * @param name
     */
    private void publishCounterMetric(String name) {
        for(MeterRegistry registry : getRegistries()) {
            Counter counter = registry.counter(getAppPrefix() + name);
            counter.increment();
            LOG.debug(String.format("registry %s has %d meters",
                        registry.getClass().getTypeName(),
                        registry.getMeters().size()));
        }
    }

    /**
     * Publishes the gauge to all meter registries
     * @param name
     * @param val
     */
    private void publishGaugeMetric(String name, int val) {
        for(MeterRegistry registry : getRegistries()) {
            registry.gauge(getAppPrefix() + name, new AtomicInteger(val));
            LOG.debug(String.format("registry %s has %d meters",
                        registry.getClass().getTypeName(),
                        registry.getMeters().size()));
        }
    }

    /**
     * Simple Gauge def for config injection
     */
    public static class Gauge {
        private String key;
        private String value;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
