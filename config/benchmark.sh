#!/bin/bash

benchmarks=(
        fio coremark
        multichase iperf cluster_boot
        nginx redis_memtier apachebench tomcat_wrk
        pgbench sysbench mongodb_ycsb
        cassandra_stress aerospike kubernetes_nginx
)

for benchmark in ${benchmarks[@]}; do
../PerfKitBenchmarker/pkb.py  \
        --benchmarks=$benchmark \
        --project=<Project> \
        --cloud=GCP \
        --benchmark_config_file=./$benchmark.yml \
        --bigquery_table=perfkit.results
done
