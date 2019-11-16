package puppet_to_diagram

import org.specs2.mutable.Specification
import io.circe.literal._


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
              "dependency2": "\"https://apples.com\"",
              "dependency3": "localhost"
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
    val parameterConfig3 = ParameterConfig("dependency3", "Dependency 3", Out)

    val result = PuppetParser.generateCoreEntityFromPuppetClassJson(input, List(
      parameterConfig1,
      parameterConfig2,
      parameterConfig3
    ), "server.com")

    val expected = CoreEntity("server.com", List(
      Parameter(parameterConfig1, "Dependency 1", ParameterString("pinnaple.io")),
      Parameter(parameterConfig2, "Dependency 2", ParameterString("apples.com")),
      Parameter(parameterConfig3, "Dependency 3", ParameterString("localhost")),
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
    ), "server.com")

    val expected = CoreEntity("server.com", List(
      Parameter(parameterConfig1, "Dependency 1", ParameterString("pinnaple.io")),
      Parameter(parameterConfig2, "Dependency 2", ParameterSeq(List("apples.com", "pears.co")),
    )))

    result must be equalTo Right(expected)
  }

  "generateCoreEntityFromHieraYamls should generate correct entity from Hiera Puppet Node and class definition (hiera with simple values)" in {

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

    val puppetClass = PuppetClass("class_name", classJson)
    val result = PuppetParser.generateCoreEntityFromHieraYamls(hieraNodeYaml, "", puppetClass, List(
      parameterConfig1,
      parameterConfig2
    ), "server.com")

    val expected = CoreEntity("server.com", Seq(
      Parameter(parameterConfig1, "Dependency 1", ParameterString("olives.pt")),
      Parameter(parameterConfig2, "Dependency 2", ParameterSeq(Seq("apples.com", "pears.co"))
    )))

    result must be equalTo Right(expected)
  }

  "generateCoreEntityFromHieraYamls should generate correct entity from Hiera Puppet Node and class definition (hiera with list values)" in {

    val hieraNodeYaml = """classes:
                          |  - class_name
                          |
                          |class_name::dependency2:
                          |   - https://olives.pt
                          |   - http://avocado.org""".stripMargin

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

    val puppetClass = PuppetClass("class_name", classJson)
    val result = PuppetParser.generateCoreEntityFromHieraYamls(hieraNodeYaml, "", puppetClass, List(
      parameterConfig1,
      parameterConfig2
    ), "server.com")

    val expected = CoreEntity("server.com", Seq(
      Parameter(parameterConfig1, "Dependency 1", ParameterString("pinnaple.io")),
      Parameter(parameterConfig2, "Dependency 2", ParameterSeq(Seq("olives.pt", "avocado.org"))
      )))

    result must be equalTo Right(expected)
  }

  "generateCoreEntityFromHieraYamls should generate correct entity from Hiera Puppet Node, Hiera Common and class definition" in {

    val hieraNodeYaml = """classes:
                          |  - class_name
                          |
                          |class_name::dependency2:
                          |   - https://oatmeal.pt
                          |   - http://oranges.org""".stripMargin

    val hieraCommonYaml = """classes:
                          |  - class_name
                          |
                          |class_name::dependency2:
                          |   - https://olives.pt
                          |   - http://avocado.org
                          |class_name::dependency1: http://peanuts.org""".stripMargin

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

    val puppetClass = PuppetClass("class_name", classJson)
    val result = PuppetParser.generateCoreEntityFromHieraYamls(hieraNodeYaml, hieraCommonYaml, puppetClass, List(
      parameterConfig1,
      parameterConfig2
    ), "server.com")

    val expected = CoreEntity("server.com", Seq(
      Parameter(parameterConfig1, "Dependency 1", ParameterString("peanuts.org")),
      Parameter(parameterConfig2, "Dependency 2", ParameterSeq(Seq("oatmeal.pt", "oranges.org"))
      )))

    result must be equalTo Right(expected)
  }
}
