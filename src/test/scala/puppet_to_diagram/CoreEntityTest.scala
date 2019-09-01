package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Color, Label, Shape, Style}
import guru.nidi.graphviz.model.Factory._
import io.circe.literal._
import org.specs2.mutable.Specification


class CoreEntityTest extends Specification {

  "CoreEntity" should {
    "fromJson should generate correct object from simple JSON" in {
      val input = json"""{
        "puppet_classes": [
          {
            "name": "class_name",
            "file": "init.pp",
            "line": 1,
            "docstring": {
              "text": ""
            },
            "defaults": {
              "dependency1": "\"https://pinnaple.io\"",
              "variable1": "\"bananas\"",
              "dependency2": "\"https://apples.com\""
            },
            "source": "blabla"
          }
        ],
        "data_types": [

        ]
      }
      """
      val propertyConfig1 = PropertyConfig("dependency1", "Dependency 1", In)
      val propertyConfig2 = PropertyConfig("dependency2", "Dependency 2", Out)

      val result = CoreEntity.fromJson(input, List(
        propertyConfig1,
        propertyConfig2
      ))

      val expected = CoreEntity("class_name", List(
        Property(propertyConfig1, "Dependency 1", "pinnaple.io"),
        Property(propertyConfig2, "Dependency 2", "apples.com"),
      ))

      result must be equalTo Right(expected)
    }

    "fromJson should deal correctly with property with list value" in {
      val input = json"""{
        "puppet_classes": [
          {
            "name": "class_name",
            "file": "init.pp",
            "line": 1,
            "docstring": {
              "text": ""
            },
            "defaults": {
              "dependency1": "\"https://pinnaple.io\"",
              "variable1": "\"bananas\"",
              "dependency2": "[\n    \"https://apples.com\",\n    \"http://pears.co\"\n  ]"
            },
            "source": "blabla"
          }
        ],
        "data_types": [

        ]
      }
      """
      val propertyConfig1 = PropertyConfig("dependency1", "Dependency 1", In)
      val propertyConfig2 = PropertyConfig("dependency2", "Dependency 2", Out)

      val result = CoreEntity.fromJson(input, List(
        propertyConfig1,
        propertyConfig2
      ))

      val expected = CoreEntity("class_name", List(
        Property(propertyConfig1, "Dependency 1", "pinnaple.io"),
        Property(propertyConfig2, "Dependency 2 1", "apples.com"),
        Property(propertyConfig2, "Dependency 2 2", "pears.co"),
      ))

      result must be equalTo Right(expected)
    }

    "toGraphvizGraph should generate correct graph (one core entity, two properties)" in {
      val propertyConfig1 = PropertyConfig("dependency1", "Dependency 1", Out)
      val propertyConfig2 = PropertyConfig("dependency2", "Dependency 2", In)

      val input = CoreEntity("class_name", List(
        Property(propertyConfig1, "Dependency 1", "pinnaple.io"),
        Property(propertyConfig2, "Dependency 2", "apples.com"),
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

  }
}
