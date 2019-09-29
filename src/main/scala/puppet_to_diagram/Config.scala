package puppet_to_diagram

case class Config(
  pathToPuppetProject: String,
  module: String,
  parametersConfigs: List[ParameterConfig])

case class ParameterConfig(rawName: String, prettyName: String, arrowDirection: ArrowDirection)

sealed trait ArrowDirection extends Serializable
case object In extends ArrowDirection
case object Out extends ArrowDirection