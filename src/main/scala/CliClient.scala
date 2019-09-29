import java.io.File
import java.nio.file.{Path, Paths}


case class CliOptions(configPath: Path = Paths.get(System.getProperty("user.home"), ".puppet-to-diagram.conf"))

object CliClient extends App {

  val parser = new scopt.OptionParser[CliOptions]("puppet-to-diagram") {
    head("Puppet To Diagram", "0.1")

    opt[File]('c', "config")
      .action((c, cliOptions) => cliOptions.copy(configPath = c.toPath))
      .text("config file to load. default is \".puppet-to-diagram.conf\" in your user directory.")

    help("help").text("show this help info")
  }

  parser.parse(args, CliOptions()) match {
    case Some(options) => Program.run(options)
    case None =>
  }
}
