package com.istresearch.galton;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.statsd.StatsdMeterRegistry;

/**
 * Simple factory class that expects a Config (injected in Logback.xml)
 *  and returns the MeterRegistry associated with it.
 */
public class MeterRegistryFactory {

    /**
     *
     * @param config
     * @return MeterRegistry
     */
    public static MeterRegistry createInstance(MeterRegistryConfig config) {
        if(config instanceof DatadogConfig) {
            return new DatadogMeterRegistry((DatadogConfig)config, Clock.SYSTEM);
        }
        else if(config instanceof DogStatsdConfig){
            return new StatsdMeterRegistry((DogStatsdConfig)config, Clock.SYSTEM);
        }
        return null;
    }

}
