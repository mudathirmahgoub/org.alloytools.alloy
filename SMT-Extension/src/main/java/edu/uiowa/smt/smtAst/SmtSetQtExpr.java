/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.printers.SmtLibPrinter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SmtSetQtExpr extends SmtFilter
{

  public final SmtSetQtExpr.Op op;

  public SmtSetQtExpr(SmtSetQtExpr.Op op, LambdaExpr lambda, SmtExpr set)
  {
    super(lambda, set);
    this.op = op;
  }


  public enum Op
  {
    ALL("set.all"),
    SOME("set.some");

    private final String opStr;

    Op(String op)
    {
      this.opStr = op;
    }

    public static SmtSetQtExpr.Op getOp(String operator)
    {
      switch (operator)
      {
        case "set.all":
          return ALL;
        case "set.some":
          return SOME;
        default:
          throw new UnsupportedOperationException("Operator " + operator + " is not defined");
      }
    }

    public SmtSetQtExpr make(LambdaExpr lambda, SmtExpr set)
    {
      return new SmtSetQtExpr(this, lambda, set);
    }

    public SmtSetQtExpr make(SmtExpr body, SmtVariable x, SmtExpr set)
    {
      LambdaExpr lambda = new LambdaExpr(Collections.singletonList(x), body);
      return new SmtSetQtExpr(this, lambda, set);
    }


    @Override
    public String toString()
    {
      return this.opStr;
    }
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