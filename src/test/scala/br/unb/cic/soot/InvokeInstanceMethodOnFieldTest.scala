package br.unb.cic.soot

import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import sootup.core.jimple.common.stmt.Stmt

class InvokeInstanceMethodOnFieldTest extends JSVFATest {
  override def getClassName(): String = "samples.InvokeInstanceMethodOnFieldSample"

  override def getMainMethod(): String = "m"

  override def analyze(unit: Stmt): NodeType = unit.getPositionInfo.getStmtPosition.getFirstLine match {
      case 16 => SourceNode
      case 18 => SinkNode
      case _ => SimpleNode
    }
}
