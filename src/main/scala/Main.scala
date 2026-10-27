import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("logscope")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    val inputPath = "data/sample.log"

    // Read raw lines, then parse each into a structured LogEvent.
    // flatMap drops any line that didn't match (parse returned None).
    val rawLines = spark.read.textFile(inputPath)
    val events   = rawLines.flatMap(LogParser.parse)

    val total  = rawLines.count()
    val parsed = events.count()

    println(s"==> Raw lines:     $total")
    println(s"==> Parsed events: $parsed")
    println(s"==> Unparsed:      ${total - parsed}")

    println("==> Sample of parsed events:")
    events.show(5, truncate = false)

    println("==> Event counts by log level:")
    events.groupBy("level").count().show()

    spark.stop()
  }
}