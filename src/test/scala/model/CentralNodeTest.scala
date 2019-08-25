package model

import org.specs2.mutable.Specification
import io.circe.literal._


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

  }
}
