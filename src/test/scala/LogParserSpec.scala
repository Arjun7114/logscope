import org.scalatest.funsuite.AnyFunSuite

class LogParserSpec extends AnyFunSuite {

  test("parses a well-formed HDFS log line into fields") {
    val line = "081109 203615 148 INFO dfs.DataNode$PacketResponder: " +
      "PacketResponder 1 for block blk_38865049064139660 terminating"
    val e = LogParser.parse(line).get
    assert(e.date == "081109")
    assert(e.time == "203615")
    assert(e.pid == "148")
    assert(e.level == "INFO")
    assert(e.component == "dfs.DataNode$PacketResponder")
    assert(e.blockIds == Seq("blk_38865049064139660"))
  }

  test("returns None for a non-matching line") {
    assert(LogParser.parse("this is not an hdfs log line").isEmpty)
  }

  test("extracts negative block ids") {
    val e = LogParser.parse(
      "081109 203807 222 INFO dfs.X: PacketResponder 0 for block blk_-6952295868487656571 terminating"
    ).get
    assert(e.blockIds == Seq("blk_-6952295868487656571"))
  }

  test("template masks block ids, ips, paths and numbers") {
    val t = LogParser.template("Received block blk_-123 of size 91178 from /10.250.19.102")
    assert(t == "Received block <BLK> of size <NUM> from /<IP>")
  }

  test("two lines of the same event type share a template") {
    val a = LogParser.parse("081109 203615 148 INFO dfs.X: PacketResponder 1 for block blk_1 terminating").get
    val b = LogParser.parse("081109 203615 148 INFO dfs.X: PacketResponder 2 for block blk_2 terminating").get
    assert(a.template == b.template)
  }
}