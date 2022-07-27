/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.AbstractTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmtUnaryExpr extends SmtExpr
{
  private final Op op;
  private final SmtExpr expr;

  private SmtUnaryExpr(Op op, SmtExpr expr)
  {
    this.op = op;
    if (expr == null)
    {
      throw new RuntimeException("Expression is null");
    }
    this.expr = expr;
    checkTypes();
  }

  @Override
  protected void checkTypes()
  {
    switch (op)
    {
      case NOT:
      {
        if (expr.getSort() != AbstractTranslator.boolSort)
        {
          throw new RuntimeException(String.format("Expression sort '%1$s' is not boolean", expr.getSort()));
        }
      }
      break;
      case SET_COMPLEMENT:
      {
        if (!(expr.getSort() instanceof SetSort))
        {
          throw new RuntimeException(String.format("The sort '%1$s' of expression '%2$s' is not a set", expr.getSort(), expr));
        }
      }
      break;
      case RELATION_TRANSPOSE:
      case RELATION_TCLOSURE:
      {
        // make sure expr is a set of tuples
        if (!(expr.getSort() instanceof SetSort &&
            ((SetSort) expr.getSort()).elementSort instanceof TupleSort))
        {
          throw new RuntimeException(String.format("The sort '%1$s' of expression '%2$s' is not a set of tuples", expr.getSort(), expr));
        }
        // make sure expr is a binary relation
        TupleSort tupleSort = (TupleSort) ((SetSort) expr.getSort()).elementSort;
        if (tupleSort.elementSorts.size() != 2)
        {
          throw new RuntimeException(String.format("The sort '%1$s' of expression '%2$s' is not a binary relation", expr.getSort(), expr));
        }
      }
      break;
      case SET_EMPTY:
      case SET_UNIVERSE:
      {
        if (!(expr instanceof SetSort))
        {
          throw new RuntimeException(String.format("Expected a set sort. Found '%1$s'", expr));
        }
      }
      break;
      case SET_CHOOSE:
      {
        if (!(expr.getSort() instanceof SetSort))
        {
          throw new RuntimeException(String.format("Expected a set sort in '%1$s', found '%2$s' ",
              this.toString(), expr.getSort()));
        }
      }
      case SET_SINGLETON:
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  public Op getOp()
  {
    return this.op;
  }

  public SmtExpr getExpr()
  {
    return this.expr;
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  @Override
  public SmtSort getSort()
  {
    switch (op)
    {
      case NOT:
        return AbstractTranslator.boolSort;
      case SET_COMPLEMENT:
        return expr.getSort();
      case RELATION_TRANSPOSE:
      {
        // type checking is handled during construction
        TupleSort oldSort = (TupleSort) ((SetSort) expr.getSort()).elementSort;
        List<SmtSort> reverse = new ArrayList<>();
        for (int i = oldSort.elementSorts.size() - 1; i >= 0; i--)
        {
          reverse.add(oldSort.elementSorts.get(i));
        }
        SetSort sort = new SetSort(new TupleSort(reverse));
        return sort;
      }
      case RELATION_TCLOSURE:
        return expr.getSort();
      case SET_SINGLETON:
        return new SetSort(expr.getSort());
      case SET_CHOOSE:
        return ((SetSort) expr.getSort()).elementSort;
      case SET_EMPTY:
        return expr.getSort();
      case SET_UNIVERSE:
        return expr.getSort();
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public SmtExpr evaluate(Map<String, FunctionDefinition> functions)
  {
    if (op == Op.SET_EMPTY)
    {
      if (expr.equals(AbstractTranslator.setOfUninterpretedIntTuple))
      {
        return new SmtUnaryExpr(op, AbstractTranslator.setOfIntSortTuple);
      }
      else
      {
        return this;
      }
    }
    SmtExpr smtExpr = this.expr.evaluate(functions);
    return new SmtUnaryExpr(this.op, smtExpr);
  }

  @Override
  public boolean equals(Object object)
  {
    if (object == this)
    {
      return true;
    }
    if (!(object instanceof SmtUnaryExpr))
    {
      return false;
    }
    SmtUnaryExpr unaryObject = (SmtUnaryExpr) object;
    return op == unaryObject.op &&
        expr.equals(unaryObject.expr);
  }

  @Override
  public List<Variable> getFreeVariables()
  {
    return expr.getFreeVariables();
  }

  @Override
  public SmtExpr substitute(Variable oldVariable, Variable newVariable)
  {
    if (expr.equals(newVariable))
    {
      throw new RuntimeException(String.format("Variable '%1$s' is not free in expression '%2$s'", newVariable, this));
    }

    SmtExpr newSmtExpr = expr.substitute(oldVariable, newVariable);
    return new SmtUnaryExpr(op, newSmtExpr);
  }

  @Override
  public SmtExpr replace(SmtExpr oldSmtExpr, SmtExpr newSmtExpr)
  {
    if (oldSmtExpr.equals(this))
    {
      return newSmtExpr;
    }
    SmtExpr smtExpr = expr.replace(oldSmtExpr, newSmtExpr);
    return new SmtUnaryExpr(op, smtExpr);
  }

  public enum Op
  {
    NOT("not"),
    SET_COMPLEMENT("set.complement"),
    RELATION_TRANSPOSE("rel.transpose"),
    RELATION_TCLOSURE("rel.tclosure"),
    SET_SINGLETON("set.singleton"),
    SET_CHOOSE("set.choose"),
    SET_UNIVERSE("as set.universe"),
    SET_EMPTY("as set.empty");

    private final String opStr;

    Op(String str)
    {
      this.opStr = str;
    }

    public static Op getOp(String operator)
    {
      switch (operator)
      {
        case "not":
          return NOT;
        case "set.complement":
          return SET_COMPLEMENT;
        case "rel.transpose":
          return RELATION_TRANSPOSE;
        case "rel.tclosure":
          return RELATION_TCLOSURE;
        case "set.singleton":
          return SET_SINGLETON;
        case "as set.universe":
          return SET_UNIVERSE;
        case "as set.empty":
          return SET_EMPTY;
        default:
          throw new UnsupportedOperationException("Operator " + operator + " is not defined");
      }
    }

    public SmtUnaryExpr make(SmtExpr expr)
    {
      return new SmtUnaryExpr(this, expr);
    }

    @Override
    public String toString()
    {
      return this.opStr;
    }
  }

  @Override
  public boolean containsExpr(SmtExpr expr)
  {
    if(expr.equals(this) || this.expr.containsExpr(expr))
    {
      return true;
    }
    return false;
  }
}
