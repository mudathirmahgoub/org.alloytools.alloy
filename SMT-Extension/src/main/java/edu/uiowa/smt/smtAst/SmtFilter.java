/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.printers.SmtLibPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmtFilter extends SmtExpr
{
  public LambdaExpr getLambda() {
    return lambda;
  }


  public LambdaExpr lambda;

  public SmtExpr getSet() {
    return set;
  }

  public final SmtExpr set;

  public SmtFilter(LambdaExpr lambda, SmtExpr set)
  {
    this.lambda = lambda;
    this.set = set;
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  @Override
  public SmtSort getSort() {
    return set.getSort();
  }

  @Override
  public String toString()
  {
    SmtLibPrinter printer = new SmtLibPrinter();
    printer.visit(this);
    return printer.getSmtLib();
  }

  @Override
  public SmtExpr evaluate(Map<String, FunctionDefinition> functions) {
    return null;
  }

  @Override
  public boolean equals(Object object) {
    return false;
  }

  @Override
  public List<Variable> getFreeVariables() {
    return null;
  }

  @Override
  public SmtExpr substitute(Variable oldVariable, Variable newVariable) {
    return null;
  }

  @Override
  public SmtExpr replace(SmtExpr oldSmtExpr, SmtExpr newSmtExpr) {
    return null;
  }

  @Override
  public boolean containsExpr(SmtExpr expr) {
    return false;
  }

}