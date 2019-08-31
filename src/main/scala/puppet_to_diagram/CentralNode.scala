package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Label, Shape}
import guru.nidi.graphviz.model.{Graph, Node}
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CentralNode(name: String, links: Seq[OuterNode])
case class OuterNode(name: String, url: String)

object CentralNode {
  def toGraphvizGraph(centralNode: CentralNode): Graph = {
    val graphCentralNode = buildCentralNode(centralNode)
    val graphWithCentralNode = graph().`with`(graphCentralNode)
    centralNode.links.foldLeft(graphWithCentralNode)((g, n) => addOuterNodeToGraphWithCentralNode(g, n, centralNode))
  }

  def fromJson(json: Json, externalDependencies: List[String]): Either[DecodingFailure, CentralNode] = {
    val puppetClass = json.hcursor.downField("puppet_classes").downArray
    for {
      centralNodeName <- puppetClass.downField("name").as[String]
      outerNodes <- defaultsCursorToOuterNodes(puppetClass.downField("defaults"), externalDependencies)
    } yield CentralNode(centralNodeName, outerNodes)
  }

  private def buildCentralNode(centralNode: CentralNode): Node =
    node(centralNode.name).`with`(Shape.RECTANGLE)

  private def defaultsCursorToOuterNodes(defaultsCursor: ACursor, externalDependencies: List[String]): Either[DecodingFailure, Seq[OuterNode]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, url) if externalDependencies.contains(name) => OuterNode(name, processUrl(url))
    })
  }

  private def addOuterNodeToGraphWithCentralNode(graph: Graph, outerNode: OuterNode, centralNode: CentralNode) = {
    val outerGraphNode = node(outerNode.name)
      .link(node(centralNode.name))
      .`with`(Label.html(s"<b>${outerNode.name}</b><br/>${outerNode.url}"))
      .`with`(Shape.RECTANGLE)
    graph.`with`(outerGraphNode)
  }

  private def processUrl(url: String) = url.stripPrefix("\"").stripSuffix("\"")

}