import io.circe.parser._
import puppet_to_diagram._
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.io.Source
import scala.language.postfixOps
import scala.sys.process._

object Program {

  def run(cliOptions: CliOptions): Unit = {
    val generalConfigSource = ConfigSource.file(cliOptions.generalConfigPath)
    val diagramConfigSource = ConfigSource.file(cliOptions.diagramConfigPath.get)

    val configEither = generalConfigSource.withFallback(diagramConfigSource).load[Config]
    if (configEither.isLeft) {
      System.err.println(s"failed to load configuration: ${configEither.left.get}")
      System.exit(1)
    }

    val config = configEither.right.get

    val initPath = s"${config.pathToPuppetProject}/environments/${cliOptions.environment}/modules/${config.module}/manifests/init.pp"
    val jsonString = s"puppet strings generate --format json $initPath" !!

    val commonYamlPath = s"${config.pathToPuppetProject}/environments/${cliOptions.environment}/data/common.yaml"
    val commonYamlString = getYamlString(commonYamlPath)

    val serverName = cliOptions.server.get
    val serverYamlPath = s"${config.pathToPuppetProject}/environments/${cliOptions.environment}/data/nodes/$serverName.yaml"
    val serverYamlString = getYamlString(serverYamlPath)

    val graph = for {
      json <- parse(jsonString)
      puppetClass = PuppetClass(config.module, json)
      node <- PuppetParser.generateCoreEntityFromHieraYamls(serverYamlString, commonYamlString, puppetClass, config.parametersConfigs, serverName)
    } yield CoreEntity.toGraphvizGraph(node)

    graph match {
      case Right(g) => GraphPrinter.createFile(g, s"${config.module}_$serverName.png")
      case Left(e)  => println("Something went wrong: " + e)
    }
  }

  private def getYamlString(yamlPath: String) = {
    val yamlFile = Source.fromFile(yamlPath)
    val yamlString = yamlFile.getLines.mkString("\n")
    yamlFile.close()
    yamlString
  }
}
