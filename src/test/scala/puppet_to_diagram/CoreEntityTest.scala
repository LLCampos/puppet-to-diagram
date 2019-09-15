package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.model.Factory._
import org.specs2.mutable.Specification


class CoreEntityTest extends Specification {

  "CoreEntity" should {
    "toGraphvizGraph should generate correct graph (one core entity, two properties)" in {
      val propertyConfig1 = ParameterConfig("dependency1", "Dependency 1", Out)
      val propertyConfig2 = ParameterConfig("dependency2", "Dependency 2", In)

      val input = CoreEntity("class_name", List(
        Parameter(propertyConfig1, "Dependency 1", ParameterString("pinnaple.io")),
        Parameter(propertyConfig2, "Dependency 2", ParameterString("apples.com")),
      ))

      val dependency2Node = node("Dependency 2")
        .`with`(Label.html("<b>Dependency 2</b><br/>apples.com"))
        .`with`(Style.FILLED, Color.rgb("D0F0FD"))

      val mainNode = node("class_name")
        .link(dependency2Node)
        .`with`(Style.FILLED, Color.rgb("FFE5CC"))

      val dependency1Node = node("Dependency 1")
        .`with`(Label.html("<b>Dependency 1</b><br/>pinnaple.io"))
        .link(mainNode)
        .`with`(Style.FILLED, Color.rgb("D0F0FD"))

      val expected = graph.`with`(
        mainNode,
        dependency1Node,
        dependency2Node,
      ).directed().nodeAttr().`with`(Shape.RECTANGLE)

      val result = CoreEntity.toGraphvizGraph(input)

      GraphPrinter.createFile(result, "result.png")
      GraphPrinter.createFile(expected, "expected.png")
      result must be equalTo expected
      ok
    }

    "toGraphvizGraph should generate correct graph (one core entity, two properties, one of them a list)" in {
      val propertyConfig1 = ParameterConfig("dependency1", "Dependency 1", Out)
      val propertyConfig2 = ParameterConfig("dependency2", "Dependency 2", In)

      val input = CoreEntity("class_name", List(
        Parameter(propertyConfig1, "Dependency 1", ParameterString("pinnaple.io")),
        Parameter(propertyConfig2, "Dependency 2", ParameterSeq(List("apples.com", "pears.co"))),
      ))

      val dependency21Node = node("Dependency 2 1")
        .`with`(Label.html("<b>Dependency 2 1</b><br/>apples.com"))
        .`with`(Style.FILLED, Color.rgb("D0F0FD"))

      val dependency22Node = node("Dependency 2 2")
        .`with`(Label.html("<b>Dependency 2 2</b><br/>pears.co"))
        .`with`(Style.FILLED, Color.rgb("D0F0FD"))

      val mainNode = node("class_name")
        .link(dependency21Node)
        .link(dependency22Node)
        .`with`(Style.FILLED, Color.rgb("FFE5CC"))

      val dependency1Node = node("Dependency 1")
        .`with`(Label.html("<b>Dependency 1</b><br/>pinnaple.io"))
        .link(mainNode)
        .`with`(Style.FILLED, Color.rgb("D0F0FD"))

      val expected = graph.`with`(
        mainNode,
        dependency1Node,
        dependency21Node,
        dependency22Node
      ).directed().nodeAttr().`with`(Shape.RECTANGLE)

      val result = CoreEntity.toGraphvizGraph(input)

      GraphPrinter.createFile(result, "result.png")
      GraphPrinter.createFile(expected, "expected.png")
      result must be equalTo expected
      ok
    }

  }
}
