import org.apache.spark.sql.SparkSession
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator

object Main {
  def main(args: Array[String]): Unit = {
    val inputPath = if (args.nonEmpty) args(0) else "data/sample.log"
    val labelPath = "data/HDFS_v1/preprocessed/anomaly_label.csv"

    val spark = SparkSession.builder().appName("logscope").master("local[*]").getOrCreate()
    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    println(s"==> Reading from: $inputPath")

    val featured = Pipeline.featureMatrix(spark, inputPath)

    val labels  = spark.read.option("header", "true").csv(labelPath).toDF("blockId", "label")
    val labeled = featured.join(labels, Seq("blockId"), "inner")

    val indexer = new StringIndexer().setInputCol("label").setOutputCol("labelIdx")
    val indexed = indexer.fit(labeled).transform(labeled)

    val Array(train, test) = indexed.randomSplit(Array(0.7, 0.3), seed = 42L)
    train.cache(); test.cache()
    println(s"==> Train rows: ${train.count()}   Test rows: ${test.count()}")

    val lr    = new LogisticRegression().setFeaturesCol("features").setLabelCol("labelIdx").setMaxIter(50)
    val model = lr.fit(train)
    val pred  = model.transform(test).cache()

    val tp = pred.filter($"prediction" === 1.0 && $"labelIdx" === 1.0).count().toDouble
    val fp = pred.filter($"prediction" === 1.0 && $"labelIdx" === 0.0).count().toDouble
    val fn = pred.filter($"prediction" === 0.0 && $"labelIdx" === 1.0).count().toDouble
    val tn = pred.filter($"prediction" === 0.0 && $"labelIdx" === 0.0).count().toDouble

    val precision = if (tp + fp > 0) tp / (tp + fp) else 0.0
    val recall    = if (tp + fn > 0) tp / (tp + fn) else 0.0
    val f1        = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0
    val auc = new BinaryClassificationEvaluator()
      .setLabelCol("labelIdx").setRawPredictionCol("rawPrediction").setMetricName("areaUnderROC")
      .evaluate(pred)

    println(f"==> Test-set anomalies: ${tp + fn}%.0f")
    println(f"==> TP=$tp%.0f  FP=$fp%.0f  FN=$fn%.0f  TN=$tn%.0f")
    println(f"==> Precision: $precision%.4f")
    println(f"==> Recall:    $recall%.4f")
    println(f"==> F1:        $f1%.4f")
    println(f"==> ROC AUC:   $auc%.4f")

    spark.stop()
  }
}