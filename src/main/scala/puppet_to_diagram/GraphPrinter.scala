package puppet_to_diagram

import java.io.File

import guru.nidi.graphviz.engine.{Engine, Format, Graphviz}
import guru.nidi.graphviz.model.Graph

object GraphPrinter {
  def createFile(graph: Graph, fileName: String): Unit = {
    Graphviz.fromGraph(graph)
      .fontAdjust(0.80)
      .engine(Engine.CIRCO)
      .render(Format.PNG).toFile(new File(fileName))
  }
}
