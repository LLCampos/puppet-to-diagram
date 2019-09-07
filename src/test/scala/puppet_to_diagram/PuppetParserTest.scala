package puppet_to_diagram

import org.specs2.mutable.Specification
import io.circe.literal._


class PuppetParserTest extends Specification {

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

    val result = PuppetParser.coreEntityFromJson(input, List(
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

    val result = PuppetParser.coreEntityFromJson(input, List(
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
}
