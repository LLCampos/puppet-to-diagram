package puppet_to_diagram

import io.circe.{ACursor, DecodingFailure, Json}

case class PuppetClass(name: String, definition: Json)

object PuppetParser {
  def generateCoreEntityFromPuppetClassJson(puppetClassJson: Json, parametersToRepresent: List[ParameterConfig]): Either[DecodingFailure, CoreEntity] = {
    val puppetClass = puppetClassJson.hcursor.downField("puppet_classes").downArray
    for {
      coreEntityName <- puppetClass.downField("name").as[String]
      parameters <- extractPropertiesFromDefaultsCursor(puppetClass.downField("defaults"), parametersToRepresent)
    } yield CoreEntity(coreEntityName, parameters)
  }

  private def extractPropertiesFromDefaultsCursor(defaultsCursor: ACursor, parametersToRepresent: List[ParameterConfig]): Either[DecodingFailure, Seq[Parameter]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, value) if parametersToRepresent.map(_.rawName).contains(name) =>
        val parameterConfig = parametersToRepresent.filter(_.rawName == name).head
        val showName = parameterConfig.prettyName
        if (value.startsWith("[")) {
          processListEntity(value).zipWithIndex.map { case (v, i) => Parameter(parameterConfig, s"$showName ${i + 1}", v) }
        } else {
          List(Parameter(parameterConfig, showName, processStringEntity(value)))
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

  def generateCoreEntityFromHieraPuppetNodeJson(hieraPuppetNode: Json, puppetClassToRepresent: PuppetClass, parametersToRepresent: List[ParameterConfig]
  ): Either[DecodingFailure, CoreEntity] = {
    val hieraProperties = extractHieraParametersForClass(hieraPuppetNode, puppetClassToRepresent.name, parametersToRepresent)
    val coreEntity = generateCoreEntityFromPuppetClassJson(puppetClassToRepresent.definition, parametersToRepresent)
    coreEntity.map(overrideProperties(_, hieraProperties))
  }

  private def extractHieraParametersForClass(hieraJson: Json, classToRepresent: String, parametersToRepresent: List[ParameterConfig]): Map[String, String] =
    parametersToRepresent.map(p => {
      val hieraParameter = generateHieraParameter(classToRepresent, p.rawName)
      p.rawName -> getValueFromJson(hieraJson, hieraParameter)
    }).toMap.filter(_._2.isDefined).mapValues(_.get)

  private def generateHieraParameter(className: String, parameterName: String) =
    s"$className::$parameterName"

  private def getValueFromJson(json: Json, key: String) = {
    json.hcursor.downField(key).focus.flatMap(_.asString)
  }

  private def overrideProperties(coreEntity: CoreEntity, hieraProperties: Map[String, String]): CoreEntity = {
    val overriddenLinks = coreEntity.links.map { link =>
      hieraProperties.get(link.config.rawName) match {
        case Some(value) => link.copy(value = processStringEntity(value))
        case None => link
      }
    }
    coreEntity.copy(links = overriddenLinks)
  }
}
