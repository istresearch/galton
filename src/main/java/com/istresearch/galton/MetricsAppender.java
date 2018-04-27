package com.istresearch.galton;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extends the Logback AppenderBase to publish metrics to registries configured in logback.xml.
 *
 */
public class MetricsAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsAppender.class);

    /**
     * Collection of gauges configured in logback.xml
     */
    private Map<String, Metric> gauges = new HashMap<>();

    /**
     * Collection of counters configured in logback.xml
     */
    private Map<String, Metric> counters = new HashMap<>();

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
     * StatsdConfig from logback.xml
     */
    private StatsdConfig statsdConfig;

    /**
     *
     * @return
     */
    public Map<String, Metric> getGauges() { return gauges; }

    /**
     *
     * @param gauge
     */
    public void addGauge(Metric gauge) {
        if(gauge.getLogsMsg() != null) {
            gauges.put(gauge.getLogsMsg(), gauge);
        }
        else if (gauge.getMarkerKey() != null) {
            gauges.put(gauge.getMarkerKey(), gauge);
        }
        else {
            LOG.error("Invalid gauge configuration - markerKey field is required");
        }
    }

    /**
     *
     * @return
     */
    public Map<String, Metric> getCounters() { return counters; }

    /**
     *
     * @param counter
     */
    public void addCounter(Metric counter) {
        if(counter.getLogsMsg() != null) {
            counters.put(counter.getLogsMsg(), counter);
        }
        else if(counter.getMarkerKey() != null) {
            counters.put(counter.getMarkerKey(), counter);
        }
        else {
            LOG.error("Invalid counter configuration - either a logMsg or markerKey field is required.");
        }
    }

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
     *
     * @return StatsdConfig
     */
    public StatsdConfig getStatsdConfig() { return statsdConfig; }

    /**
     *
     * @param statsdConfig
     */
    public void setStatsdConfig(StatsdConfig statsdConfig)
    {
        this.statsdConfig = statsdConfig;
        configs.add(this.statsdConfig);
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
        if(this.registries.isEmpty() || this.registries.size() != getConfigs().size()) {
            for(MeterRegistryConfig config : getConfigs()) {
                MeterRegistry r = MeterRegistryFactory.createInstance(config);
                if(r != null) this.registries.add(r);
            }
        }
        return this.registries;
    }

    /**
     * {@inheritDoc}
     *
     * TODO: Currently for gauges, this method requires a Map to be included in a LogstashMarker.
     *  This method should also support a Map that's passed in as an argument to the logger.
     *  ie. logger.info("some message", metricMap); It can be obtained by event.getArgumentArray().
     *  This will provide an option that doesn't require client's to use the LogstashMarker.
     */
    @Override
    protected void append(final ILoggingEvent event) {
        Marker marker = event.getMarker();
        Map<String, Object> markerMap = convertMarkerToMap(marker);
        String logMsg = transformMessage(event.getMessage());



        //checks metric config to see if logMsg was used for a counter
        if(getCounters().containsKey(logMsg)) {
            Metric m = getCounters().get(logMsg);
            if(markerMap.containsKey("tags")) m.addTags((Map<String, String>)markerMap.get("tags"));
            publishCounterMetric(m);
        }
        //checks metric config to see if logMsg was used for a gauge
        if(getGauges().containsKey(logMsg)) {
            Metric m = getGauges().get(logMsg);
            if(markerMap.containsKey("tags")) m.addTags((Map<String, String>)markerMap.get("tags"));
            publishGaugeMetric(m, Integer.valueOf(String.valueOf(markerMap.get(m.getMarkerKey()))));
        }
        for(String key : markerMap.keySet()) {
            //checks each key to see if it contains the prefix
            if(key.contains(getCounterPrefix())) {
                Metric m = new Metric();
                m.setMetricName(String.valueOf(markerMap.get(key)));
                if(markerMap.containsKey("tags")) m.addTags((Map<String, String>)markerMap.get("tags"));
                publishCounterMetric(m);
            }
            else if(key.contains(getGaugePrefix())) {
                Metric m = new Metric();
                m.setMarkerKey(key.replace(getGaugePrefix(), ""));
                if(markerMap.containsKey("tags")) m.addTags((Map<String, String>)markerMap.get("tags"));
                publishGaugeMetric(m, Integer.valueOf(String.valueOf(markerMap.get(key))));
            }
            //checks to see if the marker key was used in the metric config (requires logMsg to be absent for this metric).
            if(getCounters().containsKey(key)) {
                if(markerMap.containsKey("tags")) getCounters().get(key).addTags((Map<String, String>)markerMap.get("tags"));
                publishCounterMetric(getCounters().get(key));
            }
            if(getGauges().containsKey(key)) {
                if(markerMap.containsKey("tags")) getGauges().get(key).addTags((Map<String, String>)markerMap.get("tags"));
                publishGaugeMetric(getGauges().get(key), Integer.valueOf(String.valueOf(markerMap.get(key))));
            }
        }
    }

    /**
     * Converts the Logstash Marker into a HashMap<String, Object>
     * @param marker
     * @return Map
     */
    public static Map<String, Object> convertMarkerToMap(Marker marker) {
        if(marker != null) {
            try {
                MarkerMapGenerator generator = new MarkerMapGenerator();
                ((LogstashMarker)marker).writeTo(generator);
                return generator.getMap();
            } catch(Exception e) {
                LOG.error("Error converting marker to map", e);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Publishes the counter to all meter registries
     * @param counter Metric
     */
    private void publishCounterMetric(Metric counter) {
        for(MeterRegistry registry : getRegistries()) {
            Counter registryCounter;
            if(!counter.getTags().isEmpty()) {
                LOG.debug(String.format("publishing counter %s %s", counter.getName(getAppPrefix()), counter.getMicroMeterTags()));
                registryCounter = registry.counter(counter.getName(getAppPrefix()), counter.getMicroMeterTags());
            }
            else {
                LOG.debug(String.format("publishing counter %s", counter.getName(getAppPrefix())));
                registryCounter = registry.counter(counter.getName(getAppPrefix()));
            }
            registryCounter.increment();
            LOG.debug(String.format("registry %s has %d meters",
                        registry.getClass().getTypeName(),
                        registry.getMeters().size()));
        }
    }

    /**
     * Publishes the gauge to all meter registries
     * @param gauge Metric
     * @param value int
     */
    private void publishGaugeMetric(Metric gauge, int value) {
        for(MeterRegistry registry : getRegistries()) {
            if(!gauge.getTags().isEmpty()) {
                LOG.debug("publishing gauge %s %s %n", gauge.getName(getAppPrefix()), gauge.getMicroMeterTags().toArray(), new AtomicInteger(value));
                registry.gauge(gauge.getName(getAppPrefix()), gauge.getMicroMeterTags(), new AtomicInteger(value));
            }
            else {
                LOG.debug(String.format("publishing gauge: %s %n", gauge.getName(getAppPrefix()), new AtomicInteger(value)));
                registry.gauge(gauge.getName(getAppPrefix()), new AtomicInteger(value));
            }
            LOG.debug(String.format("registry %s has %d meters",
                        registry.getClass().getTypeName(),
                        registry.getMeters().size()));
        }
    }

    /**
     * Reduces the log message to a lower-case alpha String that's valid as a metric key in
     *  the target monitoring systems.
     * @param msg
     * @return String
     */
    public static String transformMessage(String msg) {
        return msg.replaceAll(" ", "_")
                    .replaceAll("[^a-zA-Z_]", "").toLowerCase();
    }

    /**
     * Simple metric def for config injection
     */
    public static class Metric {
        private String logMsg;
        private String markerKey;
        private String metricName;
        private List<Tag> tags = new ArrayList<>();
        private List<io.micrometer.core.instrument.Tag> microMeterTags = new ArrayList<>();

        public String getLogsMsg() { return logMsg; }
        public void setLogMsg(String logMsg) { this.logMsg = logMsg; }

        public String getMarkerKey() { return markerKey; }
        public void setMarkerKey(String markerKey) { this.markerKey = markerKey; }

        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }

        public void addTag(Tag tag) { tags.add(tag); }
        public List<Tag> getTags() { return tags; }

        public void addTags(Map<String, String> tags) {
            for(String t : tags.keySet()) {
                Tag tag = new Tag();
                tag.setKey(t);
                tag.setValue(tags.get(t));
                addTag(tag);
                microMeterTags.add(new ImmutableTag(t, tags.get(t)));
            }
        }

        public List<io.micrometer.core.instrument.Tag> getMicroMeterTags(){
            return microMeterTags;
        }

        public String getName(String prefix) {
            if(getMetricName() != null) {
                prefix += getMetricName();
            }
            else if(getMarkerKey() != null) {
                prefix += getMarkerKey();
            }
            else if(getLogsMsg() != null) {
                prefix += getLogsMsg();
            }
            return prefix;
        }
    }

    /**
     * Simple tag def for config injection
     */
    public static class Tag  {
        private String key;
        private String value;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
