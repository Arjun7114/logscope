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
    val events   = rawLines.flatMap(LogParser.parse)

    // One row per (block, event template)
    val blockTemplates = events
      .select(explode($"blockIds").as("blockId"), $"template")

    // Event-count matrix (long form): how many times each template occurs per block
    val counts = blockTemplates
      .groupBy($"blockId", $"template")
      .count()
      .cache()

    val numBlocks  = counts.select($"blockId").distinct().count()
    val matrixRows = counts.count()
    println(s"==> Blocks: $numBlocks")
    println(s"==> Non-zero (block, template) cells: $matrixRows")

    // Per-block: total events and distinct templates
    val perBlockStats = blockTemplates
      .groupBy($"blockId")
      .agg(
        count(lit(1)).as("numEvents"),
        countDistinct($"template").as("numTemplates")
      )

    println("==> Events per block (min / avg / max):")
    perBlockStats.agg(min($"numEvents"), round(avg($"numEvents"), 2), max($"numEvents")).show()

    println("==> Distinct templates per block (min / avg / max):")
    perBlockStats.agg(min($"numTemplates"), round(avg($"numTemplates"), 2), max($"numTemplates")).show()

    println("==> Sample of the count matrix (highest single counts):")
    counts.orderBy($"count".desc).show(10, truncate = false)

    spark.stop()
  }
}