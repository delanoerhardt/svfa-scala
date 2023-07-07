package br.unb.cic.soot
import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import sootup.core.jimple.common.stmt.Stmt

class MethodFieldTest extends JSVFATest {
  override def getClassName(): String = "samples.MethodFieldSample"

  override def getMainMethod(): String = "m"

  override def analyze(unit: Stmt): NodeType =
    unit.getPositionInfo.getStmtPosition.getFirstLine match {
      case 7 | 10 | 11 | 12 => SourceNode
      case 8 => SinkNode
      case _ => SimpleNode
    }
}
