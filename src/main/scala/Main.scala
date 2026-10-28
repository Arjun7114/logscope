import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.CountVectorizer

object Main {
  def main(args: Array[String]): Unit = {
    val inputPath = if (args.nonEmpty) args(0) else "data/sample.log"
    val labelPath = "data/HDFS_v1/preprocessed/anomaly_label.csv"

    val spark = SparkSession.builder()
      .appName("logscope")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    println(s"==> Reading from: $inputPath")

    val rawLines = spark.read.textFile(inputPath)
    val events   = rawLines.flatMap(LogParser.parse)

    // Each block -> the list of templates that occurred for it
    val perBlock = events
      .select(explode($"blockIds").as("blockId"), $"template")
      .groupBy($"blockId")
      .agg(collect_list($"template").as("templates"))

    // Turn each block's template list into a fixed-length count vector
    val cv = new CountVectorizer()
      .setInputCol("templates")
      .setOutputCol("features")
    val cvModel  = cv.fit(perBlock)
    val featured = cvModel.transform(perBlock).select($"blockId", $"features")
    println(s"==> Feature vocabulary size: ${cvModel.vocabulary.length}")

    // Load ground-truth labels (2 columns: block id, Normal/Anomaly)
    val labels = spark.read.option("header", "true").csv(labelPath)
      .toDF("blockId", "label")
    println(s"==> Labels loaded: ${labels.count()}")
    println("==> Label distribution:")
    labels.groupBy($"label").count().show()

    // Join features with labels
    val labeled = featured.join(labels, Seq("blockId"), "inner").cache()
    println(s"==> Blocks with features AND label: ${labeled.count()}")

    println("==> Sample labeled feature rows:")
    labeled.show(5, truncate = false)

    spark.stop()
  }
}