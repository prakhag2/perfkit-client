# perfkit-client
This is the Spring Boot based client for the [Perfkit fork](https://github.com/prakhag2/PerfKitBenchmarker). 
"benchmark.sh" does the actual run, while rest of the interfaces are created to expose perfkit over easy to use REST APIs (and any potential UI integrations).
The setup is tested on GCP.

In order to run this setup, you will need the following:

1. A MySQL database 
2. An dataset in BQ by the name "perfkit" and an empty table inside "perfkit" by the name "results"

Steps to run:

1. Build a docker image and run:
docker run -p 8081:8081 -v <GCP service-account-json key file>:/tmp/creds.json:ro \
	-e GOOGLE_APPLICATION_CREDENTIALS=/tmp/creds.json \
	-e DB_HOST=MySQL IP \
	-e DB_USER=User \
	-e DB_PASSWORD=Password docker-image

2. To run a benchmark, the equivalent curl:
curl --location --request POST 'ip-where-docker-image-is-running:8080/runbenchmark' \
--header 'Content-Type: application/json' \
--header 'Content-Encoding: gzip' \
--data-raw '{
	"benchmark": "aerospike",
    "projectId": <GCP-project-id>,
    "cloud": "GCP",
    "userId": "1",
    "config": "{\"aerospike\":{\"description\":\"RunsAerospike.\",\"flags\":{\"aerospike_benchmark_duration\":60,\"aerospike_client_threads_step_size\":8,\"aerospike_instances\":1,\"aerospike_max_client_threads\":128,\"aerospike_min_client_threads\":8,\"aerospike_num_keys\":1000000,\"aerospike_publish_detailed_samples\":false,\"aerospike_read_percent\":90,\"act_dynamic_load\":false,\"act_stop_on_complete\":true,\"aerospike_replication_factor\":1,\"aerospike_service_threads\":4,\"aerospike_storage_type\":\"memory\"},\"vm_groups\":{\"workers\":{\"vm_spec\":{\"GCP\":{\"machine_type\":\"n1-standard-1\",\"zone\":\"us-central1-b\",\"image\":\"projects/ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20220927\",\"boot_disk_size\":50}},\"vm_count\":1,\"disk_spec\":{\"GCP\":{\"disk_size\":50,\"disk_type\":\"pd-ssd\"}}},\"clients\":{\"vm_spec\":{\"GCP\":{\"machine_type\":\"n1-standard-1\",\"zone\":\"us-central1-b\",\"image\":\"projects/ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20220927\",\"boot_disk_size\":50}},\"vm_count\":1,\"disk_spec\":{\"GCP\":{\"disk_size\":50,\"disk_type\":\"pd-ssd\"}}}}}}"
}'

Sample config files are present in "config/".

3. When a job is run, it will generate a job-id. To fetch its state, run
curl --location --request POST 'ip-where-docker-image-is-running:8080/getstate' \
--header 'Content-Type: application/json' \
--header 'Content-Encoding: gzip' \
--data-raw '{
	"jobId": "67bacde1-0bf3-4469-bd8f-186c04aab40d" --> replace with the generated job-id
}'

4. For a running job, get the logs
curl --location --request POST 'ip-where-docker-image-is-running:8080/getlogs' \
--header 'Content-Type: application/json' \
--header 'Content-Encoding: gzip' \
--data-raw '{
	"jobId": "67bacde1-0bf3-4469-bd8f-186c04aab40d",
	"offset": "0"
}'

5. Once the job finishes, its results are exported in BQ. To fetch the results, run:
curl --location --request POST 'ip-where-docker-image-is-running:8080/getresult' \
--header 'Content-Type: application/json' \
--header 'Content-Encoding: gzip' \
--data-raw '{
	"jobId": "67bacde1-0bf3-4469-bd8f-186c04aab40d"
}'
