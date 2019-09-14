package puppet_to_diagram

import org.specs2.mutable.Specification
import io.circe.literal._
import io.circe.yaml.{parser => yamlParser}


class PuppetParserTest extends Specification {

  "generateCoreEntityFromPuppetClassJson should generate correct object from simple JSON" in {
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
    val parameterConfig1 = ParameterConfig("dependency1", "Dependency 1", In)
    val parameterConfig2 = ParameterConfig("dependency2", "Dependency 2", Out)

    val result = PuppetParser.generateCoreEntityFromPuppetClassJson(input, List(
      parameterConfig1,
      parameterConfig2
    ))

    val expected = CoreEntity("class_name", List(
      Parameter(parameterConfig1, "Dependency 1", "pinnaple.io"),
      Parameter(parameterConfig2, "Dependency 2", "apples.com"),
    ))

    result must be equalTo Right(expected)
  }

  "generateCoreEntityFromPuppetClassJson should deal correctly with property with list value" in {
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
    val parameterConfig1 = ParameterConfig("dependency1", "Dependency 1", In)
    val parameterConfig2 = ParameterConfig("dependency2", "Dependency 2", Out)

    val result = PuppetParser.generateCoreEntityFromPuppetClassJson(input, List(
      parameterConfig1,
      parameterConfig2
    ))

    val expected = CoreEntity("class_name", List(
      Parameter(parameterConfig1, "Dependency 1", "pinnaple.io"),
      Parameter(parameterConfig2, "Dependency 2 1", "apples.com"),
      Parameter(parameterConfig2, "Dependency 2 2", "pears.co"),
    ))

    result must be equalTo Right(expected)
  }

  "generateCoreEntityFromHieraPuppetNodeJson should generate correct entity from Hiera Puppet Node and class definition (hiera with simple values)" in {

    val hieraNodeYaml = """classes:
                      |  - class_name
                      |
                      |class_name::dependency1: https://olives.pt""".stripMargin

    val classJson = json"""{
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

    val parameterConfig1 = ParameterConfig("dependency1", "Dependency 1", In)
    val parameterConfig2 = ParameterConfig("dependency2", "Dependency 2", Out)

    val hieraNodeJson = yamlParser.parse(hieraNodeYaml).right.get

    val puppetClass = PuppetClass("class_name", classJson)
    val result = PuppetParser.generateCoreEntityFromHieraPuppetNodeJson(hieraNodeJson, puppetClass, List(
      parameterConfig1,
      parameterConfig2
    ))

    val expected = CoreEntity("class_name", List(
      Parameter(parameterConfig1, "Dependency 1", "olives.pt"),
      Parameter(parameterConfig2, "Dependency 2 1", "apples.com"),
      Parameter(parameterConfig2, "Dependency 2 2", "pears.co"),
    ))

    result must be equalTo Right(expected)
  }

  // TODO
  //"generateCoreEntityFromHieraPuppetNodeJson should generate correct entity from Hiera Puppet Node and class definition (hiera with list values)"
}
