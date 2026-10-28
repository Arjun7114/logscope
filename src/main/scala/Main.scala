import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Main {
  def main(args: Array[String]): Unit = {
    val inputPath = if (args.nonEmpty) args(0) else "data/sample.log"

    val spark = SparkSession.builder()
      .appName("logscope")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    println(s"==> Reading from: $inputPath")

    val rawLines = spark.read.textFile(inputPath)
    val events   = rawLines.flatMap(LogParser.parse).cache()

    val total  = rawLines.count()
    val parsed = events.count()
    println(s"==> Raw lines:     $total")
    println(s"==> Parsed events: $parsed")
    println(s"==> Unparsed:      ${total - parsed}")

    val distinctBlocks = events
      .select(explode($"blockIds").as("blockId"))
      .distinct()
      .count()
    println(s"==> Distinct block ids: $distinctBlocks")

    // --- Template analysis ---
    val distinctTemplates = events.select($"template").distinct().count()
    println(s"==> Distinct templates: $distinctTemplates")

    println("==> Templates by frequency:")
    events.groupBy($"template").count().orderBy($"count".desc).show(50, truncate = false)

    spark.stop()
  }
}