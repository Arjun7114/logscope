/** One parsed HDFS log line, broken into structured fields. */
case class LogEvent(
  date: String,
  time: String,
  pid: String,
  level: String,
  component: String,
  content: String
)

/** Pure functions for turning raw HDFS log lines into structured LogEvents. */
object LogParser {

  // HDFS log format:
  //   <date> <time> <pid> <level> <component>: <content>
  // e.g. 081109 203615 148 INFO dfs.DataNode$PacketResponder: PacketResponder 1 ...
  private val LinePattern =
    """^(\d{6})\s+(\d{6})\s+(\d+)\s+(\w+)\s+([\w.$]+):\s+(.*)$""".r

  /** Parse one line. Returns None if the line doesn't match the expected format. */
  def parse(line: String): Option[LogEvent] = line match {
    case LinePattern(date, time, pid, level, component, content) =>
      Some(LogEvent(date, time, pid, level, component, content))
    case _ => None
  }
}