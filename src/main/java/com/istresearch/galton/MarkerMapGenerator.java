package com.istresearch.galton;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Quick and dirty implementation to build and extract a hashmap
 *  from the generator.
 */
public class MarkerMapGenerator extends JsonGeneratorDelegate {

    private Map<String, Object> map = new HashMap<>();

    private String lastFieldName;

    /**
     * Constructor override
     * @throws Exception
     */
    public MarkerMapGenerator() throws Exception {
        super(new JsonFactory().createGenerator(new StringWriter()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFieldName(String name) throws IOException {
        map.put(name, null);
        lastFieldName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object pojo) throws IOException, JsonProcessingException {
        map.put(lastFieldName, pojo);
    }

    /**
     *
     * @return Map
     */
    public Map<String, Object> getMap() { return map; }

}
