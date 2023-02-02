package com.testing.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.testing.entity.BenchmarkRequest;
import com.testing.entity.PerfkitCompareRequest;
import com.testing.model.User;
import com.testing.repository.JobRepository;
import com.testing.repository.UserRepository;

@Service
public class PerfkitService {
	@Autowired JobRepository jobRepository;
	@Autowired UserRepository userRepository;
	@Autowired private StorageProvider storageProvider;
	@Autowired private JobScheduler jobScheduler;
	@Autowired private BigQuery bigQueryClient;
	
	private final String perfkitDir = "/tmp";
	private String configFile = "/tmp/%s/%s.yml";
	private String logFile = "/tmp/%s/log";
	
	public JobId enqueueTest(BenchmarkRequest request, JsonNode jsonNodeTree) {
		return jobScheduler.enqueue(() -> execute(request, jsonNodeTree, JobContext.Null));		
	}

	@Job
	public void execute(BenchmarkRequest request, JsonNode jsonNodeTree, JobContext ctx) throws Exception {
		System.out.println("Inside execute for: "+ctx.getJobId().toString());
		String benchmark = request.getBenchmark();
		String projectId = request.getProjectId();
		String cloud = request.getCloud();
		UUID jobId = ctx.getJobId();
		try {
			Files.createDirectories(Paths.get(perfkitDir+"/"+jobId));
			System.out.println("Dir created");
			new YAMLMapper().writeValue(new File(String.format(configFile, jobId, benchmark)), jsonNodeTree);
			System.out.println("Config created");
	
			String[] cmd = {"sh", "-c", "./benchmark.sh "
					+ " --benchmarks=" + benchmark 
					+ " --project=" + projectId 
					+ " --cloud=" + cloud 
					+ " --benchmark_config_file=" + String.format(configFile, jobId, benchmark)
					+ " --bq_project=" + projectId
					+ " --bigquery_table=perfkit.results"
					+ " --metadata=id:" + jobId
					};
	
			ProcessBuilder probuilder = new ProcessBuilder(cmd)
					.redirectError(new File(String.format(logFile, jobId)))
					.redirectOutput(new File(String.format(logFile, jobId)));
			probuilder.directory(new File(perfkitDir));
			Process process = probuilder.start();
			process.waitFor();
			process.destroy();		
		} catch (Exception e) {
			//jobScheduler.delete(jobId, e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	public void saveJob(BenchmarkRequest request, String jobId) throws Exception {
		try {
			com.testing.model.Job job = new com.testing.model.Job();
			job.setJobId(jobId);
			job.setBenchmark(request.getBenchmark());
			job.setConfig(request.getConfig());
			job.setTimestamp(Date.from(Instant.now()));

			Optional<User> user = userRepository.findById(request.getUserId());
			if (!user.isPresent()) {
				user = Optional.of(new User());
				user.get().setUserId(request.getUserId());
			}
			job.setUser(user.get());
			Set<com.testing.model.Job> jobs = new HashSet<com.testing.model.Job>();
			jobs.add(job);
			user.get().setJobs(jobs);

			jobRepository.save(job);
		} catch (Exception e) {
			//jobScheduler.delete(UUID.fromString(jobId), e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	public Set<com.testing.model.Job> getJobsForUserId(String userId) {
		Optional<User> user = userRepository.findById(userId);
		if (user.isPresent()) {
			return user.get().getJobs();
		}
		return Collections.<com.testing.model.Job>emptySet();
	}
	
	public String getStateForJobId(String jobId) throws JobException, InterruptedException {
		String state = "";
		org.jobrunr.jobs.Job job = storageProvider.getJobById(UUID.fromString(jobId));
		state = job.getJobState().getName().toString();

		// Background job completed, but did perfkit finish successfully?
		// TBD - think of a better way. This is too expensive!
		if (state.equalsIgnoreCase("SUCCEEDED")) {
			List<String> jobResult = getResultForJobId(jobId);
			if (jobResult.isEmpty()) {
				state = "FAILED";
			}
		}
		return state;
	}
	
	public List<String> getResultForJobId(String jobId) throws JobException, InterruptedException {
		List<String> rows = new ArrayList<>();
		String query =  "WITH\n"
				+ "  PERFKITRESULTS AS (\n"
				+ "  SELECT\n"
				+ "    metric,\n"
				+ "    value,\n"
				+ "    unit,\n"
				+ "    test,\n"
				+ "    labels\n"
				+ "  FROM\n"
				+ "  `" + bigQueryClient.getOptions().getProjectId() + ".perfkit.results` t\n"
				+ "  WHERE\n"
				+ "    REGEXP_CONTAINS(LOWER(TO_JSON_STRING(t)), '" + jobId + "')\n"
				+ "    AND metric != \"lscpu\"\n"
				+ "    AND metric != \"gcc_version\"\n"
				+ "    AND metric != \"glibc_version\"\n"
				+ "    AND metric != \"proccpu_mapping\"\n"
				+ "    AND metric != \"proccpu\"\n"
				+ "    AND metric != \"cpu_vuln\"\n"
				+ "    ),\n"
				+ "  AGGREGATEDRESULT AS (\n"
				+ "  SELECT\n"
				+ "    metric,\n"
				+ "    unit,\n"
				+ "    test,\n"
				+ "    ARRAY_AGG ( STRUCT(value,labels) order by value ) AS value_labels\n"
				+ "  FROM\n"
				+ "    PERFKITRESULTS AS t\n"
				+ "  GROUP BY\n"
				+ "    metric,\n"
				+ "    unit,\n"
				+ "    test )\n"
				+ "SELECT\n"
				+ "  TO_JSON_STRING(t) AS result\n"
				+ "FROM\n"
				+ "  AGGREGATEDRESULT AS t";
				    
		 QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
		 TableResult results = bigQueryClient.query(queryConfig);
		 for (FieldValueList row : results.iterateAll()) {
		    rows.add(row.get(0).getValue().toString());
		 }
		return rows;
	}
		
	public List<String> getPerfkitCompareResults(PerfkitCompareRequest request) throws JobException, InterruptedException {
		String benchmark = request.getBenchmark();
		List<String> rows = new ArrayList<>();
		List<String> list = request.getInstances();
		list.replaceAll(e -> '"' + e + '"');
				
		String query = "DECLARE\n"
				+ "  i INT64 DEFAULT 1;\n"
				+ "DECLARE\n"
				+ "  dsql STRING DEFAULT '';\n"
				+ "DECLARE\n"
				+ "  INSTANCES ARRAY<STRING>;\n"
				+ "SET\n"
				+ "  INSTANCES = " + list +";\n"
				+ "WHILE\n"
				+ "  i <= ARRAY_LENGTH(INSTANCES) DO\n"
				+ "SET\n"
				+ "  dsql = dsql || \"\"\"\n"
				+ "    (WITH PERFKITRESULTS AS \n"
				+ "      (SELECT '\"\"\" || INSTANCES[ORDINAL(i)] || \"\"\"' AS instance, \n"
				+ "      DATE(TIMESTAMP_SECONDS(CAST(timestamp AS INT64))) AS date, \n"
				+ "      metric, \n"
				+ "      value, \n"
				+ "      unit, \n"
				+ "      test,\n"
				+ "      labels \n"
				+ "      FROM `" + bigQueryClient.getOptions().getProjectId() +".perfkit.results` t \n"
				+ "      WHERE test = \\\"" + benchmark + "\\\" \n"
				+ "      AND REGEXP_CONTAINS(LOWER(TO_JSON_STRING(t)),'\"\"\" || INSTANCES[ORDINAL(i)] || \"\"\"') \n"
				+ "      AND NOT REGEXP_CONTAINS(LOWER(TO_JSON_STRING(t)), '\\\"metadata=id:\\\"')\n"
				+ "      AND metric != \\\"lscpu\\\" \n"
				+ "      AND metric != \\\"gcc_version\\\" \n"
				+ "      AND metric != \\\"glibc_version\\\" \n"
				+ "      AND metric != \\\"proccpu_mapping\\\" \n"
				+ "      AND metric != \\\"proccpu\\\" \n"
				+ "      AND metric != \\\"cpu_vuln\\\"\n"
				+ "      ), \n"
				+ "    AGGREGATEDRESULT AS \n"
				+ "      (SELECT instance, \n"
				+ "      date, \n"
				+ "      metric, \n"
				+ "      unit, \n"
				+ "      test, \n"
				+ "      ARRAY_AGG( STRUCT(value, labels) order by value ) AS value_label \n"
				+ "      FROM PERFKITRESULTS AS t \n"
				+ "      GROUP BY \n"
				+ "      instance, \n"
				+ "      date, \n"
				+ "      metric, \n"
				+ "      unit, \n"
				+ "      test),\n"
				+ "    RES as (\n"
				+ "      SELECT instance,\n"
				+ "      ARRAY_AGG( STRUCT(date, metric, unit, test, value_label) ) as result\n"
				+ "      FROM AGGREGATEDRESULT\n"
				+ "      WHERE date = (SELECT MAX(date) FROM AGGREGATEDRESULT)\n"
				+ "      GROUP BY instance\n"
				+ "    )\n"
				+ "    SELECT \n"
				+ "    TO_JSON_STRING(t) AS result \n"
				+ "    FROM RES AS t \n"
				+ "    )\n"
				+ "    union all \"\"\";\n"
				+ "SET\n"
				+ "  i = i + 1;\n"
				+ "END WHILE\n"
				+ "  ;\n"
				+ "SET\n"
				+ "  dsql = SUBSTR(dsql, 1, LENGTH(dsql) - LENGTH(' union all'));\n"
				+ "EXECUTE IMMEDIATE\n"
				+ "  dsql";
		
		 QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
		 TableResult results = bigQueryClient.query(queryConfig);
		 for (FieldValueList row : results.iterateAll()) {
		    rows.add(row.get(0).getValue().toString());
		 }
		return rows;
	}

	public Map<String, Object> getLogsForJobId(String jobId, long offset) throws FileNotFoundException, IOException {
		Map<String, Object> map = new HashMap<>();
		long endPos = 0L;
		List<String> logs = new ArrayList<String>();
		try (RandomAccessFile randFile = new RandomAccessFile(String.format(logFile, jobId), "r")) {
			endPos = randFile.length();
			randFile.seek(offset);
			String currentLine = "";
			while ((currentLine = randFile.readLine()) != null) {
				logs.add(currentLine);
			}
		}
		map.put("offset", endPos);
		map.put("logs", logs);
		return map;
	}
}
