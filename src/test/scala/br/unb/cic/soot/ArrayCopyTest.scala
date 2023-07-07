package br.unb.cic.soot

import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.stmt.{JAssignStmt, JInvokeStmt, Stmt}

class ArrayCopyTest extends JSVFATest {

  override def getClassName(): String = "samples.ArrayCopySample"
  override def getMainMethod(): String = "main"

  override def analyze(unit: Stmt): NodeType = {
    unit match {
      case invokeStmt: JInvokeStmt =>
        return analyzeInvokeStmt(invokeStmt.getInvokeExpr)
      case _ =>
    }
    unit match {
      case assignStmt: JAssignStmt[Value, Value] =>
        assignStmt.getRightOp match {
          case invokeStmt: AbstractInvokeExpr =>
            return analyzeInvokeStmt(invokeStmt)
          case _ =>
        }
      case _ =>
    }
    return SimpleNode
  }

  def analyzeInvokeStmt(exp: AbstractInvokeExpr) : NodeType =
    exp.getMethodSignature.getName match {
      case "source" => SourceNode
      case "sink"   => SinkNode
      case _        => SimpleNode
    }

}


