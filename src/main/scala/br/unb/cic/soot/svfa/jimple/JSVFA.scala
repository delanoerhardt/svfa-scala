package br.unb.cic.soot.svfa.jimple

import br.unb.cic.soot.graph._
import br.unb.cic.soot.svfa.jimple.dsl.{DSL, LanguageParser}
import br.unb.cic.soot.svfa.jimple.rules.RuleAction
import br.unb.cic.soot.svfa.{SVFA, SourceSinkDef}
import com.typesafe.scalalogging.LazyLogging

import br.unb.cic.soot.svfa.LocalDefsHolder._
import sootup.core.jimple._
import sootup.core.jimple.basic.{Local, Value}
import sootup.core.jimple.common.constant.StringConstant
import sootup.core.jimple.common.expr._
import sootup.core.jimple.common.ref._
import sootup.core.jimple.common.stmt.{JAssignStmt, JIdentityStmt, JReturnStmt, Stmt}
import sootup.core.model.{Body, SootMethod}
import sootup.core.signatures.FieldSignature
import sootup.core.types.ArrayType

import scala.collection.mutable.ListBuffer

/**
 * A Jimple based implementation of
 * SVFA.
 */
abstract class JSVFA extends SVFA with Analysis with FieldSensitiveness with ObjectPropagation with SourceSinkDef with LazyLogging with DSL {
  val traversedMethods = scala.collection.mutable.Set.empty[SootMethod]
  val allocationSites = scala.collection.mutable.HashMap.empty[Value, StatementNode]
  val arrayStores = scala.collection.mutable.HashMap.empty[basic.Local, List[Stmt]]
  val languageParser = new LanguageParser(this)
  val methodRules = languageParser.evaluate(code())
  var methods = 0

  def initAllocationSites(): Unit = {
    // TODO: this seems to just loop over Stmt on method bodies. I can't find any
    // equivalent to getReachableMethods in SootUp*, so maybe we should just loop
    // over all methods.

    // *  resolveCall in AbstractCallGraphAlgorithm extensions seems to be the
    // closest, but it is protected and takes on an InvokeStmt

    // val listener = Scene.v().getReachableMethods.listener()

    // while(listener.hasNext) {
    //   val m = listener.next().method()
    //   if (m.hasActiveBody) {
    //     val body = m.getActiveBody
    //     body.getUnits.forEach(unit => {
    //       if (unit.isInstanceOf[JAssignStmt]) {
    //         val right = unit.asInstanceOf[JAssignStmt].getRightOp
    //         if (right.isInstanceOf[NewExpr] || right.isInstanceOf[NewArrayExpr] || right.isInstanceOf[StringConstant]) {
    //           allocationSites += (right -> createNode(m, unit))
    //         }
    //       }
    //       else if(unit.isInstanceOf[soot.jimple.JReturnStmt]) {
    //         val exp = unit.asInstanceOf[soot.jimple.JReturnStmt].getOp
    //         if(exp.isInstanceOf[StringConstant]) {
    //           allocationSites += (exp -> createNode(m, unit))
    //         }
    //       }
    //     })
    //   }
    // }
  }

  override def afterGraphConstruction(): Unit = {
    initAllocationSites()
    getEntryPoints().foreach(method => {
      traverse(method)
      methods = methods + 1
    })
  }

  def traverse(assignStmt: AssignStmt, method: SootMethod, defs: LocalDefs): Unit = {
    val left = assignStmt.stmt.getLeftOp
    val right = assignStmt.stmt.getRightOp

    (left, right) match {
      case (_: Local, q: JInstanceFieldRef) => loadRule(assignStmt.stmt, q, method, defs)
      case (_: Local, q: JArrayRef) => loadArrayRule(assignStmt.stmt, q, method, defs)
      case (_: Local, q: AbstractInvokeExpr) => invokeRule(assignStmt, q, method, defs)
      case (_: Local, q: Local) => copyRule(assignStmt.stmt, q, method, defs)
      case (_: Local, _) => copyRuleInvolvingExpressions(assignStmt.stmt, method, defs)
      case (p: JInstanceFieldRef, _: Local) => storeRule(assignStmt.stmt, p, method, defs)
      case (_: JArrayRef, _) => storeArrayRule(assignStmt)
      case _ =>
    }
  }

  def traverse(stmt: InvokeStmt, method: SootMethod, defs: LocalDefs): Unit = {
    val exp = stmt.stmt.getInvokeExpr
    invokeRule(stmt, exp, method, defs)
  }

