package com.testing.entity;

import java.util.List;

public class ResultResponse {
	private List<String> rows;
	private String jobId;

	public List<String> getRows() {
		return rows;
	}

	public void setRows(List<String> rows) {
		this.rows = rows;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
}