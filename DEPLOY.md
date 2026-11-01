# Deploying to AWS EMR

logscope runs unchanged on a multi-node Spark cluster. Validated on EMR
(2× m5.xlarge, Spark 3.5, reading input and writing the anomaly report to S3).

## Key detail: build for Scala 2.12
EMR's bundled Spark is compiled with **Scala 2.12**, so the cluster jar must be
2.12 (the local / standalone build is 2.13). The project cross-builds both:

```bash
sbt ++2.12.18 package      # -> target/scala-2.12/logscope_2.12-0.1.0-SNAPSHOT.jar
```

Submit the **thin** jar (your classes only); the cluster provides Spark.

## Steps
```bash
# 1. Upload thin jar + data to S3
aws s3 cp target/scala-2.12/logscope_2.12-0.1.0-SNAPSHOT.jar s3://<bucket>/jars/logscope.jar
aws s3 cp data/HDFS_v1/HDFS.log                              s3://<bucket>/data/HDFS.log
aws s3 cp data/HDFS_v1/preprocessed/anomaly_label.csv       s3://<bucket>/data/anomaly_label.csv

# 2. Launch a cluster
aws emr create-cluster --name logscope --release-label emr-7.9.0 \
  --applications Name=Spark --use-default-roles \
  --instance-type m5.xlarge --instance-count 2 \
  --log-uri s3://<bucket>/logs/ --region <region>

# 3. Submit the job (reads + writes S3)
aws emr add-steps --cluster-id <id> --region <region> \
  --steps Type=Spark,Name=logscope,Args=[--class,Main,s3://<bucket>/jars/logscope.jar,s3://<bucket>/data/HDFS.log,s3://<bucket>/data/anomaly_label.csv,s3://<bucket>/output]

# 4. ALWAYS terminate when done
aws emr terminate-clusters --cluster-ids <id> --region <region>
```

## Result (2× m5.xlarge)
Full pipeline end to end in ~7 min: 11.2M log lines from S3 → 575,061 block feature
vectors → supervised detection, anomaly report written to S3. F1 ≈ 0.99 on the
held-out split.