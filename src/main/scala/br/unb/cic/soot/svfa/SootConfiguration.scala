package br.unb.cic.soot.svfa

import scala.collection.JavaConverters._

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import sootup.core.inputlocation.AnalysisInputLocation
import sootup.java.core.language.JavaLanguage
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation
import sootup.java.core.JavaProject
import sootup.core.model.SootMethod


sealed trait CG 

case object CHA extends CG
case object SPARK_LIBRARY extends CG
case object SPARK extends CG

/**
 * Base class for all implementations
 * of SVFA algorithms.
 */
abstract class SootConfiguration {

  // protected var pointsToAnalysis: PointsToAnalysis = _

  def sootClassPath(): String

  def applicationClassPath(): List[String]

  def getEntryPoints(): List[SootMethod] = Nil

  def getIncludeList(): List[String]

  def createSceneTransform(): (String, Any)

  def configurePackages(): List[String] = List("cg", "wjtp")

  def beforeGraphConstruction(): scala.Unit = {}

  def afterGraphConstruction(): scala.Unit = {}

  def callGraph(): CG = SPARK

  def configureSoot() {
  }

  def pathToJCE(): String =
    System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar"

  def pathToRT(): String =
    System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"

}
