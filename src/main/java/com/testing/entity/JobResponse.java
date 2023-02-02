package com.testing.entity;

import java.util.Date;

public class JobResponse { 
	private String benchmark;
	private String config;
	private String jobId;
	private Date timeStamp;
	
//  Derived from jobrunr tables	
//	private StateResponse state;
	
//  Derived from bigquery
//	private ResultResponse result;
	
//  Extracted from the shared file-system
//	private LogsResponse logs;
			
	public String getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(String benchmark) {
		this.benchmark = benchmark;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
}
