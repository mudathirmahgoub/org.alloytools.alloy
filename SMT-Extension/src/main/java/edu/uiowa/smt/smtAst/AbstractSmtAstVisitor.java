package edu.uiowa.smt.smtAst;

import io.github.cvc5.*;

abstract public class AbstractSmtAstVisitor implements SmtAstVisitor
{
  @Override
  public void visit(SmtAst smtAst)
  {
    if (smtAst instanceof Assertion)
    {
      this.visit((Assertion) smtAst);
    }
    else if (smtAst instanceof Declaration)
    {
      this.visit((Declaration) smtAst);
    }
    else if (smtAst instanceof ExpressionValue)
    {
      this.visit((ExpressionValue) smtAst);
    }
    else if (smtAst instanceof SmtExpr)
    {
      this.visit((SmtExpr) smtAst);
    }
    else if (smtAst instanceof SmtScript)
    {
      this.visit((SmtScript) smtAst);
    }
    else if (smtAst instanceof SmtModel)
    {
      this.visit((SmtModel) smtAst);
    }
    else if (smtAst instanceof SmtSettings)
    {
      this.visit((SmtSettings) smtAst);
    }
    else if (smtAst instanceof SmtUnsatCore)
    {
      this.visit((SmtUnsatCore) smtAst);
    }
    else if (smtAst instanceof SmtValues)
    {
      this.visit((SmtValues) smtAst);
    }
    else
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Term visit(Declaration declaration)
  {
    if (declaration instanceof FunctionDefinition)
    {
      return visit((FunctionDefinition) declaration);
    }
    else if (declaration instanceof FunctionDeclaration)
    {
      return visit((FunctionDeclaration) declaration);
    }
    else if (declaration instanceof SmtVariable)
    {
      return visit((SmtVariable) declaration);
    }
    else
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  abstract public Term visit(SmtExpr smtExpr);

  @Override
  abstract public Sort visit(SmtSort sort);

  @Override
  abstract public void visit(SmtScript script);

  @Override
  abstract public Term visit(SmtBinaryExpr expr);

  @Override
  abstract public Sort visit(IntSort intSort);

  @Override
  abstract public Term visit(SmtQtExpr expr);

  @Override
  abstract public Sort visit(RealSort realSort);

  @Override
  abstract public Sort visit(SetSort setSort);

  @Override
  abstract public Sort visit(StringSort stringSort);

  @Override
  abstract public Sort visit(TupleSort tupleSort);

  @Override
  abstract public Term visit(SmtUnaryExpr expr);

  @Override
  abstract public Sort visit(UninterpretedSort uninterpretedSort);

  @Override
  abstract public Term visit(IntConstant intConstant);

  @Override
  abstract public Term visit(Variable variable);
  @Override
  abstract public Term visit(FunctionDeclaration declaration);

  @Override
  abstract public Term visit(FunctionDefinition definition);

  @Override
  abstract public Term visit(BoolConstant booleanConstant);

  @Override
  abstract public void visit(Assertion assertion);

  @Override
  abstract public Term visit(SmtMultiArityExpr expr);

  @Override
  abstract public Term visit(SmtCallExpr callExpression);
  @Override
  abstract public Term visit(SmtVariable smtVariable);

  @Override
  abstract public Sort visit(BoolSort boolSort);

  @Override
  abstract public Term visit(SmtLetExpr letExpression);

  @Override
  abstract public Term visit(SmtIteExpr iteExpression);

  @Override
  abstract public void visit(UninterpretedConstant uninterpretedConstant);

  @Override
  abstract public void visit(SmtSettings smtSettings);

  @Override
  abstract public void visit(SmtValues smtValues);

  @Override
  abstract public void visit(ExpressionValue expressionValue);

  @Override
  abstract public void visit(SmtUnsatCore smtUnsatCore);
}
