package model

import guru.nidi.graphviz.model.Graph
import guru.nidi.graphviz.model.Factory._
import io.circe.{ACursor, DecodingFailure, Json}

case class CentralNode(name: String, links: Seq[OuterNode])
case class OuterNode(name: String, url: String)

object CentralNode {
  def toGraphvizGraph(centralNode: CentralNode): Graph = {
    val graphCentralNode = node(centralNode.name)
    val graphWithCentralNode = graph().`with`(graphCentralNode)
    centralNode.links.foldLeft(graphWithCentralNode)((g, n) => g.`with`(node(n.name).link(node(centralNode.name))))
  }

  def fromJson(json: Json, externalDependencies: List[String]): Either[DecodingFailure, CentralNode] = {
    val puppetClass = json.hcursor.downField("puppet_classes").downArray
    for {
      centralNodeName <- puppetClass.downField("name").as[String]
      outerNodes <- defaultsCursorToOuterNodes(puppetClass.downField("defaults"), externalDependencies)
    } yield CentralNode(centralNodeName, outerNodes)
  }

  private def defaultsCursorToOuterNodes(defaultsCursor: ACursor, externalDependencies: List[String]): Either[DecodingFailure, Seq[OuterNode]] = {
    defaultsCursor.as[Map[String, String]].map(_.toList.collect {
      case (name, url) if externalDependencies.contains(name) => OuterNode(name, processUrl(url))
    })
  }

  private def processUrl(url: String) = url.stripPrefix("\"").stripSuffix("\"")

}