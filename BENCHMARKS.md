# Benchmarks

Scaling measurement for the data-heavy stage of the pipeline: read → parse →
per-block template counts → feature vectors, over the full HDFS_v1 log
(11,175,629 lines / ~1.5 GB → 575,061 block feature vectors).

Timed region excludes Spark startup. Single run per setting, Spark `local[N]`
on one machine (20 logical cores). These are single-machine, single-run numbers,
so treat them as indicative, not precise.

| Cores | Elapsed (s) | Throughput (lines/s) | Speedup vs 1 core |
|------:|------------:|---------------------:|------------------:|
| 1     | 450.8       | 24,793               | 1.00× |
| 2     | 281.2       | 39,745               | 1.60× |
| 4     | 174.9       | 63,913               | 2.58× |
| 8     | 118.4       | 94,359               | 3.81× |
| 20    | 138.2       | 80,848               | 3.26× |

## Observations
- Scaling is sublinear: the `groupBy` shuffle over 575K blocks, single-disk
  I/O, and JVM/GC overhead cap the gains on a single machine.
- Throughput peaks at **8 cores (3.81×)** and *regresses* at 20. The machine has
  ~8–10 physical cores plus hyperthreading; oversubscribing past the physical
  cores adds shuffle/scheduling contention that outweighs the extra parallelism.
- Takeaway: for this shuffle- and I/O-bound workload, the right lever past the
  physical-core limit is more *machines* (a real cluster), not more threads.

Reproduce: `sbt "runMain Benchmark data/HDFS_v1/HDFS.log local[N]"`