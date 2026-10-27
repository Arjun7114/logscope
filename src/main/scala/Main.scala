import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]): Unit = {
    // Use the path passed on the command line, else fall back to the small sample.
    val inputPath = if (args.nonEmpty) args(0) else "data/sample.log"

    val spark = SparkSession.builder()
      .appName("logscope")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    println(s"==> Reading from: $inputPath")

    val rawLines = spark.read.textFile(inputPath)
    // Parse, and cache so we don't re-parse the whole file for every action below.
    val events = rawLines.flatMap(LogParser.parse).cache()

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