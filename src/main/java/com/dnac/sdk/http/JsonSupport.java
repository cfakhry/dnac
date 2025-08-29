// com/dnac/sdk/http/JsonSupport.java
package com.dnac.sdk.http;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonSupport {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public <T> T read(String json, Class<T> type) throws Exception { return mapper.readValue(json, type); }
    public String write(Object o) throws Exception { return mapper.writeValueAsString(o); }
    public ObjectMapper mapper() { return mapper; }
}