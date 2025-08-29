package com.dnac.sdk.model.site;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteListResponse {
    public List<Site> response;
    public String version;
}