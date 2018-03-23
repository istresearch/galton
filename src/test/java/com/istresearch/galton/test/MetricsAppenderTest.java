package com.istresearch.galton.test;

import com.istresearch.galton.MarkerMapGenerator;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

/**
 * TODO: Use a mocking framework for unit tests
 * TODO: Use a test appender to allow for asserting logger output
 */

public class MetricsAppenderTest {

    private static final Logger logger = LoggerFactory.getLogger(MetricsAppenderTest.class);

    @Test public void testConvertMarkerToMap() {
        Map<String, String> tags = new HashMap<String, String>() {{
            put("tagKey", "tagValue");
            put("tagKey2", "tagValue2");
        }};
        Map<String, Object> markers = new HashMap<String, Object>() {{
            put("galton.counter", "count_this");
            put("rule_type", "aType");
            put("galton.gauge.rules_pending", 293);
            put("tags", tags);
        }};
        try {
            Marker marker = Markers.appendEntries(markers);
            MarkerMapGenerator generator = new MarkerMapGenerator();
            ((LogstashMarker)marker).writeTo(generator);
            assertEquals(generator.getMap(), markers);
        } catch(Exception e) {
            logger.error("Exception occurred", e);
        }
    }

    /**
     * Below are config-based counter tests
     */

    /**
     * Produces metric named test_counter_metric_name1 with tags:
     *  - test_config_counter_dynamic_tag_key:test_config_counter_dynamic_tag_value
     *  - test_config_counter_dynamic_tag_key2:test_config_counter_dynamic_tag_value2
     */
    @Test public void testMetricsAppenderConfigCounterAndTags() {
        /*   <counter>
                 <logMsg>test_counter_log_msg1</logMsg>
                 <metricName>test_counter_metric_name1</metricName>
                 <tag>
                     <key>test_counter_static_tag_key</key>
                     <value>test_counter_static_tag_value</value>
                 </tag>
             </counter> */
        int i = 0;
        while (i < 5) {
            Map<String, String> tags = new HashMap<String, String>() {{
                put("test_config_counter_dynamic_tag_key", "test_config_counter_dynamic_tag_value");
                put("test_config_counter_dynamic_tag_key2", "test_config_counter_dynamic_tag_value2");
            }};
            Map<String, Object> marker = new HashMap<String, Object>() {{
                put("tags", tags);
            }};
            logger.info(Markers.appendEntries(marker), "test counter log msg1");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_counter_marker_key2
     */
    @Test public void testMetricsAppenderConfigCounterMarkerOnly() {
        /* <counter>
                 <markerKey>test_counter_marker_key2</markerKey>
             </counter> */
        int i = 0;
        while (i < 5) {
            Map<String, Object> markers = new HashMap<String, Object>() {{
                put("test_counter_marker_key2", "some_value");
            }};
            logger.info(Markers.appendEntries(markers), "galton should recognize this marker and increment its count");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_counter_log_msg2
     */
    @Test public void testMetricsAppenderConfigCounterLogMsgOnly() {
        /*   <counter>
                 <logMsg>test_counter_log_msg2</logMsg>
             </counter> */
        int i = 0;
        while (i < 5) {
            logger.info("test counter log msg2");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_counter_metric_name1 with tags:
     *  - test_counter_static_tag_key:test_counter_static_tag_value
     */
    @Test public void testMetricsAppenderConfigCounter() {
        /*   <counter>
                 <logMsg>test_counter_log_msg3</logMsg>
                 <metricName>test_counter_metric_name1</metricName>
                 <tag>
                     <key>test_counter_static_tag_key</key>
                     <value>test_counter_static_tag_value</value>
                 </tag>
             </counter> */
        int i = 0;
        while (i < 5) {
            logger.info("test counter log msg3");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Below are code-based counter tests
     */

    /**
     * Produces metric named test_galton.counter_marker
     */
    @Test public void testMetricsAppenderCodeCounter() {
        int i = 0;
        while (i < 5) {
            Map<String, Object> markers = new HashMap<String, Object>() {{
                put("galton.counter", "test_galton.counter_marker");
            }};
            logger.info(Markers.appendEntries(markers), "this should increment metric test_galton.counter_marker");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_galton.counter_marker2 with tags:
     *  - test_code_counter_dynamic_tag_key:test_code_counter_dynamic_tag_value
     *  - test_code_counter_dynamic_tag_key2:test_code_counter_dynamic_tag_value2
     */
    @Test public void testMetricsAppenderCodeCounterAndTags() {
        int i = 0;
        while (i < 5) {
            Map<String, String> tags = new HashMap<String, String>() {{
                put("test_code_counter_dynamic_tag_key", "test_code_counter_dynamic_tag_value");
                put("test_code_counter_dynamic_tag_key2", "test_code_counter_dynamic_tag_value2");
            }};
            Map<String, Object> markers = new HashMap<String, Object>() {{
                put("galton.counter", "test_galton.counter_marker2");
                put("tags", tags);
            }};
            logger.info(Markers.appendEntries(markers), "this should increment metric test_galton.counter_marker2");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }


    /**
     * Below are config-based gauge tests
     */

    /**
     * Produces metric named test_gauge_metric_name1 with tags:
     *  - test_gauge_marker_key1:test_gauge_metric_name1
     *  - test_gauge_static_tag_key2:test_gauge_static_tag_value2
     */
    @Test public void testMetricsAppenderConfigGauge1() {
         /* <gauge>
                <logMsg>test_gauge_log_msg1</logMsg>
                <markerKey>test_gauge_marker_key1</markerKey>
                <metricName>test_gauge_metric_name1</metricName>
                <tag>
                    <key>test_gauge_static_tag_key</key>
                    <value>test_gauge_static_tag_value</value>
                </tag>
                <tag>
                    <key>test_gauge_static_tag_key2</key>
                    <value>test_gauge_static_tag_value2</value>
                </tag>
            </gauge> */
        int i = 0;
        while (i < 5) {
            Map<String, Object> marker = new HashMap<String, Object>() {{
                put("test_gauge_marker_key1", String.valueOf(ThreadLocalRandom.current().nextInt(0, 1000)));
            }};
            logger.info(Markers.appendEntries(marker), "test gauge log msg1");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_gauge_marker_key2
     */
    @Test public void testMetricsAppenderConfigGauge2() {
         /* <gauge>
                <logMsg>test_gauge_log_msg2</logMsg>
                <markerKey>test_gauge_marker_key2</markerKey>
            </gauge> */
        int i = 0;
        while (i < 5) {
            Map<String, Object> marker = new HashMap<String, Object>() {{
                put("test_gauge_marker_key2", String.valueOf(ThreadLocalRandom.current().nextInt(0, 1000)));
            }};
            logger.info(Markers.appendEntries(marker), "test gauge log msg2");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Below are code-based gauge tests
     */

    /**
     * Produces metric named test_gauge_with_prefix
     */
    @Test public void testMetricsAppenderCodeGauge() {
        int i = 0;
        while (i < 5) {

            Map<String, Object> markers = new HashMap<String, Object>() {{
                put("galton.gauge.test_gauge_with_prefix", String.valueOf(ThreadLocalRandom.current().nextInt(0, 1000)));
            }};
            logger.info(Markers.appendEntries(markers), "This should publish a value for test_gauge_with_prefix");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

    /**
     * Produces metric named test_gauge_with_prefix2 with tags:
     *  - test_gauge_dynamic_tag_key:test_gauge_dynamic_tag_value
     *  - test_gauge_dynamic_tag_key2:test_gauge_dynamic_tag_value2
     */
    @Test public void testMetricsAppenderCodeGaugeAndTags() {
        int i = 0;
        while (i < 5) {
            Map<String, String> tags = new HashMap<String, String>() {{
                put("test_gauge_dynamic_tag_key", "test_gauge_dynamic_tag_value");
                put("test_gauge_dynamic_tag_key2", "test_gauge_dynamic_tag_value2");
            }};
            Map<String, Object> markers = new HashMap<String, Object>() {{
                put("galton.gauge.test_gauge_with_prefix2", String.valueOf(ThreadLocalRandom.current().nextInt(0, 1000)));
                put("tags", tags);
            }};
            logger.info(Markers.appendEntries(markers), "This should publish a value for test_gauge_with_prefix2");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
            i++;
        }
    }

}