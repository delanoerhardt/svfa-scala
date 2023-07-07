package br.unb.cic.soot

import br.unb.cic.soot.graph.{NodeType, SimpleNode, SinkNode, SourceNode}
import sootup.core.jimple.common.stmt.Stmt

class ConfluenceTest04 extends JSVFATest {
  override def getClassName(): String = "samples.fields.Confluence04"

  override def getMainMethod(): String = "main"

  override def analyze(unit: Stmt): NodeType = unit.getPositionInfo.getStmtPosition.getFirstLine match {
    case 15 => SourceNode
    case 11 => SinkNode
    case _  => SimpleNode
  }


}
