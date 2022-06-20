/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import io.github.cvc5.*;

public interface SmtAstVisitor {
  void visit(SmtAst smtAst);

  Term visit(Declaration declaration);

  void visit(SmtModel model);

  void visit(SmtScript script);

  Term visit(SmtBinaryExpr expr);

  Sort visit(SmtSort sort);

  Sort visit(IntSort intSort);

  Term visit(SmtQtExpr quantifiedExpression);

  Sort visit(RealSort realSort);

  Sort visit(SetSort setSort);

  Sort visit(StringSort stringSort);

  Sort visit(TupleSort tupleSort);

  Term visit(SmtExpr smtExpr);

  Term visit(SmtUnaryExpr unaryExpression);

  Sort visit(UninterpretedSort uninterpretedSort);

  Term visit(IntConstant intConstant);

  Term visit(Variable variable);

  Term visit(FunctionDeclaration functionDeclaration);

  Term visit(FunctionDefinition functionDefinition);

  Term visit(BoolConstant booleanConstant);

  void visit(Assertion assertion);

  Term visit(SmtMultiArityExpr expression);

  Term visit(SmtCallExpr smtCallExpr);

  Term visit(SmtVariable smtVariable);

  Sort visit(BoolSort boolSort);

  Term visit(SmtLetExpr letExpression);

  Term visit(SmtIteExpr iteExpression);

  void visit(UninterpretedConstant uninterpretedConstant);

  void visit(SmtSettings smtSettings);

  void visit(SmtValues smtValues);

  void visit(ExpressionValue expressionValue);

  void visit(SmtUnsatCore smtUnsatCore);
}
