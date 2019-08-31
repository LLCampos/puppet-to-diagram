package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Label, Shape}
import guru.nidi.graphviz.model.{Graph, Node}
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CoreEntity(name: String, links: Seq[Property])
case class Property(config: PropertyConfig, showName: String, value: String)

sealed trait ArrowDirection
object In extends ArrowDirection
object Out extends ArrowDirection

case class PropertyConfig(rawName: String, arrowDirection: ArrowDirection)

object CoreEntity {
  def toGraphvizGraph(coreEntity: CoreEntity): Graph = {
    val coreNode = buildCoreNode(coreEntity)
    val graphWithCoreNode = buildBaseGraph(coreNode)
    coreEntity.links.foldLeft(graphWithCoreNode)((g, n) => addOuterNodesToGraph(g, n, coreEntity))
  }

  def fromJson(json: Json, propertiesToConsider: List[PropertyConfig]): Either[DecodingFailure, CoreEntity] = {
    val puppetClass = json.hcursor.downField("puppet_classes").downArray
    for {
      coreEntityName <- puppetClass.downField("name").as[String]
      properties <- extractPropertiesFromDefaultsCursor(puppetClass.downField("defaults"), propertiesToConsider)
    } yield CoreEntity(coreEntityName, properties)
  }

  private def buildCoreNode(centralNode: CoreEntity): Node =
    node(centralNode.name).`with`(Shape.RECTANGLE)

  private def buildBaseGraph(coreNode: Node): Graph =
    graph().`with`(coreNode).directed()

  private def extractPropertiesFromDefaultsCursor(defaultsCursor: ACursor, propertiesToConsider: List[PropertyConfig]): Either[DecodingFailure, Seq[Property]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, value) if propertiesToConsider.map(_.rawName).contains(name) =>
        val propertyConfig = propertiesToConsider.filter(_.rawName == name).head
        if (value.startsWith("[")) {
          processListEntity(value).zipWithIndex.map { case (v, i) => Property(propertyConfig, s"$name ${i + 1}", v) }
        } else {
          List(Property(propertyConfig, name, processStringEntity(value)))
        }
    }.flatten)
  }

  private def addOuterNodesToGraph(graph: Graph, properties: Property, coreEntity: CoreEntity) = {
    val outerGraphNode = node(properties.showName)
      .link(node(coreEntity.name))
      .`with`(Label.html(s"<b>${properties.showName}</b><br/>${properties.value}"))
      .`with`(Shape.RECTANGLE)
    graph.`with`(outerGraphNode)
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