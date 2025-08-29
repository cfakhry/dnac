package com.dnac.sdk.model.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {
    public String id;
    public String name;
    public String language;
    public Boolean composite;
    public Boolean customParamsOrder;
    public Long lastUpdateTime;
    public Long latestVersionTime;
    public String projectName;
    public String projectId;
    public Integer noOfConflicts;
    public Boolean projectAssociated;
    public Boolean documentDatabase;
}