package com.dnac.sdk.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateProjectRequest {
    public String name;            // required
    public String description;     // optional
    public List<Tag> tags;         // optional

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        public String name;
        public String id;          // optional
    }
}