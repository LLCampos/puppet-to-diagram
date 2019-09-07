package puppet_to_diagram

import io.circe.{ACursor, DecodingFailure, Json}

object PuppetParser {
  def coreEntityFromJson(json: Json, propertiesToConsider: List[PropertyConfig]): Either[DecodingFailure, CoreEntity] = {
    val puppetClass = json.hcursor.downField("puppet_classes").downArray
    for {
      coreEntityName <- puppetClass.downField("name").as[String]
      properties <- extractPropertiesFromDefaultsCursor(puppetClass.downField("defaults"), propertiesToConsider)
    } yield CoreEntity(coreEntityName, properties)
  }

  private def extractPropertiesFromDefaultsCursor(defaultsCursor: ACursor, propertiesToConsider: List[PropertyConfig]): Either[DecodingFailure, Seq[Property]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, value) if propertiesToConsider.map(_.rawName).contains(name) =>
        val propertyConfig = propertiesToConsider.filter(_.rawName == name).head
        val showName = propertyConfig.prettyName
        if (value.startsWith("[")) {
          processListEntity(value).zipWithIndex.map { case (v, i) => Property(propertyConfig, s"$showName ${i + 1}", v) }
        } else {
          List(Property(propertyConfig, showName, processStringEntity(value)))
        }
    }.flatten)
  }

  private def processStringEntity(value: String) = {
    stripUrlDetails(value.stripPrefix("\"").stripSuffix("\""))
  }

  private def stripUrlDetails(value: String) = {
    value.split("://")(1).split(""":\d+\?""")(0)
  }

  private def processListEntity(value: String): List[String] = {
    value
      .stripPrefix("[").stripSuffix("]").trim
      .split("\n")
      .map(_.trim.stripSuffix(","))
      .map(processStringEntity)
      .toList
  }
}
