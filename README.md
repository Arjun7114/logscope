# logscope

Distributed log anomaly detection built with Scala and Apache Spark.

Processes the full HDFS log dataset — millions of raw log lines with
block-level anomaly labels — end to end: parsing, template mining,
feature engineering, and anomaly scoring, all distributed via Spark.

This is a larger-scale counterpart to an earlier single-node experiment
that ran on a small sample. The goal is to process the entire dataset
and honestly measure how it behaves at scale.

Status: work in progress, built step by step.

Stack: Scala 2.13 · Apache Spark · sbt