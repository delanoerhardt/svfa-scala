package br.unb.cic.soot.svfa

import br.unb.cic.soot.graph.NodeType
import sootup.core.jimple.common.stmt.Stmt

// Update

trait SourceSinkDef {
  def analyze(unit: Stmt) : NodeType
}

