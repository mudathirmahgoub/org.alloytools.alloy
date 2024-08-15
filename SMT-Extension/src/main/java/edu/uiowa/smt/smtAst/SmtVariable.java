/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

public class SmtVariable extends Declaration
{
  private SmtExpr constraint;
  private SmtExpr set;

  public SmtVariable(String name, SmtSort sort, boolean isOriginal)
  {
    super(name, sort, isOriginal);
  }

  public void setConstraint(SmtExpr constraint)
  {
    this.constraint = constraint;
  }

  public SmtExpr getConstraint()
  {
    return constraint;
  }

  public SmtExpr getSet()
  {
    return set;
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  public void setSet(SmtExpr set)
  {
    this.set = set;
  }
}
