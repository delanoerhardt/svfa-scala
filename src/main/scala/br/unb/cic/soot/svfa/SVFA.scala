package br.unb.cic.soot.svfa

import sootup.callgraph.ClassHierarchyAnalysisAlgorithm
import sootup.core.signatures.MethodSignature
import sootup.core.types.{Type, VoidType}
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation

import java.nio.file.Paths
import sootup.java.core.language.JavaLanguage
import sootup.java.core.{JavaPackageName, JavaProject}
import sootup.java.core.types.JavaClassType

import java.util


/**
 * Base class for all implementations
 * of SVFA algorithms.
 */
abstract class SVFA extends SootConfiguration {

  var svg = new br.unb.cic.soot.graph.Graph()

  def buildSparseValueFlowGraph() {
    configureSoot()
    beforeGraphConstruction()

    // Needed to avoid internal errors in sootup, remove after reading properties from wala.properties is fixed
    System.setProperty("java.specification.version", "1.8")
    System.setProperty("sun.boot.class.path", "C:\\Program Files\\Java\\jre-1.8\\lib\\rt.jar")

    // These properties also set up Primordial class loading so that the following is not needed
    //    val pathToLibs = new JavaClassPathAnalysisInputLocation("C:\\Program Files\\Java\\jre-1.8\\lib\\rt.jar") //add rt.jar
    // ...JavaProject.builder(language).addInputLocation(pathToLibs)...

    val pathToBinary = Paths.get(Paths.get("").toAbsolutePath.toString, "/src/test/out.jar")
    val binInputLocation = new JavaClassPathAnalysisInputLocation(pathToBinary.toString())

//    val pathToSrc = Paths.get(Paths.get("").toAbsolutePath.toString, "/src/test/java")
//    val srcInputLocation = new JavaSourcePathAnalysisInputLocation(pathToSrc.toString())

    val language = new JavaLanguage(9)

    val project = JavaProject.builder(language).addInputLocation(binInputLocation).enableModules().build();

    val view = project.createView()

    val classType = project.getIdentifierFactory().getClassType("securibench.micro.aliasing.Aliasing1");

    println(classType)

    val sootClass = view.getClass(classType)

    println(sootClass)

    val cha = new ClassHierarchyAnalysisAlgorithm(view);

    val arguments = new util.ArrayList[Type]()
    arguments.add(new JavaClassType("HttpServletRequest", new JavaPackageName("javax.servlet.http")))
    arguments.add(new JavaClassType("HttpServletResponse", new JavaPackageName("javax.servlet.http")))


    val methodSignature: MethodSignature = new MethodSignature(classType, "doGet", arguments, VoidType.getInstance())

    val entryPoints: util.List[MethodSignature] = new util.ArrayList[MethodSignature]()

    entryPoints.add(methodSignature)

    val cg = cha.initialize(entryPoints);

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
