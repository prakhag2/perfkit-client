package com.testing.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jobrunr.jobs.JobId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testing.entity.BenchmarkRequest;
import com.testing.entity.BenchmarkResponse;
import com.testing.entity.JobResponse;
import com.testing.entity.JobsRequest;
import com.testing.entity.LogsRequest;
import com.testing.entity.LogsResponse;
import com.testing.entity.PerfkitCompareRequest;
import com.testing.entity.ResultRequest;
import com.testing.entity.ResultResponse;
import com.testing.entity.StateRequest;
import com.testing.entity.StateResponse;
import com.testing.exceptionhandler.PerfkitException;
import com.testing.model.Job;
import com.testing.service.PerfkitService;

@RestController
public class PerfkitController {
	@Autowired private PerfkitService perfkitService;

	@CrossOrigin
	@RequestMapping(value = "/healthcheck", method = RequestMethod.GET)
	public ResponseEntity<?> healthCheck() {
		return new ResponseEntity<Object>("", HttpStatus.OK);
	}

	@CrossOrigin
	@RequestMapping(value = "/runbenchmark", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> runBenchmark(@RequestBody BenchmarkRequest request) {
		try {
			JsonNode jsonNodeTree = new ObjectMapper().readTree(request.getConfig());
			JobId jobId = perfkitService.enqueueTest(request, jsonNodeTree);
			System.out.println("Job id created: "+jobId.toString());
			perfkitService.saveJob(request, jobId.toString());
			System.out.println("Job id: "+jobId.toString()+" saved");
	
			BenchmarkResponse response = new BenchmarkResponse();
			response.setJobId(jobId.asUUID().toString());
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/getstate", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> getState(@RequestBody StateRequest request) {
		try {
			String jobId = request.getJobId();
			String state = perfkitService.getStateForJobId(request.getJobId());
			
			StateResponse response = new StateResponse();
			response.setState(state);
			response.setJobId(jobId);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}
	}
	
	@CrossOrigin
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/getlogs", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> getLogs(@RequestBody LogsRequest request) {
		try {
			String jobId = request.getJobId();
			long offset = request.getOffset();
			Map<String, Object> map = perfkitService.getLogsForJobId(jobId, offset);
	
			LogsResponse response = new LogsResponse();
			response.setOffset((long) map.get("offset"));
			response.setLogs((List<String>) map.get("logs"));
			response.setJobId(jobId);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}

	}

	@CrossOrigin
	@RequestMapping(value = "/getresult", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> getResult(@RequestBody ResultRequest request) {
		try {
			String jobId = request.getJobId();
			List<String> rows = perfkitService.getResultForJobId(request.getJobId());
			
			ResultResponse response = new ResultResponse();
			response.setRows(rows);
			response.setJobId(jobId);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/perfkitcompare", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> getPerfkitCompare(@RequestBody PerfkitCompareRequest request) {
		try {
			List<String> rows = perfkitService.getPerfkitCompareResults(request);
			
			ResultResponse response = new ResultResponse();
			response.setRows(rows);
			return new ResponseEntity<Object>(response, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/getuserjobs", method = RequestMethod.POST, produces = { "application/json" })
	public ResponseEntity<?> getUserJobs(@RequestBody JobsRequest request) throws JsonProcessingException {
		try {
			String userId = request.getUserId();
			Set<Job> jobs = perfkitService.getJobsForUserId(userId);
			List<JobResponse> responses = new ArrayList<>();

			for (Job job : jobs) {
				JobResponse response = new JobResponse();
				response.setBenchmark(job.getBenchmark());
				response.setConfig(job.getConfig());
				response.setJobId(job.getJobId());
				response.setTimeStamp(job.getTimestamp());
				responses.add(response);
			}
			return new ResponseEntity<Object>(responses, HttpStatus.OK);
		} catch (Exception e) {
			throw new PerfkitException(e);
		}
	}
}
