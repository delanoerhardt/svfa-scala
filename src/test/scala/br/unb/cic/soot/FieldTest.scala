package br.unb.cic.soot

import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import org.scalatest.{BeforeAndAfter, FunSuite}
import sootup.core.jimple.common.stmt.Stmt

class FieldTest extends JSVFATest {
  override def getClassName(): String = "samples.FieldSample"

  override def getMainMethod(): String = "main"

  override def analyze(unit: Stmt): NodeType = {
    if (unit.getPositionInfo.getStmtPosition.getFirstLine == 6) {
      return SourceNode
    }
    if (unit.getPositionInfo.getStmtPosition.getFirstLine == 7 || unit.getPositionInfo.getStmtPosition.getFirstLine == 11) {
      return SinkNode
    }
    return SimpleNode
  }
}


class FieldTestSuite extends FunSuite with BeforeAndAfter {
  test("we should find exactly one conflict in the FieldSample analysis") {
    val svfa = new FieldTest()
    //    This
    svfa.buildSparseValueFlowGraph()
    assert(svfa.reportConflictsSVG().size == 2) // NOTE: We are not traversing the body of
    //       a method associated to a SinkNode.
  }
}
