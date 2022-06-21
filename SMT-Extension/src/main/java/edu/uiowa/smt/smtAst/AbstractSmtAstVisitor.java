package edu.uiowa.smt.smtAst;

import static io.github.cvc5.Kind.*;

import io.github.cvc5.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class AbstractSmtAstVisitor implements SmtAstVisitor
{
  protected Solver solver = new Solver();
  protected Map<String, Sort> sortMap = new HashMap<>();
  // here we are using a list instead of a map to handle scopes for declared terms.
  // Innermost terms are closest to the end of the list.
  // Out of scope terms should be removed.
  protected List<Triplet<String, Declaration, Term>> termSymbols = new ArrayList<>();

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

  public final Kind getKind(SmtBinaryExpr.Op op)
  {
    switch (op)
    {
      case IMPLIES: return IMPLIES;
      case PLUS: return ADD;
      case MINUS: return SUB;
      case MULTIPLY: return MULT;
      case DIVIDE: return INTS_DIVISION;
      case MOD: return INTS_MODULUS;
      case EQ: return EQUAL;
      case GTE: return GEQ;
      case LTE: return LEQ;
      case GT: return GT;
      case LT: return LT;
      case UNION: return SET_UNION;
      case INTERSECTION: return SET_INTER;
      case SETMINUS: return SET_MINUS;
      case MEMBER: return SET_MEMBER;
      case SUBSET: return SET_SUBSET;
      case JOIN: return RELATION_JOIN;
      case PRODUCT: return RELATION_PRODUCT;
      default: throw new UnsupportedOperationException(op.toString());
    }
  }

  public Term tupleSelect(Term tuple, int index)
  {
    Term projection = tuple.getSort().getDatatype().getConstructor(0).getSelector(index).getTerm();
    Term select = solver.mkTerm(APPLY_SELECTOR, projection, tuple);
    return select;
  }

  @Override
  abstract public Sort visit(IntSort intSort);

  @Override
  abstract public Term visit(SmtQtExpr expr);

  public final Kind getKind(SmtQtExpr.Op op)
  {
    switch (op)
    {
      case FORALL: return FORALL;
      case EXISTS: return EXISTS;
      default: throw new UnsupportedOperationException();
    }
  }

  @Override
  abstract public Sort visit(RealSort realSort);

  @Override
  abstract public Sort visit(SetSort setSort);

  @Override
  public Sort visit(StringSort stringSort)
  {
    return solver.getStringSort();
  }

  @Override
  abstract public Sort visit(TupleSort tupleSort);

  @Override
  abstract public Term visit(SmtUnaryExpr expr);

  public final Kind getKind(SmtUnaryExpr.Op op)
  {
    switch (op)
    {
      case NOT: return NOT;
      case COMPLEMENT: return SET_COMPLEMENT;
      case TRANSPOSE: return RELATION_TRANSPOSE;
      case TCLOSURE: return RELATION_TCLOSURE;
      case SINGLETON: return SET_SINGLETON;
      case CHOOSE: return SET_CHOOSE;
      case UNIVSET: return SET_UNIVERSE;
      case EMPTYSET: return SET_EMPTY;
      default: throw new UnsupportedOperationException(op.toString());
    }
  }

  @Override
  abstract public Sort visit(UninterpretedSort uninterpretedSort);

  public final Sort getUninterpretedSort(UninterpretedSort uninterpretedSort)
  {
    if (sortMap.containsKey(uninterpretedSort.getName()))
    {
      return sortMap.get(uninterpretedSort.getName());
    }
    else
    {
      try
      {
        Sort sort = solver.declareSort(uninterpretedSort.getName(), 0);
        sortMap.put(uninterpretedSort.getName(), sort);
        return sort;
      }
      catch (CVC5ApiException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

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
  public final Kind getKind(SmtMultiArityExpr.Op op)
  {
    switch (op)
    {
      case MKTUPLE: throw new UnsupportedOperationException(op.toString());
      case INSERT: return SET_INSERT;
      case DISTINCT: return DISTINCT;
      case OR: return OR;
      case AND: return AND;
      default: throw new UnsupportedOperationException();
    }
  }

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