  def traverseSinkStatement(statement: Statement, method: SootMethod, defs: LocalDefs): Unit = {
    statement.base.getUses.forEach {
      case local: Local => copyRule(statement.base, local, method, defs)
      case fieldRef: JInstanceFieldRef => loadRule(statement.base, fieldRef, method, defs)
      case _ =>
      // TODO:
      //   we have to think about other cases here.
      //   e.g: a reference to a parameter
    }
  }

  /*
   * This rule deals with the following situation:
   *
   * (*) p = q + r
   *
   * In this case, we create and edge from defs(q) and
   * from defs(r) to the statement p = q + r
   */
  def copyRuleInvolvingExpressions(stmt: JAssignStmt[Value, Value], method: SootMethod, defs: LocalDefs): Unit = {
    stmt.getRightOp.getUses.forEach {
      case local: Local =>
        copyRule(stmt, local, method, defs)
      case _ =>
    }
  }

  def storeArrayRule(assignStmt: AssignStmt): Unit = {
    val l = assignStmt.stmt.getLeftOp.asInstanceOf[JArrayRef].getBase
    val stores = assignStmt.stmt :: arrayStores.getOrElseUpdate(l, List())
    arrayStores.put(l, stores)
  }

  def createCSOpenLabel(method: SootMethod, stmt: Stmt, callee: SootMethod): CallSiteLabel = {
    val statement = br.unb.cic.soot.graph
                      .Statement(method.getDeclaringClassType.toString, method.toString, stmt.toString,
                                 stmt.getPositionInfo.getStmtPosition.getFirstLine, stmt, method)
    CallSiteLabel(ContextSensitiveRegion(statement, callee.toString), CallSiteOpenLabel)
  }

  def createCSCloseLabel(method: SootMethod, stmt: Stmt, callee: SootMethod): CallSiteLabel = {
    val statement = br.unb.cic.soot.graph
                      .Statement(method.getDeclaringClassType.toString, method.toString, stmt.toString,
                                 stmt.getPositionInfo.getStmtPosition.getFirstLine, stmt, method)
    CallSiteLabel(ContextSensitiveRegion(statement, callee.toString), CallSiteCloseLabel)
  }

  def isThisInitStmt(expr: AbstractInvokeExpr, unit: Stmt): Boolean =
    unit.isInstanceOf[JIdentityStmt[IdentityRef]] &&
      unit.asInstanceOf[JIdentityStmt[IdentityRef]].getRightOp.isInstanceOf[JThisRef]

  def isParameterInitStmt(expr: AbstractInvokeExpr, pmtCount: Int, unit: Stmt): Boolean =
    unit.isInstanceOf[JIdentityStmt[IdentityRef]] &&
      unit.asInstanceOf[JIdentityStmt[IdentityRef]].getRightOp.isInstanceOf[JParameterRef] &&
      expr.getArg(pmtCount).isInstanceOf[Local]

  def isAssignReturnLocalStmt(callSite: Stmt, unit: Stmt): Boolean =
    unit.isInstanceOf[JReturnStmt] && unit.asInstanceOf[JReturnStmt].getOp.isInstanceOf[Local] &&
      callSite.isInstanceOf[JAssignStmt[Value, Value]]

  def isReturnStringStmt(callSite: Stmt, unit: Stmt): Boolean =
    unit.isInstanceOf[JReturnStmt] && unit.asInstanceOf[JReturnStmt].getOp.isInstanceOf[StringConstant] &&
      callSite.isInstanceOf[JAssignStmt[Value, Value]]

  def findFieldStores(local: Local, field: FieldSignature): ListBuffer[GraphNode] = {
    val res: ListBuffer[GraphNode] = new ListBuffer[GraphNode]()
    
    for (node <- svg.nodes()) {
      node.stmt() match {
        case assignment: JAssignStmt[Value, Value] =>
          assignment.getLeftOp match {
            case ref: JInstanceFieldRef =>
              val base = ref.getBase
            // if(pointsToAnalysis.reachingObjects(base).hasNonEmptyIntersection(pointsToAnalysis.reachingObjects(local))) {
            //   if(field.equals(assignment.getLeftOp.asInstanceOf[JFieldRef].getField)) {
            //     res += createNode(node.method(), node.unit())
            //   }
            // }
            case _ =>
          }
        case _ =>
      }
    }

    res
  }

