package model

import java.io.File

import guru.nidi.graphviz.engine.{Format, Graphviz}
import org.specs2.mutable.Specification
import io.circe.literal._
import guru.nidi.graphviz.model.Factory._


class CentralNodeTest extends Specification {

  "CentralNode" should {
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
      val result = CentralNode.fromJson(input, List("dependency1", "dependency2"))

      val expected = CentralNode("class_name", List(
        OuterNode("dependency1", "https://pinnaple.io"),
        OuterNode("dependency2", "https://apples.com"),
      ))

      result must be equalTo Right(expected)
    }

    "dependecies as list" in {
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
              "dependency2": "[\n    \"https://apples.com\",\n    \"https://pears.co\"\n  ]"
            },
            "source": "blabla"
          }
        ],
        "data_types": [

        ]
      }
      """
      val result = CentralNode.fromJson(input, List("dependency1", "dependency2"))

      val expected = CentralNode("class_name", List(
        OuterNode("dependency1", "https://pinnaple.io"),
        OuterNode("dependency2", "https://apples.com"),
        OuterNode("dependency2", "https://pears.co"),
      ))

      result must be equalTo Right(expected)
    }.pendingUntilFixed("todo later")

    "toGraphvizGraph should generate correct graph" in {
      val input = CentralNode("class_name", List(
        OuterNode("dependency1", "https://pinnaple.io"),
        OuterNode("dependency2", "https://apples.com"),
      ))

      val expected = graph.`with`(
        node("class_name"),
        node("dependency1").link(node("class_name")),
        node("dependency2").link(node("class_name")),
      )

      val result = CentralNode.toGraphvizGraph(input)

      result must be equalTo expected

      Graphviz.fromGraph(result).render(Format.PNG).toFile(new File("result.png"))
      Graphviz.fromGraph(result).render(Format.PNG).toFile(new File("expected.png"))
      ok
    }

  }
}
