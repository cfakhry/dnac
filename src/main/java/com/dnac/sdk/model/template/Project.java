package com.dnac.sdk.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    public String id;
    public String name;
    public String description;
    public Long   lastUpdateTime;
    public Boolean isDeletable;
    public List<Template> templates;
}