package br.unb.cic.soot
import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import sootup.core.jimple.common.stmt.Stmt

class HashmapTest extends JSVFATest {
  override def getClassName(): String = "samples.HashmapSample"

  override def getMainMethod(): String = "m"

  override def analyze(unit: Stmt): NodeType =
    unit.getPositionInfo.getStmtPosition.getFirstLine match {
        case 11 => SourceNode
        case 12 => SinkNode
        case _ => SimpleNode
    }
}
