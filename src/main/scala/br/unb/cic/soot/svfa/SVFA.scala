package br.unb.cic.soot.svfa

// Update
// import soot._
import java.nio.file.Paths
import sootup.java.core.language.JavaLanguage
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation
import sootup.java.core.JavaProject


/**
 * Base class for all implementations
 * of SVFA algorithms.
 */
abstract class SVFA extends SootConfiguration {

  var svg = new br.unb.cic.soot.graph.Graph()

  def buildSparseValueFlowGraph() {
    configureSoot()
    beforeGraphConstruction()
    val (pack, t) = createSceneTransform()
    
    val pathToBinary = Paths.get("")
    val inputLocation = new JavaSourcePathAnalysisInputLocation(pathToBinary.toString());

    val language = new JavaLanguage(9)

    val project = JavaProject.builder(language).addInputLocation(inputLocation).build();

    val view = project.createView()

    val classType = project.getIdentifierFactory().getClassType("example.HelloWorld");

    // PackManager.v().getPack(pack).add(t)
    // configurePackages().foreach(p => PackManager.v().getPack(p).apply())
    afterGraphConstruction()
  }

  def svgToDotModel(): String = {
    svg.toDotModel()
  }

  def reportConflictsSVG() = {
    svg.reportConflicts()
  }

}
