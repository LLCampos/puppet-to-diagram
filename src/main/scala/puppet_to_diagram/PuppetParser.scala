package puppet_to_diagram

import io.circe.{ACursor, DecodingFailure, Json}
import io.circe.yaml.{parser => yamlParser}

case class PuppetClass(name: String, definition: Json)

object PuppetParser {
  def generateCoreEntityFromPuppetClassJson(puppetClassJson: Json, parametersToRepresent: List[ParameterConfig], serverName: String): Either[DecodingFailure, CoreEntity] = {
    val puppetClass = puppetClassJson.hcursor.downField("puppet_classes").downArray
    for {
      parameters <- extractPropertiesFromDefaultsCursor(puppetClass.downField("defaults"), parametersToRepresent)
    } yield CoreEntity(serverName, parameters)
  }

  private def extractPropertiesFromDefaultsCursor(defaultsCursor: ACursor, parametersToRepresent: List[ParameterConfig]): Either[DecodingFailure, Seq[Parameter]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, value) if parametersToRepresent.map(_.rawName).contains(name) =>
        val parameterConfig = parametersToRepresent.filter(_.rawName == name).head
        val prettyName = parameterConfig.prettyName
        if (value.startsWith("[")) {
          Parameter(parameterConfig, prettyName, ParameterSeq(value))
        } else {
          Parameter(parameterConfig, prettyName, ParameterString.buildWithCleanUp(value))
        }
    })
  }

  def generateCoreEntityFromHieraYamls(hieraPuppetNodeYaml: String, hieraPuppetCommonYaml: String, puppetClassToRepresent: PuppetClass, parametersToRepresent: List[ParameterConfig], serverName: String): Either[io.circe.Error, CoreEntity] = {
    val mergedHieraJson = for {
      hieraPuppetNodeJson <- yamlParser.parse(hieraPuppetNodeYaml)
      hieraPuppetCommonJson <- yamlParser.parse(hieraPuppetCommonYaml)
    } yield hieraPuppetCommonJson.deepMerge(hieraPuppetNodeJson)

    mergedHieraJson.right.flatMap(
      generateCoreEntityFromHieraPuppetNodeJson(_, puppetClassToRepresent, parametersToRepresent, serverName)
    )
  }

  private def generateCoreEntityFromHieraPuppetNodeJson(hieraPuppetNode: Json, puppetClassToRepresent: PuppetClass, parametersToRepresent: List[ParameterConfig], serverName: String
  ): Either[DecodingFailure, CoreEntity] = {
    val hieraProperties = extractHieraParametersForClass(hieraPuppetNode, puppetClassToRepresent.name, parametersToRepresent)
    val coreEntity = generateCoreEntityFromPuppetClassJson(puppetClassToRepresent.definition, parametersToRepresent, serverName)
    coreEntity.map(overrideProperties(_, hieraProperties))
  }

  private def extractHieraParametersForClass(hieraJson: Json, classToRepresent: String, parametersToRepresent: List[ParameterConfig]): Map[String, Seq[String]] =
    parametersToRepresent.map(p => {
      val hieraParameter = generateHieraParameter(classToRepresent, p.rawName)
      p.rawName -> getValueFromJson(hieraJson, hieraParameter)
    }).toMap.filter(_._2.isDefined).mapValues(_.get)

  private def generateHieraParameter(className: String, parameterName: String) =
    s"$className::$parameterName"

  private def getValueFromJson(json: Json, key: String) = {
    json.hcursor.downField(key).focus.flatMap {
      case json if json.isArray => json.asArray.map(_.flatMap(_.asString))
      case json => json.asString.map(Seq(_))
    }
  }

  private def overrideProperties(coreEntity: CoreEntity, hieraProperties: Map[String, Seq[String]]): CoreEntity = {
    val overriddenLinks = coreEntity.links.map { link =>
      hieraProperties.get(link.config.rawName) match {
        case Some(value) =>
          if (value.size == 1) {
            link.copy(value = ParameterString.buildWithCleanUp(value.head))
          } else {
            link.copy(value = ParameterSeq.buildWithCleanUp(value))
          }
        case None => link
      }
    }
    coreEntity.copy(links = overriddenLinks)
  }
}
