import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]): Unit = {
    // Start a local Spark session using all CPU cores
    val spark = SparkSession.builder()
      .appName("logscope")
      .master("local[*]")
      .getOrCreate()

    // Quiet down Spark's very chatty logging
    spark.sparkContext.setLogLevel("WARN")

    // Read the sample log file, one line per row
    val lines = spark.read.textFile("data/sample.log")

    println(s"==> Total lines: ${lines.count()}")
    println("==> First few lines:")
    lines.show(5, truncate = false)

    spark.stop()
  }
}