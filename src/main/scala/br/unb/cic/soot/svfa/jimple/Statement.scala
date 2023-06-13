package br.unb.cic.soot.svfa.jimple

import sootup.core.jimple.common.stmt.Stmt
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.jimple.common.stmt.JInvokeStmt
import sootup.core.jimple.basic.Value

abstract class Statement(val base: Stmt){}

case class AssignStmt(b: JAssignStmt[Value, Value]) extends Statement(b) {
  val stmt = b
}
case class InvokeStmt(b: Stmt) extends Statement(b) {
  val stmt = base.asInstanceOf[JInvokeStmt]
}
case class InvalidStmt(b: Stmt) extends Statement(b)

object Statement {
  def convert(base: Stmt): Statement = base match {
    case base: JAssignStmt[Value, Value] => AssignStmt(base)
    case base: JInvokeStmt => InvokeStmt(base)
    case _ => InvalidStmt(base)
  }
}
