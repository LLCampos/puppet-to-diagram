package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.model.{Graph, Node}
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CoreEntity(name: String, links: Seq[Property])
case class Property(config: PropertyConfig, showName: String, value: String)
case class PropertyConfig(rawName: String, prettyName: String, arrowDirection: ArrowDirection)

sealed trait ArrowDirection
object In extends ArrowDirection
object Out extends ArrowDirection

case class CoreNodeData(coreEntity: CoreEntity, node: Node, links: Seq[PropertyNodeData])
case class PropertyNodeData(property: Property, node: Node)

object CoreEntity {
  val ColorCoreNode: Color = Color.rgb("FFE5CC")
  val ColorPropertyNodes: Color = Color.rgb("D0F0FD")

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

  private def coreEntityToCoreNodeData(coreEntity: CoreEntity): CoreNodeData = {
    val propertyNodeDatas = buildPropertyNodeDatas(coreEntity)
    CoreNodeData(
      coreEntity,
      buildCoreNode(coreEntity, propertyNodeDatas),
      propertyNodeDatas
    )
  }

  private def buildPropertyNodeDatas(coreEntity: CoreEntity): Seq[PropertyNodeData] =
    coreEntity.links.map(p => PropertyNodeData(p, buildPropertyNode(p, coreEntity)))

  private def buildPropertyNode(property: Property, coreEntity: CoreEntity): Node = {
    val propertyNode = prettifyPropertyNode(node(property.showName), property)
    if (property.config.arrowDirection == Out)
        propertyNode.link(node(coreEntity.name))
    else
        propertyNode
  }

  private def prettifyPropertyNode(node: Node, property: Property) =
    node
      .`with`(buildNodeLabelForProperty(property))
      .`with`(Style.FILLED, ColorPropertyNodes)

  private def buildNodeLabelForProperty(property: Property) = {
    Label.html(s"<b>${property.showName}</b><br/>${property.value}")
  }

  private def buildCoreNode(coreEntity: CoreEntity, propertyNodeDatas: Seq[PropertyNodeData]): Node = {
    val baseNode = prettifyCoreNode(node(coreEntity.name))
    propertyNodeDatas.filter(_.property.config.arrowDirection == In)
      .foldLeft(baseNode)((n, p) => n.link(p.node))
  }

  private def prettifyCoreNode(node: Node): Node =
    node.`with`(Style.FILLED, ColorCoreNode)

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