package ca.sfu.cmpt745.ex06.checker;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.AbstractExprSwitch;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import soot.toolkits.scalar.ForwardFlowAnalysis;


public class KittenChecker extends BodyTransformer {
  enum KittenState {
    SLEEPING, EATING, PLAYING, PLOTTING, RUNNING
  }

  KittenChecker(KittenErrorReporter reporter) {
    this.reporter = reporter;
  }

  protected void internalTransform(Body body, String phase, Map options) {
    UnitGraph graph = new ExceptionalUnitGraph(body);

    System.out.println("Implement your analysis here.");
    KittenAnalysis analysis = new KittenAnalysis(graph,reporter);

    // You should define and uncomment the kitten analysis above.
    // Then explore the results and report potential errors using the provided
    // reporter.
  }

  final KittenErrorReporter reporter;

  private class KittenAnalysis extends ForwardFlowAnalysis<Unit, Map<Local, EnumSet<KittenState>>> {

    public KittenAnalysis(UnitGraph graph, KittenErrorReporter reporter) {
        super(graph);
        this.reporter = reporter;
        super.doAnalysis();
    }

    final KittenErrorReporter reporter;

    @Override
    protected Map<Local, EnumSet<KittenState>> newInitialFlow() {
        return new HashMap<>();
    }

    @Override
    protected Map<Local, EnumSet<KittenState>> entryInitialFlow() {
        return new HashMap<>();
    }

    @Override
    protected void merge(Map<Local, EnumSet<KittenState>> in1,Map<Local, EnumSet<KittenState>> in2,Map<Local, EnumSet<KittenState>> out) {
        out.clear();
        Set<Local> allLocals = new HashSet<>();
        allLocals.addAll(in1.keySet());
        allLocals.addAll(in2.keySet());

        for (Local local : allLocals) {
            EnumSet<KittenState> states1 = in1.getOrDefault(local, EnumSet.noneOf(KittenState.class));
            EnumSet<KittenState> states2 = in2.getOrDefault(local, EnumSet.noneOf(KittenState.class));
            EnumSet<KittenState> merged = EnumSet.copyOf(states1);
            merged.addAll(states2);
            out.put(local, merged);
        }
    }

    @Override
    protected void copy(Map<Local, EnumSet<KittenState>> source,Map<Local, EnumSet<KittenState>> dest) {
        dest.clear();
        for (Map.Entry<Local, EnumSet<KittenState>> entry : source.entrySet()) {
            dest.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }
    }

    @Override
    protected void flowThrough(Map<Local, EnumSet<KittenState>> in,Unit unit,Map<Local, EnumSet<KittenState>> out) {
      copy(in, out);
      //check if its an Assign Statement like x = new Kitten or for test 9 kitten2 = kitten1
      if (unit instanceof AssignStmt) {
        AssignStmt assignStmt = (AssignStmt) unit;
        Value rhs = assignStmt.getRightOp();
        Value lhs = assignStmt.getLeftOp();
        if (rhs instanceof NewExpr && lhs instanceof Local) {
          NewExpr expr = (NewExpr) rhs;
          if (expr.getType().toString().equals("Kitten")) {
            Local kittenLocal = (Local) lhs;
            out.put(kittenLocal, EnumSet.of(KittenState.SLEEPING));
          }
        }
        else if (rhs instanceof Local && lhs instanceof Local) {
          Local rightKitten = (Local) rhs;
          Local leftKitten = (Local) lhs;
          EnumSet<KittenState> states = out.getOrDefault(rightKitten, EnumSet.noneOf(KittenState.class));
          out.put(leftKitten, EnumSet.copyOf(states));
        }
      }
      else if (unit instanceof InvokeStmt) {
        InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
        if (invokeExpr instanceof InstanceInvokeExpr) {
          InstanceInvokeExpr instanceInvoke = (InstanceInvokeExpr) invokeExpr;
          Value base = instanceInvoke.getBase();
          if (base instanceof Local) {
            Local kittenLocal = (Local) base;
            String methodName = instanceInvoke.getMethod().getName();
            EnumSet<KittenState> currentStates = out.getOrDefault(kittenLocal, EnumSet.noneOf(KittenState.class));
            EnumSet<KittenState> newStates = EnumSet.noneOf(KittenState.class);
            KittenState targetState = KittenState.SLEEPING;
            int isTransferMethod = 0;
            switch (methodName) {
              case "pet":
                  targetState = KittenState.SLEEPING;
                  isTransferMethod = 1;
                  break;
              case "feed":
                  targetState = KittenState.EATING;
                  isTransferMethod = 1;
                  break;
              case "tease":
                  targetState = KittenState.PLAYING;
                  isTransferMethod = 1;
                  break;
              case "ignore":
                  targetState = KittenState.PLOTTING;
                  isTransferMethod = 1;
                  break;
              case "scare":
                  targetState = KittenState.RUNNING;
                  isTransferMethod = 1;
                  break;
              default:
                  newStates = currentStates;
                  break;
            }
            if(isTransferMethod == 1){
              isValidTransfer(methodName,currentStates,kittenLocal, unit);
              newStates.add(targetState);
              out.put(kittenLocal, newStates);
            }
          }
        }
      }
    }

    private void isValidTransfer(String name, EnumSet<KittenState> currentStates,Local variable,Unit unit){
      int lineNumber = unit.getJavaSourceStartLineNumber();
      for(KittenState state : currentStates){
        if(state == KittenState.RUNNING){
          if(name.equals("pet")){
            reporter.reportError(variable.getName(),lineNumber,"sleeping","running");
          }
        }
        else if(state == KittenState.SLEEPING){
          if(name.equals("tease")){
            reporter.reportError(variable.getName(),lineNumber,"playing","sleeping");
          }
          else if(name.equals("ignore")){
            reporter.reportError(variable.getName(),lineNumber,"plotting","sleeping");
          }
        }
        else if(state == KittenState.EATING){
          if(name.equals("tease")){
            reporter.reportError(variable.getName(),lineNumber,"playing","eating");
          }
          else if(name.equals("ignore")){
            reporter.reportError(variable.getName(),lineNumber,"plotting","eating");
          }

        }
        else if(state == KittenState.PLAYING){
          if(name.equals("pet")){
            reporter.reportError(variable.getName(),lineNumber,"sleeping","playing");
          }
          else if(name.equals("ignore")){
            reporter.reportError(variable.getName(),lineNumber,"plotting","playing");
          }
        }
      }
      return;
    }
  }

  
  
}
                               