  /*
     * This rule deals with the following situation:
     *
     * (*) p = q
     *
     * In this case, we create an edge from defs(q)
     * to the statement p = q.
     */
  protected def copyRule(targetStmt: Stmt, local: Local, method: SootMethod, defs: LocalDefs): Unit = {
    defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
      val source = createNode(method, sourceStmt)
      val target = createNode(method, targetStmt)
      updateGraph(source, target)
    })
  }

  /*
   * This rule deals with the following situations:
   *
   *  (*) p = q.f
   */
  protected def loadRule(stmt: Stmt, ref: JInstanceFieldRef, method: SootMethod, defs: LocalDefs): Unit = {
    val base = ref.getBase
    // value field of a string.
    val className = ref.getFieldSignature.getDeclClassType.getClassName
    if ((className == "java.lang.String") && ref.getFieldSignature.getName == "value") {
      base match {
        case local: Local =>
          defs.getOrElse(local, EmptyLocalDefs()).forEach(source => {
            val sourceNode = createNode(method, source)
            val targetNode = createNode(method, stmt)
            updateGraph(sourceNode, targetNode)
          })
        case _ =>
      }
      return;
    }
    // default case
    base match {
      case local: Local =>
        var allocationNodes = findAllocationSites(local, false, ref.getFieldSignature)

        if (allocationNodes.isEmpty) {
          allocationNodes = findAllocationSites(local, true, ref.getFieldSignature)
        }

        if (allocationNodes.isEmpty) {
          allocationNodes = findFieldStores(local, ref.getFieldSignature)
        }

        allocationNodes.foreach(source => {
          val target = createNode(method, stmt)
          updateGraph(source, target)
          svg.getAdjacentNodes(source).get.foreach(s => updateGraph(s, target))
        })

        // create an edge from the base defs to target
        // if an object is tainted, we should propagate the taint to all
        // fields as well. Not completely sure if this should be
        // the case.
        if (propagateObjectTaint()) {
          defs.getOrElse(local, EmptyLocalDefs()).forEach(source => {
            val sourceNode = createNode(method, source)
            val targetNode = createNode(method, stmt)
            updateGraph(sourceNode, targetNode)
          })
        }
      case _ =>
    }
  }

  protected def loadArrayRule(targetStmt: Stmt, ref: JArrayRef, method: SootMethod, defs: LocalDefs): Unit = {
    val base = ref.getBase

    base match {
      case local: Local =>

        defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
          val source = createNode(method, sourceStmt)
          val target = createNode(method, targetStmt)
          updateGraph(source, target)
        })

        val stores = arrayStores.getOrElseUpdate(local, List())
        stores.foreach(sourceStmt => {
          val source = createNode(method, sourceStmt)
          val target = createNode(method, targetStmt)
          updateGraph(source, target)
        })
      case _ =>
    }
  }

  private def getBaseObject(expr: AbstractInvokeExpr): Local =
    expr match {
      case invokeExpr: JVirtualInvokeExpr => invokeExpr.getBase
      case invokeExpr: JSpecialInvokeExpr => invokeExpr.getBase
      case _ => expr.asInstanceOf[AbstractInstanceInvokeExpr].getBase
    }

  private def hasBaseObject(expr: AbstractInvokeExpr) =
    expr.isInstanceOf[JVirtualInvokeExpr] || expr.isInstanceOf[JSpecialInvokeExpr] ||
      expr.isInstanceOf[JInterfaceInvokeExpr]

  private def traverse(method: SootMethod, forceNewTraversal: Boolean = false): Unit = {
    // TODO: method.isPhantom has no equivalent
    //if((!forceNewTraversal) && (method.isPhantom || traversedMethods.contains(method))) {
    if ((!forceNewTraversal) && traversedMethods.contains(method)) {
      return
    }

    traversedMethods.add(method)

    val body = method.getBody()

    val graph = body.getStmtGraph()
    val defs = Body.collectDefs(body.getStmts()).asInstanceOf[LocalDefs]

    body.getStmts.forEach(stmt => {
      val v = Statement.convert(stmt)

      v match {
        case AssignStmt(base) => traverse(AssignStmt(base), method, defs)
        case InvokeStmt(base) => traverse(InvokeStmt(base), method, defs)
        case _ if analyze(stmt) == SinkNode => traverseSinkStatement(v, method, defs)
        case _ =>
      }
    })
  }

  private def invokeRule(callStmt: Statement, exp: AbstractInvokeExpr, caller: SootMethod, defs: LocalDefs): Unit = {
    val callee = exp.getMethodSignature

    if (analyze(callStmt.base) == SinkNode) {
      defsToCallOfSinkMethod(callStmt, exp, caller, defs)
      return // TODO: we are not exploring the body of a sink method.
      //       For this reason, we only find one path in the
      //       FieldSample test case, instead of two.
    }

    if (analyze(callStmt.base) == SourceNode) {
      val source = createNode(caller, callStmt.base)
      svg.addNode(source)
    }

    // TODO method rules
    //    for(r <- methodRules) {
    //      if(r.check(callee)) {
    //        r.apply(caller, callStmt.base, defs)
    //        return
    //      }
    //    }

    if (intraprocedural()) return

    var pmtCount = 0
    // TODO find where we can read method bodies from
//    val body: Body = callee.retrieveActiveBody()
//    val g = body.getStmtGraph()
//    val calleeDefs = Body.collectDefs(body.getStmts())
//
//    body.getStmts.forEach(s => {
//      if (isThisInitStmt(exp, s)) {
//        defsToThisObject(callStmt, caller, defs, s, exp, callee)
//      }
//      else if (isParameterInitStmt(exp, pmtCount, s)) {
//        defsToFormalArgs(callStmt, caller, defs, s, exp, callee, pmtCount)
//        pmtCount = pmtCount + 1
//      }
//      else if (isAssignReturnLocalStmt(callStmt.base, s)) {
//        defsToCallSite(caller, callee, calleeDefs, callStmt.base, s)
//      }
//      else if (isReturnStringStmt(callStmt.base, s)) {
//        stringToCallSite(caller, callee, callStmt.base, s)
//      }
//    })
//
//    // TODO use method instead of signature
//    traverse(callee)
  }

  private def applyPhantomMethodCallRule(callStmt: Statement, exp: AbstractInvokeExpr, caller: SootMethod,
                                         defs: LocalDefs): Unit = {
    val srcArg = exp.getArg(0)
    val destArg = exp.getArg(2)
    (srcArg, destArg) match {
      case (local: Local, localDestArg: Local) =>
        defs.getOrElse(local, EmptyLocalDefs()).forEach(srcArgDefStmt => {
          val sourceNode = createNode(caller, srcArgDefStmt)
          val allocationNodes = findAllocationSites(localDestArg)
          allocationNodes.foreach(targetNode => {
            updateGraph(sourceNode, targetNode)
          })
        })
      case _ =>
    }
  }

  /*
   * creates a graph node from a sootMethod / sootUnit
   */
  def createNode(method: SootMethod, stmt: Stmt): StatementNode =
    svg.createNode(method, stmt, analyze)

  def findAllocationSites(local: Local, oldSet: Boolean = true, field: FieldSignature = null): ListBuffer[GraphNode] = {
    // val pta = if(pointsToAnalysis.isInstanceOf[PAG]) pointsToAnalysis.asInstanceOf[PAG]
    // else if (pointsToAnalysis.isInstanceOf[DemandCSPointsTo]) pointsToAnalysis.asInstanceOf[DemandCSPointsTo].getPAG
    // else null

    // val pta = null

    // if(pta != null) {
    //   val reachingObjects = if(field == null) pta.reachingObjects(local.asInstanceOf[Local])
    //   else pta.reachingObjects(local, field)

    //   if(!reachingObjects.isEmpty) {
    //     val allocations = if(oldSet) reachingObjects.asInstanceOf[DoublePointsToSet].getOldSet
    //     else reachingObjects.asInstanceOf[DoublePointsToSet].getNewSet

    //     val v = new AllocationVisitor()
    //     allocations.asInstanceOf[HybridPointsToSet].forall(v)
    //     return v.allocationNodes
    //   }
    // }
    new ListBuffer[GraphNode]()
  }

  def updateGraph(source: GraphNode, target: GraphNode, forceNewEdge: Boolean = false): Boolean = {
    var res = false
    if (!runInFullSparsenessMode() || true) {
      addNodeAndEdgeDF(source.asInstanceOf[StatementNode], target.asInstanceOf[StatementNode])

      res = true
    }

    res
  }

  /**
   * Override this method in the case that
   * a complete graph should be generated.
   *
   * Otherwise, only nodes that can be reached from
   * source nodes will be in the graph
   *
   * @return true for a full sparse version of the graph.
   *         false otherwise.
   * @deprecated
   */
  def runInFullSparsenessMode() = true

  def addNodeAndEdgeDF(from: StatementNode, to: StatementNode): Unit = {
    val auxNodeFrom = containsNodeDF(from)
    val auxNodeTo = containsNodeDF(to)
    if (auxNodeFrom != null) {
      if (auxNodeTo != null) {
        svg.addEdge(auxNodeFrom, auxNodeTo)
      } else {
        svg.addEdge(auxNodeFrom, to)
      }
    } else {
      if (auxNodeTo != null) {
        svg.addEdge(from, auxNodeTo)
      } else {
        svg.addEdge(from, to)
      }
    }
  }

  def containsNodeDF(node: StatementNode): StatementNode = {
    for (n <- svg.edges()) {
      val auxNodeFrom = n.from.asInstanceOf[StatementNode]
      val auxNodeTo = n.to.asInstanceOf[StatementNode]
      if (auxNodeFrom.equals(node)) return n.from.asInstanceOf[StatementNode]
      if (auxNodeTo.equals(node)) return n.to.asInstanceOf[StatementNode]
    }

    null
  }

  /*
   * This rule deals with statements in the form:
   *
   * (*) p.f = expression
   */
  private def storeRule(targetStmt: JAssignStmt[Value, Value], fieldRef: JInstanceFieldRef, method: SootMethod,
                        defs: LocalDefs): Unit = {
    val local = targetStmt.getRightOp.asInstanceOf[Local]
    fieldRef.getBase match {
      case base: Local =>
        if (fieldRef.getFieldSignature.getDeclClassType.getClassName == "java.lang.String" &&
          fieldRef.getFieldSignature.getName == "value") {
          defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
            val source = createNode(method, sourceStmt)
            val allocationNodes = findAllocationSites(base)
            allocationNodes.foreach(targetNode => {
              updateGraph(source, targetNode)
            })
          })
        } else {
          //        val allocationNodes = findAllocationSites(base)

          //        val allocationNodes = findAllocationSites(base, true, fieldRef.getField)
          //        if(!allocationNodes.isEmpty) {
          //          allocationNodes.foreach(targetNode => {
          defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
            val source = createNode(method, sourceStmt)
            val target = createNode(method, targetStmt)
            updateGraph(source, target)
          })
          //          })
          //        }
        }
      case _ =>
    }
  }

  private def defsToCallSite(caller: SootMethod, callee: SootMethod, calleeDefs: LocalDefs, callStmt: Stmt,
                             retStmt: Stmt): Unit = {
    val target = createNode(caller, callStmt)

    val local = retStmt.asInstanceOf[JReturnStmt].getOp.asInstanceOf[Local]
    calleeDefs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
      val source = createNode(callee, sourceStmt)
      val csCloseLabel = createCSCloseLabel(caller, callStmt, callee)
      svg.addEdge(source, target, csCloseLabel)


      if (local.getType.isInstanceOf[ArrayType]) {
        val stores = arrayStores.getOrElseUpdate(local, List())
        stores.foreach(sourceStmt => {
          val source = createNode(callee, sourceStmt)
          val csCloseLabel = createCSCloseLabel(caller, callStmt, callee)
          svg.addEdge(source, target, csCloseLabel)
        })
      }
    })
  }

  private def stringToCallSite(caller: SootMethod, callee: SootMethod, callStmt: Stmt, retStmt: Stmt): Unit = {
    val target = createNode(caller, callStmt)
    val source = createNode(callee, retStmt)
    svg.addEdge(source, target)
  }

  private def defsToThisObject(callStatement: Statement,
                               caller: SootMethod,
                               calleeDefs: LocalDefs,
                               targetStmt: Stmt,
                               expr: AbstractInvokeExpr,
                               callee: SootMethod): Unit = {
    val invokeExpr = expr match {
      case e: JVirtualInvokeExpr => e
      case e: JSpecialInvokeExpr => e
      case e: JInterfaceInvokeExpr => e
      case _ => null //TODO: not sure if the other cases
      // are also relevant here. Otherwise,
      // we can just match with AbstractInstanceInvokeExpr
    }

    if (invokeExpr == null) {
      return
    }

    invokeExpr.getBase match {
      case base: Local =>

        val target = createNode(callee, targetStmt)

        calleeDefs.getOrElse(base, EmptyLocalDefs()).forEach(sourceStmt => {
          val source = createNode(caller, sourceStmt)
          val csOpenLabel = createCSOpenLabel(caller, callStatement.base, callee)
          svg.addEdge(source, target, csOpenLabel)
        })
      case _ =>
    }
  }

  private def defsToFormalArgs(stmt: Statement,
                               caller: SootMethod,
                               defs: LocalDefs,
                               assignStmt: Stmt,
                               exp: AbstractInvokeExpr,
                               callee: SootMethod,
                               pmtCount: Int): Unit = {
    val target = createNode(callee, assignStmt)

    val local = exp.getArg(pmtCount).asInstanceOf[Local]
    defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
      val source = createNode(caller, sourceStmt)
      val csOpenLabel = createCSOpenLabel(caller, stmt.base, callee)
      svg.addEdge(source, target, csOpenLabel)
    })
  }

  /*
   * a class to visit the allocation nodes of the objects that
   * a field might point to.
   *
   * @param method method of the statement stmt
   * @param stmt statement with a load operation
   */
  // class AllocationVisitor() extends P2SetVisitor {

  //   var allocationNodes = new ListBuffer[GraphNode]()

  //   override def visit(n: pag.Node): Unit = {
  //     if (n.isInstanceOf[AllocNode]) {
  //       val allocationNode = n.asInstanceOf[AllocNode]

  //       var stmt : StatementNode = null

  //       if (allocationNode.getNewExpr.isInstanceOf[NewExpr]) {
  //         if (allocationSites.contains(allocationNode.getNewExpr.asInstanceOf[NewExpr])) {
  //           stmt = allocationSites(allocationNode.getNewExpr.asInstanceOf[NewExpr])
  //         }
  //       }
  //       else if(allocationNode.getNewExpr.isInstanceOf[NewArrayExpr]) {
  //         if (allocationSites.contains(allocationNode.getNewExpr.asInstanceOf[NewArrayExpr])) {
  //           stmt = allocationSites(allocationNode.getNewExpr.asInstanceOf[NewArrayExpr])
  //         }
  //       }
  //       else if(allocationNode.getNewExpr.isInstanceOf[String]) {
  //         val str: StringConstant = StringConstant.v(allocationNode.getNewExpr.asInstanceOf[String])
  //         stmt = allocationSites.getOrElseUpdate(str, null)
  //       }

  //       if(stmt != null) {
  //         allocationNodes += stmt
  //       }
  //     }
  //   }
  // }

  private def defsToCallOfSinkMethod(stmt: Statement,
                                     exp: AbstractInvokeExpr,
                                     caller: SootMethod,
                                     defs: LocalDefs): Unit = {
    // edges from definitions to args
    exp.getArgs.stream().filter(a => a.isInstanceOf[Local]).forEach(a => {
      val local = a.asInstanceOf[Local]
      val targetStmt = stmt.base
      defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
        val source = createNode(caller, sourceStmt)
        val target = createNode(caller, targetStmt)
        updateGraph(source, target)
      })

      if (local.getType.isInstanceOf[ArrayType]) {
        val stores = arrayStores.getOrElseUpdate(local, List())
        stores.foreach(sourceStmt => {
          val source = createNode(caller, sourceStmt)
          val target = createNode(caller, targetStmt)
          updateGraph(source, target)
        })
      }
    })
    // edges from definition to base object of an invoke expression
    if (isFieldSensitiveAnalysis() && exp.isInstanceOf[AbstractInstanceInvokeExpr]) {
      exp.asInstanceOf[AbstractInstanceInvokeExpr].getBase match {
        case local: Local =>
          val targetStmt = stmt.base
          defs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
            val source = createNode(caller, sourceStmt)
            val target = createNode(caller, targetStmt)
            updateGraph(source, target)
          })
        case _ =>
      }
    }
  }

  /*
   * Create an edge  from the definition of the local argument
   * to the definitions of the base object of a method call. In
   * more details, we should use this rule to address a situation
   * like:
   *
   * - virtualinvoke r3.<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>(r1);
   *
   * Where we wanto create an edge from the definitions of r1 to
   * the definitions of r3.
   */
  trait CopyFromMethodArgumentToBaseObject extends RuleAction {
    def from: Int

    def apply(sootMethod: SootMethod, invokeStmt: Stmt, localDefs: LocalDefs): Unit = {
      var srcArg: Value = null
      var expr: AbstractInvokeExpr = null

      try {
        srcArg = invokeStmt.getInvokeExpr.getArg(from)
        expr = invokeStmt.getInvokeExpr
      } catch {
        case e: Exception =>
          srcArg = invokeStmt.getInvokeExpr.getArg(from)
          expr = invokeStmt.getInvokeExpr
          println("Entrou com errro!")
      }
      if (hasBaseObject(expr) && srcArg.isInstanceOf[Local]) {
        val local = srcArg.asInstanceOf[Local]

        getBaseObject(expr) match {
          case localBase: Local =>
            localDefs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
              val sourceNode = createNode(sootMethod, sourceStmt)
              localDefs.getOrElse(localBase, EmptyLocalDefs()).forEach(targetStmt => {
                val targetNode = createNode(sootMethod, targetStmt)
                updateGraph(sourceNode, targetNode)
              })
            })
          case _ =>
        }
      }
    }
  }

  //  /*
  //   * It either updates the graph or not, depending on
  //   * the types of the nodes.
  //   */

  /*
     * Create an edge from a method call to a local.
     * In more details, we should use this rule to address
     * a situation like:
     *
     * - $r6 = virtualinvoke r3.<java.lang.StringBuffer: java.lang.String toString()>();
     *
     * Where we want to create an edge from the definitions of r3 to
     * this statement.
     */
  trait CopyFromMethodCallToLocal extends RuleAction {
    def apply(sootMethod: SootMethod, invokeStmt: Stmt, localDefs: LocalDefs): Unit = {
      val expr = invokeStmt.getInvokeExpr
      val localOpt = invokeStmt match {
        case assignStmt: JAssignStmt[_, _] => Some(assignStmt);
        case _ => None
      }
      if (hasBaseObject(expr) && localOpt.nonEmpty) {
        val base = getBaseObject(expr)
        val local = localOpt.get.getLeftOp
        base match {
          case localBase: Local if local.isInstanceOf[Local] =>
            localDefs.getOrElse(localBase, EmptyLocalDefs()).forEach(source => {
              val sourceNode = createNode(sootMethod, source)
              val targetNode = createNode(sootMethod, invokeStmt)
              updateGraph(sourceNode, targetNode)
            })
          case _ =>
        }
      }
    }
  }

  /* Create an edge from the definitions of a local argument
   * to the assignment statement. In more details, we should use this rule to address
   * a situation like:
   * $r12 = virtualinvoke $r11.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>(r6);
   */
  trait CopyFromMethodArgumentToLocal extends RuleAction {
    def from: Int

    def apply(sootMethod: SootMethod, invokeStmt: Stmt, localDefs: LocalDefs): Unit = {
      val srcArg = invokeStmt.getInvokeExpr.getArg(from)
      val targetStmtOpt = invokeStmt match {
        case targetStmt: JAssignStmt[_, _] => Some(targetStmt);
        case _ => None
      }
      if (targetStmtOpt.nonEmpty && srcArg.isInstanceOf[Local]) {
        val local = srcArg.asInstanceOf[Local]
        val targetStmt = targetStmtOpt.get
        localDefs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
          val source = createNode(sootMethod, sourceStmt)
          val target = createNode(sootMethod, targetStmt)
          updateGraph(source, target)
        })
      }
    }
  }

  /*
 * Create an edge between the definitions of the actual
 * arguments of a method call. We should use this rule
 * to address situations like:
 *
 * - System.arraycopy(l1, _, l2, _)
 *
 * Where we wanto to create an edge from the definitions of
 * l1 to the definitions of l2.
 */
  trait CopyBetweenArgs extends RuleAction {
    def from: Int

    def target: Int

    def apply(sootMethod: SootMethod, invokeStmt: Stmt, localDefs: LocalDefs): Unit = {
      val srcArg = invokeStmt.getInvokeExpr.getArg(from)
      val destArg = invokeStmt.getInvokeExpr.getArg(target)
      srcArg match {
        case local: Local if destArg.isInstanceOf[Local] =>
          localDefs.getOrElse(local, EmptyLocalDefs()).forEach(sourceStmt => {
            val sourceNode = createNode(sootMethod, sourceStmt)
            localDefs.getOrElse(destArg.asInstanceOf[Local], EmptyLocalDefs()).forEach(targetStmt => {
              val targetNode = createNode(sootMethod, targetStmt)
              updateGraph(sourceNode, targetNode)
            })
          })
        case _ =>
      }
    }
  }

}
