package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Label, Shape}
import guru.nidi.graphviz.model.{Graph, Node}
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CoreEntity(name: String, links: Seq[Properties])
case class Properties(name: String, value: String)

object CoreEntity {
  def toGraphvizGraph(coreEntity: CoreEntity): Graph = {
    val coreNode = buildCoreNode(coreEntity)
    val graphWithCoreNode = buildBaseGraph(coreNode)
    coreEntity.links.foldLeft(graphWithCoreNode)((g, n) => addOuterNodesToGraph(g, n, coreEntity))
  }

  def fromJson(json: Json, propertiesToConsider: List[String]): Either[DecodingFailure, CoreEntity] = {
    val puppetClass = json.hcursor.downField("puppet_classes").downArray
    for {
      coreEntityName <- puppetClass.downField("name").as[String]
      properties <- defaultsCursorToOuterNodes(puppetClass.downField("defaults"), propertiesToConsider)
    } yield CoreEntity(coreEntityName, properties)
  }

  private def buildCoreNode(centralNode: CoreEntity): Node =
    node(centralNode.name).`with`(Shape.RECTANGLE)

  private def buildBaseGraph(coreNode: Node): Graph =
    graph().`with`(coreNode).directed()

  private def defaultsCursorToOuterNodes(defaultsCursor: ACursor, externalDependencies: List[String]): Either[DecodingFailure, Seq[Properties]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, url) if externalDependencies.contains(name) => Properties(name, processEntity(url))
    })
  }

  private def addOuterNodesToGraph(graph: Graph, properties: Properties, coreEntity: CoreEntity) = {
    val outerGraphNode = node(properties.name)
      .link(node(coreEntity.name))
      .`with`(Label.html(s"<b>${properties.name}</b><br/>${properties.value}"))
      .`with`(Shape.RECTANGLE)
    graph.`with`(outerGraphNode)
  }

  private def processEntity(url: String) = url.stripPrefix("\"").stripSuffix("\"")

}