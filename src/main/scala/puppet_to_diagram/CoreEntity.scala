package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.model.Factory._
import guru.nidi.graphviz.model.{Graph, Node}

case class CoreEntity(name: String, links: Seq[Parameter])
case class Parameter(config: ParameterConfig, showName: String, value: String)
case class ParameterConfig(rawName: String, prettyName: String, arrowDirection: ArrowDirection)

sealed trait ArrowDirection
object In extends ArrowDirection
object Out extends ArrowDirection

case class CoreNodeData(coreEntity: CoreEntity, node: Node, links: Seq[ParameterNodeData])
case class ParameterNodeData(parameter: Parameter, node: Node)

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
    val parameterNodeDatas = buildPropertyNodeDatas(coreEntity)
    CoreNodeData(
      coreEntity,
      buildCoreNode(coreEntity, parameterNodeDatas),
      parameterNodeDatas
    )
  }

  private def buildPropertyNodeDatas(coreEntity: CoreEntity): Seq[ParameterNodeData] =
    coreEntity.links.map(p => ParameterNodeData(p, buildPropertyNode(p, coreEntity)))

  private def buildPropertyNode(parameter: Parameter, coreEntity: CoreEntity): Node = {
    val parameterNode = prettifyPropertyNode(node(parameter.showName), parameter)
    if (parameter.config.arrowDirection == Out)
        parameterNode.link(node(coreEntity.name))
    else
        parameterNode
  }

  private def prettifyPropertyNode(node: Node, parameter: Parameter) =
    node
      .`with`(buildNodeLabelForProperty(parameter))
      .`with`(Style.FILLED, ColorPropertyNodes)

  private def buildNodeLabelForProperty(parameter: Parameter) = {
    Label.html(s"<b>${parameter.showName}</b><br/>${parameter.value}")
  }

  private def buildCoreNode(coreEntity: CoreEntity, parameterNodeDatas: Seq[ParameterNodeData]): Node = {
    val baseNode = prettifyCoreNode(node(coreEntity.name))
    parameterNodeDatas.filter(_.parameter.config.arrowDirection == In)
      .foldLeft(baseNode)((n, p) => n.link(p.node))
  }

  private def prettifyCoreNode(node: Node): Node =
    node.`with`(Style.FILLED, ColorCoreNode)
}