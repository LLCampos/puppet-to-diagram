import io.circe.parser._
import puppet_to_diagram._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.io.Source
import scala.language.postfixOps
import scala.sys.process._

object Program {

  def run(cliOptions: CliOptions): Unit = {
    val configEither = ConfigSource.file(cliOptions.configPath).load[Config]
    if (configEither.isLeft) {
      System.err.println(s"failed to load configuration: ${configEither.left.get}")
      System.exit(1)
    }

    val config = configEither.right.get

    val initPath = s"${config.pathToPuppetProject}/environments/${cliOptions.environment}/modules/${config.module}/manifests/init.pp"
    val commonYamlPath = s"${config.pathToPuppetProject}/environments/${cliOptions.environment}/data/common.yaml"

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
      case Right(g) => GraphPrinter.createFile(g, s"${config.module}_${cliOptions.environment}.png")
      case Left(e)  => println("Something went wrong: " + e)
    }
  }
}
