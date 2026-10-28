/** One parsed HDFS log line, broken into structured fields. */
case class LogEvent(
  date: String,
  time: String,
  pid: String,
  level: String,
  component: String,
  content: String,
  blockIds: Seq[String]
)

/** Pure functions for turning raw HDFS log lines into structured LogEvents. */
object LogParser {

  // <date> <time> <pid> <level> <component>: <content>
  private val LinePattern =
    """^(\d{6})\s+(\d{6})\s+(\d+)\s+(\w+)\s+([\w.$]+):\s+(.*)$""".r

  // HDFS block ids look like blk_38865049064139660 or blk_-6952295868487656571
  private val BlockPattern = """blk_-?\d+""".r

  def parse(line: String): Option[LogEvent] = line match {
    case LinePattern(date, time, pid, level, component, content) =>
      val blocks = BlockPattern.findAllIn(content).toList
      Some(LogEvent(date, time, pid, level, component, content, blocks))
    case _ => None
  }
}