package br.unb.cic.soot.svfa

// Update
// import soot._
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation

import java.nio.file.Paths
import sootup.java.core.language.JavaLanguage
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation
import sootup.java.core.{JavaPackageName, JavaProject}
import sootup.java.core.types.JavaClassType


/**
 * Base class for all implementations
 * of SVFA algorithms.
 */
abstract class SVFA extends SootConfiguration {

  var svg = new br.unb.cic.soot.graph.Graph()

  def buildSparseValueFlowGraph() {
    configureSoot()
    beforeGraphConstruction()

    val pathToBinary = Paths.get(Paths.get("").toAbsolutePath.toString, "/src/test/java")

    val pathToLibs = new JavaClassPathAnalysisInputLocation(System.getProperty("java.home") + "/lib/rt.jar") //add rt.jar

    val inputLocation = new JavaSourcePathAnalysisInputLocation(pathToBinary.toString())


    val language = new JavaLanguage(9)

    val project = JavaProject.builder(language).addInputLocation(inputLocation).addInputLocation(pathToLibs).enableModules().build();

    val view = project.createView()

    val classType = project.getIdentifierFactory().getClassType("securibench.micro.aliasing.Aliasing1");

    val sootClass = view.getClass(classType)

    val cha = new ClassHierarchyAnalysisAlgorithm(view);

    val cg = cha.initialize();

    println(cg)

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
