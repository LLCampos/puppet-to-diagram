package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.model.Factory._
import guru.nidi.graphviz.model.{Graph, Node}

case class CoreEntity(name: String, links: Seq[Parameter])
case class Parameter(config: ParameterConfig, prettyName: String, value: ParameterValue)
case class ParameterConfig(rawName: String, prettyName: String, arrowDirection: ArrowDirection)

sealed trait ArrowDirection
object In extends ArrowDirection
object Out extends ArrowDirection

case class CoreNodeData(coreEntity: CoreEntity, node: Node, links: Seq[ParameterNodeData])
case class ParameterNodeData(parameter: Parameter, nodes: Seq[Node])

object CoreEntity {
  val ColorCoreNode: Color = Color.rgb("FFE5CC")
  val ColorPropertyNodes: Color = Color.rgb("D0F0FD")

  def toGraphvizGraph(coreEntity: CoreEntity): Graph = {
    val coreNodeData = coreEntityToCoreNodeData(coreEntity)
    buildGraph(coreNodeData)
  }

  private def buildGraph(coreNodeData: CoreNodeData): Graph = {
    val baseGraph = buildBaseGraph(coreNodeData)
    coreNodeData.links.foldLeft(baseGraph)((g, n) => addNodesToGraph(g, n.nodes))
  }

  private def addNodesToGraph(graph: Graph, nodes: Seq[Node]): Graph =
    nodes.foldLeft(graph)((g, n) => g.`with`(n))

  private def buildBaseGraph(coreNodeData: CoreNodeData): Graph =
    graph().`with`(coreNodeData.node).directed().nodeAttr().`with`(Shape.RECTANGLE)

  private def coreEntityToCoreNodeData(coreEntity: CoreEntity): CoreNodeData = {
    val parameterNodeDatas = buildPropertyNodeDatas(coreEntity)
    CoreNodeData(
      coreEntity,
      buildCoreNode(coreEntity, parameterNodeDatas),
      parameterNodeDatas
    )
  }

  private def buildPropertyNodeDatas(coreEntity: CoreEntity): Seq[ParameterNodeData] =
    coreEntity.links.map { p =>
      p.value match {
        case ParameterString(v) => ParameterNodeData(p, Seq(buildPropertyNode(p.prettyName, v, p.config, coreEntity)))
        case ParameterList(l)   =>
          val nodes = l.zipWithIndex.map { case (v, i) =>
            buildPropertyNode(s"${p.prettyName} ${i + 1}", v, p.config, coreEntity)
          }
          ParameterNodeData(p, nodes)
      }
    }

  private def buildPropertyNode(nodeName: String, nodeValue: String, parameterConfig: ParameterConfig, coreEntity: CoreEntity): Node = {
    val parameterNode = prettifyPropertyNode(node(nodeName), nodeValue)
    if (parameterConfig.arrowDirection == Out)
        parameterNode.link(node(coreEntity.name))
    else
        parameterNode
  }

  private def prettifyPropertyNode(node: Node, nodeValue: String) =
    node
      .`with`(buildNodeLabelForProperty(node, nodeValue))
      .`with`(Style.FILLED, ColorPropertyNodes)

  private def buildNodeLabelForProperty(node: Node, nodeValue: String) = {
    Label.html(s"<b>${node.name()}</b><br/>$nodeValue")
  }

  private def buildCoreNode(coreEntity: CoreEntity, parameterNodeDatas: Seq[ParameterNodeData]): Node = {
    val baseNode = prettifyCoreNode(node(coreEntity.name))
    parameterNodeDatas.filter(_.parameter.config.arrowDirection == In)
      .foldLeft(baseNode)((n, p) => linkNodesToCoreNode(n, p.nodes))
  }

  private def linkNodesToCoreNode(baseNode: Node, otherNodes: Seq[Node]): Node =
    otherNodes.foldLeft(baseNode)((n, p) => n.link(p))

  private def prettifyCoreNode(node: Node): Node =
    node.`with`(Style.FILLED, ColorCoreNode)
}