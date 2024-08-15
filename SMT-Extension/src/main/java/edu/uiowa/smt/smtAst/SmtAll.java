/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.printers.SmtLibPrettyPrinter;
import edu.uiowa.smt.printers.SmtLibPrinter;

import java.util.List;
import java.util.Map;

public class SmtAll extends SmtFilter
{

  public SmtAll(LambdaExpr lambda, SmtExpr set)
  {
    super(lambda, set);
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  @Override
  public SmtSort getSort() {
    return AbstractTranslator.boolSort;
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