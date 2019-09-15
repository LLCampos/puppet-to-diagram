package puppet_to_diagram

sealed trait ParameterValue

case class ParameterString(value: String) extends ParameterValue
object ParameterString {
  def buildWithCleanUp(value: String) = new ParameterString(processStringEntity(value))

  private def processStringEntity(value: String) = {
    stripUrlDetails(value.stripPrefix("\"").stripSuffix("\""))
  }

  private def stripUrlDetails(value: String) = {
    value.split("://")(1).split(""":\d+\?""")(0)
  }
}

case class ParameterSeq(value: Seq[String]) extends ParameterValue
object ParameterSeq {
  def apply(value: String): ParameterSeq =
    new ParameterSeq(processListEntity(value))

  private def processListEntity(value: String): Seq[String] = {
    value
      .stripPrefix("[").stripSuffix("]").trim
      .split("\n")
      .map(_.trim.stripSuffix(","))
      .map(ParameterString.buildWithCleanUp(_).value)
  }
}


