package br.unb.cic.soot

import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import org.scalatest.FunSuite
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.stmt.{JAssignStmt, JInvokeStmt, Stmt}

class CallSiteMatch(var className: String = "", var mainMethod: String = "") extends JSVFATest {
  override def getClassName(): String = className

  override def getMainMethod(): String = mainMethod

  override def analyze(unit: Stmt): NodeType = {
    if (unit.isInstanceOf[JInvokeStmt]) {
      val invokeStmt = unit.asInstanceOf[JInvokeStmt]
      return analyzeInvokeExpr(invokeStmt.getInvokeExpr)
    }
    if (unit.isInstanceOf[JAssignStmt[Value, Value]]) {
      val assignStmt = unit.asInstanceOf[JAssignStmt[Value, Value]]
      if (assignStmt.getRightOp.isInstanceOf[AbstractInvokeExpr]) {
        val invokeExpr = assignStmt.getRightOp.asInstanceOf[AbstractInvokeExpr]
        return analyzeInvokeExpr(invokeExpr)
      }
    }
    SimpleNode
  }

  def analyzeInvokeExpr(exp: AbstractInvokeExpr) : NodeType =
    exp.getMethodSignature.getName match {
      case "source" => SourceNode
      case "sink"   => SinkNode
      case _        => SimpleNode
    }
}

class CallSiteMatchTestSuite extends FunSuite {
  test("in the class CallSiteMatch1 we should detect 1 conflict of a unopened callsite test case") {
    val svfa = new CallSiteMatch("samples.callSiteMatch.CallSiteMatch1", "main")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 1)
  }

  test("in the class CallSiteMatch2 we should detect 1 conflict of a unclosed callsite test case") {
    val svfa = new CallSiteMatch("samples.callSiteMatch.CallSiteMatch2", "main")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 1)
  }

  test("in the class CallSiteMatch3 we should detect 1 conflict of a unclosed and unopened " +
    "callsite test case") {
    val svfa = new CallSiteMatch("samples.callSiteMatch.CallSiteMatch3", "main")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 1)
  }

  ignore("in the class CallSiteMatch4 we should detect 2 conflict of a unclosed and unopened " +
    "callsite with a common method in between test case") {
    val svfa = new CallSiteMatch("samples.callSiteMatch.CallSiteMatch4", "main")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 2)
  }

  test("in the class CallSiteMatch5 we should detect 2 conflict of a balanced callsite test case") {
    val svfa = new CallSiteMatch("samples.callSiteMatch.CallSiteMatch5", "main")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 2)
  }
}
