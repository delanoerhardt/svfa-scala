package br.unb.cic.soot.svfa

import sootup.core.jimple.basic.Local
import java.util.ArrayList
import sootup.core.jimple.common.stmt.Stmt

object LocalDefsHolder {
  type LocalDefs = Map[Local, ArrayList[Stmt]]

  def EmptyLocalDefs() = new ArrayList[Stmt]()
}
