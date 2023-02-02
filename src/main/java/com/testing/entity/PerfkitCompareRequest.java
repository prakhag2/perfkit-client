package com.testing.entity;

import java.util.List;

public class PerfkitCompareRequest {
	
	private String benchmark;
	private List<String> instances;

	public String getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(String benchmark) {
		this.benchmark = benchmark;
	}

	public List<String> getInstances() {
		return instances;
	}

	public void setInstances(List<String> instances) {
		this.instances = instances;
	}
}