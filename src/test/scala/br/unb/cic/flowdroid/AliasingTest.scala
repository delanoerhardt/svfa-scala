package br.unb.cic.flowdroid

import br.unb.cic.soot.graph.NodeType
import br.unb.cic.soot.graph._
import org.scalatest.FunSuite
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.stmt.{JAssignStmt, JInvokeStmt, Stmt}

class AliasingTest(var className: String = "", var mainMethod: String = "") extends FlowdroidSpec {
  override def getClassName(): String = className

  override def getMainMethod(): String = mainMethod

  override def analyze(unit: Stmt): NodeType = {
    unit match {
      case invokeStmt: JInvokeStmt =>
        return analyzeInvokeExpr(invokeStmt.getInvokeExpr)
      case assignStmt: JAssignStmt[Value, Value] =>
        assignStmt.getRightOp match {
          case invokeExpr: AbstractInvokeExpr =>
            return analyzeInvokeExpr(invokeExpr)
          case _ =>
        }
      case _ =>
    }
    SimpleNode
  }

  def analyzeInvokeExpr(exp: AbstractInvokeExpr): NodeType = {
    if (sourceList.contains(exp.getMethodSignature.toString)) {
      return SourceNode;
    } else if (sinkList.contains(exp.getMethodSignature.toString)) {
      return SinkNode;
    }
    SimpleNode
  }
}

class AliasingTestSuite extends FunSuite {
  test("in the class Aliasing1 we should detect 1 conflict of a simple aliasing test case") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing1", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 1)
  }

  test("in the class Aliasing2 we should not detect any conflict in this false positive test case") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing2", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 0)
 }

  test("in the class Aliasing3 we should not detect any conflict, but in Flowdroid this test case was not conclusive") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing3", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 0)
  }

  test("in the class Aliasing4 we should detect 2 conflict") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing4", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 2)
  }

  ignore("in the class Aliasing5 we should detect 1 conflict") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing5", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 1)
  }

  test("in the class Aliasing6 we should detect 7 conflicts") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing6", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 7)
  }

  ignore("in the class Aliasing7 we should detect 7 conflicts") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing7", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 7)
  }

  ignore("in the class Aliasing8 we should detect 8 conflicts") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing8", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 8)
  }

  ignore("in the class Aliasing9 we should detect 1 conflicts") {
    val svfa = new AliasingTest("securibench.micro.aliasing.Aliasing9", "doGet")
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 2)
  }
}
