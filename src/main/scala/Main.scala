import io.circe.parser._
import puppet_to_diagram.{Config, CoreEntity, GraphPrinter, In, Out, ParameterConfig, PuppetClass, PuppetParser}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.io.Source
import scala.language.postfixOps
import sys.process._

object Main extends App {

  val configEither = ConfigSource.default.load[Config]
  val config = configEither.right.get

  val initPath = s"${config.pathToPuppetProject}/environments/${config.environment}/modules/${config.module}/manifests/init.pp"
  val commonYamlPath = s"${config.pathToPuppetProject}/environments/${config.environment}/data/common.yaml"

  val jsonString = s"puppet strings generate --format json $initPath" !!

  val commonYamlFile = Source.fromFile(commonYamlPath)
  val commonYamlString = commonYamlFile.getLines.mkString("\n")
  commonYamlFile.close()

  val graph = for {
    json <- parse(jsonString)
    puppetClass = PuppetClass(config.module, json)
    node <- PuppetParser.generateCoreEntityFromHieraPuppetNodeYaml(commonYamlString, puppetClass, config.parametersConfigs)
  } yield CoreEntity.toGraphvizGraph(node)

  graph match {
    case Right(g) => GraphPrinter.createFile(g, s"${config.module}_${config.environment}.png")
  }
}
