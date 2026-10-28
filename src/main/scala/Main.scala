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

    // --- Block ID analysis ---
    val withBlocks = events.filter(size($"blockIds") > 0).count()
    println(s"==> Events with a block id: $withBlocks")
    println(s"==> Events with no block id: ${parsed - withBlocks}")

    val distinctBlocks = events
      .select(explode($"blockIds").as("blockId"))
      .distinct()
      .count()
    println(s"==> Distinct block ids: $distinctBlocks")

    println("==> Sample events with their block ids:")
    events.select($"component", $"blockIds").show(5, truncate = false)

    spark.stop()
  }
}