package puppet_to_diagram

import guru.nidi.graphviz.attribute.{Label, Shape}
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
      val result = CoreEntity.fromJson(input, List("dependency1", "dependency2"))

      val expected = CoreEntity("class_name", List(
        Properties("dependency1", "pinnaple.io"),
        Properties("dependency2", "apples.com"),
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
      val result = CoreEntity.fromJson(input, List("dependency1", "dependency2"))

      val expected = CoreEntity("class_name", List(
        Properties("dependency1", "pinnaple.io"),
        Properties("dependency2 1", "apples.com"),
        Properties("dependency2 2", "pears.co"),
      ))

      result must be equalTo Right(expected)
    }

    "toGraphvizGraph should generate correct graph (one core entity, two properties)" in {
      val input = CoreEntity("class_name", List(
        Properties("dependency1", "pinnaple.io"),
        Properties("dependency2", "apples.com"),
      ))

      val mainNode = node("class_name")
        .`with`(Shape.RECTANGLE)

      val dependency1Node = node("dependency1")
        .`with`(Label.html("<b>dependency1</b><br/>pinnaple.io"))
        .`with`(Shape.RECTANGLE)
        .link(node("class_name"))

      val dependency2Node = node("dependency2")
        .`with`(Label.html("<b>dependency2</b><br/>apples.com"))
        .`with`(Shape.RECTANGLE)
        .link(node("class_name"))

      val expected = graph.`with`(
        mainNode,
        dependency1Node,
        dependency2Node,
      ).directed()

      val result = CoreEntity.toGraphvizGraph(input)

      GraphPrinter.createFile(result, "result.png")
      GraphPrinter.createFile(expected, "expected.png")
      result must be equalTo expected
      ok
    }

  }
}
