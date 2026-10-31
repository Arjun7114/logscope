import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.{StandardScaler, PCA}
import org.apache.spark.ml.linalg.{Vector => MLVector}

object PcaDetector {
  def main(args: Array[String]): Unit = {
    val inputPath = if (args.nonEmpty) args(0) else "data/sample.log"
    val labelPath = "data/HDFS_v1/preprocessed/anomaly_label.csv"
    val k         = if (args.length > 1) args(1).toInt else 4   // number of principal components

    val spark = SparkSession.builder().appName("logscope-pca").master("local[*]").getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    println(s"==> Reading from: $inputPath  (PCA components k=$k)")

    val featured = Pipeline.featureMatrix(spark, inputPath)
    val labels   = spark.read.option("header", "true").csv(labelPath).toDF("blockId", "label")
    val labeled  = featured.join(labels, Seq("blockId"), "inner")

    // Standardize features (center + scale)
    val scaler = new StandardScaler()
      .setInputCol("features").setOutputCol("scaled")
      .setWithStd(true).setWithMean(true)
    val scaled = scaler.fit(labeled).transform(labeled)

    // PCA: project onto the top-k "normal behavior" subspace
    val pca       = new PCA().setInputCol("scaled").setOutputCol("pca").setK(k)
    val projected = pca.fit(scaled).transform(scaled)

    // Anomaly score = reconstruction error (SPE) = ||x||^2 - ||projection||^2
    // (large = the block does not fit the normal subspace)
    val sqNorm = udf { (v: MLVector) => val a = v.toArray; a.map(x => x * x).sum }
    val scored = projected
      .withColumn("spe", sqNorm($"scaled") - sqNorm($"pca"))
      .cache()

    // Flag the most-distant ~3% as anomalies
    val threshold = scored.stat.approxQuantile("spe", Array(0.97), 0.001)(0)
    println(f"==> SPE threshold (97th pct): $threshold%.4f")

    val pred = scored
      .withColumn("predAnomaly", $"spe" > lit(threshold))
      .withColumn("isAnomaly",   $"label" === "Anomaly")

    val tp = pred.filter($"predAnomaly" &&  $"isAnomaly").count().toDouble
    val fp = pred.filter($"predAnomaly" && !$"isAnomaly").count().toDouble
    val fn = pred.filter(!$"predAnomaly" &&  $"isAnomaly").count().toDouble
    val tn = pred.filter(!$"predAnomaly" && !$"isAnomaly").count().toDouble

    val precision = if (tp + fp > 0) tp / (tp + fp) else 0.0
    val recall    = if (tp + fn > 0) tp / (tp + fn) else 0.0
    val f1        = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0

    println(f"==> TP=$tp%.0f  FP=$fp%.0f  FN=$fn%.0f  TN=$tn%.0f")
    println(f"==> Precision: $precision%.4f")
    println(f"==> Recall:    $recall%.4f")
    println(f"==> F1:        $f1%.4f")

    spark.stop()
  }
}