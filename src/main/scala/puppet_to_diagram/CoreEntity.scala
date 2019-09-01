package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Label, Shape}
import guru.nidi.graphviz.model.{Graph, Node}
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CoreEntity(name: String, links: Seq[Property])
case class Property(config: PropertyConfig, showName: String, value: String)
case class PropertyConfig(rawName: String, arrowDirection: ArrowDirection)

sealed trait ArrowDirection
object In extends ArrowDirection
object Out extends ArrowDirection

case class CoreNodeData(coreEntity: CoreEntity, node: Node, links: Seq[PropertyNodeData])
case class PropertyNodeData(property: Property, node: Node)

object CoreEntity {
  def toGraphvizGraph(coreEntity: CoreEntity): Graph = {
    val coreNodeData = coreEntityToCoreNodeData(coreEntity)
    buildGraph(coreNodeData)
  }

  private def buildGraph(coreNodeData: CoreNodeData): Graph = {
    val baseGraph = buildBaseGraph(coreNodeData)
    coreNodeData.links.foldLeft(baseGraph)((g, n) => g.`with`(n.node))
  }

  private def buildBaseGraph(coreNodeData: CoreNodeData): Graph =
    graph().`with`(coreNodeData.node).directed().nodeAttr().`with`(Shape.RECTANGLE)

  private def coreEntityToCoreNodeData(coreEntity: CoreEntity): CoreNodeData =
    CoreNodeData(
      coreEntity,
      buildCoreNode(coreEntity),
      buildPropertyNodeDatas(coreEntity)
    )

  private def buildPropertyNodeDatas(coreEntity: CoreEntity): Seq[PropertyNodeData] =
    coreEntity.links.map(p => PropertyNodeData(p, buildPropertyNode(p, coreEntity)))

  private def buildPropertyNode(property: Property, coreEntity: CoreEntity): Node =
    node(property.showName)
      .link(node(coreEntity.name))
      .`with`(Label.html(s"<b>${property.showName}</b><br/>${property.value}"))

  private def buildCoreNode(centralNode: CoreEntity): Node =
    node(centralNode.name)

  def fromJson(json: Json, propertiesToConsider: List[PropertyConfig]): Either[DecodingFailure, CoreEntity] = {
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
        if (value.startsWith("[")) {
          processListEntity(value).zipWithIndex.map { case (v, i) => Property(propertyConfig, s"$name ${i + 1}", v) }
        } else {
          List(Property(propertyConfig, name, processStringEntity(value)))
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