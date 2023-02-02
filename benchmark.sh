#!/bin/sh
set -e
gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS

while [ $# -gt 0 ]; do
  case "$1" in
    --benchmarks=*)
      benchmarks="${1#*=}"
      ;;
    --project=*)
      project="${1#*=}"
      ;;
    --cloud=*)
      cloud="${1#*=}"
      ;;
    --benchmark_config_file=*)
      benchmark_config_file="${1#*=}"
      ;;
    --bq_project=*)
      bq_project="${1#*=}"
      ;;
    --bigquery_table=*)
      bigquery_table="${1#*=}"
      ;;
    --metadata=*)
      metadata="${1#*=}"
      ;;
    *)
      printf "***************************\n"
      printf "* Error: Invalid argument.*\n"
      printf "***************************\n"
      exit 1
  esac
  shift
done

/tmp/pkb/PerfKitBenchmarker/pkb.py \
	--benchmarks=$benchmarks \
	--project=$project \
	--cloud=$cloud \
	--benchmark_config_file=$benchmark_config_file \
	--bq_project=$project \
	--bigquery_table=perfkit.results \
        --metadata=$metadata
