/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.printers.SmtLibPrinter;

import java.util.*;

public class LambdaExpr extends SmtExpr
{
  public SmtExpr body;
  public final List<SmtVariable> inputVariables;

  public LambdaExpr(List<SmtVariable> inputVariables, SmtExpr body)
  {
    this.inputVariables = inputVariables;
    this.body = body;
  }

  public List<SmtVariable> getInputVariables()
  {
    return this.inputVariables;
  }

  public SmtExpr getBody()
  {
    return this.body;
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  @Override
  public SmtSort getSort() {
    return body.getSort();
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
    List<Variable> freeVariables = new ArrayList<>();
    List<Variable> bodyVariables = body.getFreeVariables();
    for (Variable smtExpr : bodyVariables)
    {
      if(!inputVariables.contains(smtExpr))
      {
        freeVariables.add(smtExpr);
      }
    }
    return freeVariables;
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

  public void setBody(SmtExpr body)
  {
    this.body = body;
  }
}