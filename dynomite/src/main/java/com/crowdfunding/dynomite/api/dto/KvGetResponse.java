package com.crowdfunding.dynomite.api.dto;

public class KvGetResponse {
    private String key;
    private String value;
    private String servedBy;

    public KvGetResponse(String key, String value, String servedBy) {
        this.key = key;
        this.value = value;
        this.servedBy = servedBy;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getServedBy() {
        return servedBy;
    }
}

