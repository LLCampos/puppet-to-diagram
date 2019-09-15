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

// TODO: Should be Seq instead of List?
case class ParameterList(value: List[String]) extends ParameterValue
object ParameterList {
  def apply(value: String): ParameterList =
    new ParameterList(processListEntity(value))

  private def processListEntity(value: String): List[String] = {
    value
      .stripPrefix("[").stripSuffix("]").trim
      .split("\n")
      .map(_.trim.stripSuffix(","))
      .map(ParameterString.buildWithCleanUp(_).value)
      .toList
  }
}


