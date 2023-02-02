package com.testing.entity;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class BenchmarkRequest {
	
	private String benchmark;
	private String projectId;
	private String cloud;
	private String config;
	private String userId;
	Map<Object, Object> details = new LinkedHashMap<>();

    @JsonAnySetter
    void setDetails(Object key, Object value) {
        details.put(key, value);
    }
    
    public Map<Object, Object> getDetails() {
    	return details;
    }

	public String getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(String benchmark) {
		this.benchmark = benchmark;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getCloud() {
		return cloud;
	}

	public void setCloud(String cloud) {
		this.cloud = cloud;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}