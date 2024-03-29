package puppet_to_diagram

sealed trait ParameterValue

case class ParameterString(value: String) extends ParameterValue
object ParameterString {
  def buildWithCleanUp(value: String) = new ParameterString(processStringEntity(value))

  private def processStringEntity(value: String) = {
    stripUrlDetails(value.stripPrefix("\"").stripSuffix("\""))
  }

  private def stripUrlDetails(value: String) = {
    value.split("://") match {
      case v if v.size > 1 => v(1).split(""":\d+\?""")(0)
      case v => v(0)
    }
  }
}

case class ParameterSeq(value: Seq[String]) extends ParameterValue
object ParameterSeq {
  def buildWithCleanUp(value: Seq[String]): ParameterSeq =
    ParameterSeq(value.map(ParameterString.buildWithCleanUp(_).value))

  def apply(value: String): ParameterSeq =
    new ParameterSeq(processSeqEntity(value))

  private def processSeqEntity(value: String): Seq[String] = {
    value
      .stripPrefix("[").stripSuffix("]").trim
      .split("\n")
      .map(_.trim.stripSuffix(","))
      .map(ParameterString.buildWithCleanUp(_).value)
  }
}


