package com.dnac.sdk.model.site;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Site {
    public String id;
    public String name;
    public String instanceTenantId;
    public String siteHierarchy;
    public String siteNameHierarchy;

    // Arbitrary metadata; DNAC can return heterogeneous objects here
    public List<Object> additionalInfo;
}