package br.unb.cic.soot

import br.unb.cic.soot.graph._
import org.scalatest.{BeforeAndAfter, FunSuite}

import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.stmt.{JAssignStmt, JInvokeStmt, Stmt}
class IfElseTest extends JSVFATest {
  override def getClassName(): String = "samples.IfElseScenario"

  override def getMainMethod(): String = "main"

  override def analyze(unit: Stmt): NodeType = {
    if (unit.isInstanceOf[JInvokeStmt]) {
      val invokeStmt = unit.asInstanceOf[JInvokeStmt]
      return analyzeInvokeStmt(invokeStmt.getInvokeExpr)
    }
    if (unit.isInstanceOf[JAssignStmt[Value, Value]]) {
      val assignStmt = unit.asInstanceOf[JAssignStmt[Value, Value]]
      if (assignStmt.getRightOp.isInstanceOf[AbstractInvokeExpr]) {
        val invokeStmt = assignStmt.getRightOp.asInstanceOf[AbstractInvokeExpr]
        return analyzeInvokeStmt(invokeStmt)
      }
    }
    return SimpleNode
  }

  def analyzeInvokeStmt(exp: AbstractInvokeExpr): NodeType =
    exp.getMethodSignature.getName match {
      case "source" => SourceNode
      case "sink" => SinkNode
      case _ => SimpleNode
    }
}
