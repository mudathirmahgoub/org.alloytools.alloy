/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.printers;

import static io.github.cvc5.Kind.*;

import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;
import io.github.cvc5.*;
import java.util.List;
import java.util.Map;

public class Cvc5Visitor extends AbstractSmtAstVisitor
{
  protected SmtSettings smtSettings;

  public Cvc5Visitor(SmtSettings smtSettings)
  {
    this.smtSettings = smtSettings;
  }

  public Cvc5Visitor()
  {
    this.smtSettings = SmtSettings.Default;
  }

  public Solver getSolver()
  {
    return solver;
  }

  protected void initializeProgram()
  {
    visit(smtSettings);
  }

  public void visit(SmtScript script)
  {
    if (script.getParent() == null)
    {
      initializeProgram();
    }

    for (SmtSort smtSort : script.getSorts())
    {
      if (smtSort instanceof UninterpretedSort)
      {
        try
        {
          Sort sort = solver.declareSort(smtSort.getName(), 0);
          sortSymbols.put(smtSort.getName(), sort);
        }
        catch (CVC5ApiException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
    for (FunctionDeclaration declaration : script.getFunctions())
    {
      if (declaration instanceof FunctionDefinition)
      {
        this.visit((FunctionDefinition) declaration);
      }
      else
      {
        this.visit(declaration);
      }
    }

    for (Assertion assertion : script.getAssertions())
    {
      this.visit(assertion);
    }
  }

  @Override
  public Term visit(SmtBinaryExpr expr)
  {
    if (expr.getOp() != SmtBinaryExpr.Op.TUPSEL)
    {
      Kind k = getKind(expr.getOp());
      Term A = visit(expr.getA());
      Term B = visit(expr.getB());
      return solver.mkTerm(k, A, B);
    }
    else
    {
      int index = Integer.parseInt(((IntConstant) expr.getA()).getValue());
      Term tuple = visit(expr.getB());
      return tupleSelect(tuple, index);
    }
  }

  @Override
  public Sort visit(IntSort intSort)
  {
    return solver.getIntegerSort();
  }

  @Override
  public Term visit(SmtQtExpr smtQtExpr)
  {
    int size = termSymbols.size();
    Kind k = getKind(smtQtExpr.getOp());
    Term[] boundVars = new Term[smtQtExpr.getVariables().size()];
    List<SmtVariable> smtVariables = smtQtExpr.getVariables();
    for (int i = 0; i < smtVariables.size(); i++)
    {
      String symbol = TranslatorUtils.sanitizeWithBars(smtVariables.get(i));
      Sort sort = visit(smtVariables.get(i).getSort());
      boundVars[i] = solver.mkVar(sort, symbol);
      termSymbols.add(new Triplet<>(symbol, smtVariables.get(i), boundVars[i]));
    }
    Term body = visit(smtQtExpr.getExpr());
    Term bvl = solver.mkTerm(VARIABLE_LIST, boundVars);
    termSymbols.subList(size, termSymbols.size()).clear();
    return solver.mkTerm(k, new Term[] {bvl, body});
  }

  @Override
  public Sort visit(RealSort realSort)
  {
    return solver.getRealSort();
  }

  @Override
  public Sort visit(SetSort setSort)
  {
    Sort sort = visit(setSort.elementSort);
    return solver.mkSetSort(sort);
  }

  @Override
  public Sort visit(StringSort stringSort)
  {
    return solver.getStringSort();
  }

  @Override
  public Sort visit(TupleSort tupleSort)
  {
    Sort[] sorts = new Sort[tupleSort.elementSorts.size()];
    for (int i = 0; i < tupleSort.elementSorts.size() - 1; ++i)
    {
      sorts[i] = visit(tupleSort.elementSorts.get(i));
    }
    int index = tupleSort.elementSorts.size() - 1;
    sorts[index] = visit(tupleSort.elementSorts.get(index));
    return solver.mkTupleSort(sorts);
  }

  @Override
  public Term visit(SmtUnaryExpr expr)
  {
    Term term = visit(expr.getExpr());
    Kind k = getKind(expr.getOp());
    if (k == SET_EMPTY)
    {
      Sort sort = visit(expr.getSort());
      term = solver.mkEmptySet(sort);
    }
    else if (k == SET_UNIVERSE)
    {
      Sort sort = visit(expr.getSort());
      term = solver.mkUniverseSet(sort);
    }
    else
    {
      term = solver.mkTerm(k, term);
    }
    return term;
  }

  @Override
  public Sort visit(UninterpretedSort uninterpretedSort)
  {
    return getUninterpretedSort(uninterpretedSort);
  }

  @Override
  public Term visit(IntConstant intConstant)
  {
    int value = Integer.parseInt(intConstant.getValue());
    return solver.mkInteger(value);
  }

  @Override
  public Term visit(Variable variable)
  {
    return getTerm(variable.getDeclaration());
  }

  @Override
  public Term visit(FunctionDeclaration declaration)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(declaration);
    List<SmtSort> inputSorts = declaration.getInputSorts();
    Sort[] sorts = new Sort[inputSorts.size()];
    for (int i = 0; i < inputSorts.size(); i++)
    {
      sorts[i] = visit(inputSorts.get(i));
    }

    Sort sort = visit(declaration.getSort());
    Term term = solver.declareFun(symbol, sorts, sort);
    termSymbols.add(new Triplet<>(symbol, declaration, term));
    return term;
  }

  @Override
  public Term visit(FunctionDefinition definition)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(definition);
    Term[] terms = new Term[definition.inputVariables.size()];
    for (int i = 0; i < definition.inputVariables.size(); i++)
    {
      terms[i] = visit(definition.inputVariables.get(i));
    }

    Sort sort = visit(definition.getSort());

    Term body = visit(definition.smtExpr);
    Term term = solver.defineFun(symbol, terms, sort, body);
    termSymbols.add(new Triplet<>(symbol, definition, term));
    return term;
  }

  @Override
  public Term visit(BoolConstant boolConstant)
  {
    return solver.mkBoolean(boolConstant.getBooleanValue());
  }

  @Override
  public void visit(Assertion assertion)
  {
    Term term = visit(assertion.getSmtExpr());
    solver.assertFormula(term);
  }

  @Override
  public Term visit(SmtMultiArityExpr expr)
  {
    Term[] terms = new Term[expr.getExprs().size()];
    for (int i = 0; i < expr.getExprs().size(); ++i)
    {
      terms[i] = visit(expr.getExprs().get(i));
    }

    if (expr.getOp() == SmtMultiArityExpr.Op.MKTUPLE)
    {
      Sort[] sorts = new Sort[expr.getExprs().size()];
      for (int i = 0; i < expr.getExprs().size(); ++i)
      {
        sorts[i] = visit(expr.getExprs().get(i).getSort());
      }
      return solver.mkTuple(sorts, terms);
    }
    Kind k = getKind(expr.getOp());
    return solver.mkTerm(k, terms);
  }

  @Override
  public Term visit(SmtCallExpr smtCallExpr)
  {
    FunctionDeclaration f = smtCallExpr.getFunction();
    Term fTerm = getTerm(f);
    String symbol = TranslatorUtils.sanitizeWithBars(smtCallExpr.getFunction());
    if (smtCallExpr.getArguments().size() > 0)
    {
      Term[] terms = new Term[smtCallExpr.getArguments().size() + 1];
      terms[0] = fTerm;
      for (int i = 0; i < smtCallExpr.getArguments().size() - 1; ++i)
      {
        Term term = visit(smtCallExpr.getArguments().get(i));
        terms[i + 1] = term;
      }
      int index = smtCallExpr.getArguments().size() - 1;
      terms[index + 1] = visit(smtCallExpr.getArguments().get(index));
      Term term = solver.mkTerm(APPLY_UF, terms);
      return term;
    }
    else
    {
      return fTerm;
    }
  }

  @Override
  public Term visit(SmtVariable variable)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(variable);
    Sort sort = visit(variable.getSort());
    return solver.mkVar(sort, symbol);
  }

  @Override
  public Sort visit(BoolSort boolSort)
  {
    return solver.getBooleanSort();
  }

  @Override
  public Term visit(SmtLetExpr let)
  {
    int size = termSymbols.size();
    for (Map.Entry<SmtVariable, SmtExpr> letVar : let.getLetVariables().entrySet())
    {
      String symbol = TranslatorUtils.sanitizeWithBars(letVar.getKey());
      Term body = visit(letVar.getValue());
      Sort sort = visit(letVar.getValue().getSort());
      Term term = solver.defineFun(symbol, new Term[0], sort, body);
      termSymbols.add(new Triplet<>(symbol, letVar.getKey(), term));
    }
    Term letBody = visit(let.getSmtExpr());
    // restore scope after let body
    termSymbols.subList(size, termSymbols.size()).clear();
    return letBody;
  }

  @Override
  public Term visit(SmtIteExpr ite)
  {
    Term condition = visit(ite.getCondExpr());
    Term thenTerm = visit(ite.getThenExpr());
    Term elseTerm = visit(ite.getElseExpr());
    return solver.mkTerm(ITE, condition, thenTerm, elseTerm);
  }

  @Override
  public void visit(UninterpretedConstant uninterpretedConstant)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SmtSettings smtSettings)
  {
    for (String logic : smtSettings.getLogic())
    {
      try
      {
        solver.setLogic(logic);
      }
      catch (CVC5ApiException e)
      {
        throw new RuntimeException(e);
      }
    }
    Map<String, String> options = smtSettings.getSolverOptions();
    for (Map.Entry<String, String> entry : options.entrySet())
    {
      solver.setOption(entry.getKey(), entry.getValue());
    }
  }

  public String printGetValue(SmtExpr smtExpr)
  {
    Term term = visit(smtExpr);
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SmtValues smtValues)
  {
    for (ExpressionValue value : smtValues.getValues())
    {
      visit(value);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(ExpressionValue expressionValue)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SmtUnsatCore smtUnsatCore)
  {
    throw new UnsupportedOperationException();
  }

  public Term getTerm(Declaration declaration)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(declaration);
    for (int i = termSymbols.size() - 1; i >= 0; i--)
    {
      if (termSymbols.get(i).first.equals(symbol) && termSymbols.get(i).second.equals(declaration))
      {
        return termSymbols.get(i).third;
      }
    }
    // create term for this variable
    // Term term = visit(declaration);
    throw new RuntimeException(declaration.toString());
    // ToDo: review when there is a collision in names in different scopes
    //    return term;
  }

  @Override
  public Term visit(SmtExpr smtExpr)
  {
    if (smtExpr instanceof Variable)
    {
      return visit((Variable) smtExpr);
    }
    if (smtExpr instanceof SmtUnaryExpr)
    {
      return visit((SmtUnaryExpr) smtExpr);
    }
    if (smtExpr instanceof SmtBinaryExpr)
    {
      return visit((SmtBinaryExpr) smtExpr);
    }
    if (smtExpr instanceof SmtMultiArityExpr)
    {
      return visit((SmtMultiArityExpr) smtExpr);
    }
    if (smtExpr instanceof SmtQtExpr)
    {
      return visit((SmtQtExpr) smtExpr);
    }
    if (smtExpr instanceof SmtSort)
    {
      visit((SmtSort) smtExpr);
      return null;
    }
    if (smtExpr instanceof IntConstant)
    {
      return visit((IntConstant) smtExpr);
    }
    if (smtExpr instanceof SmtCallExpr)
    {
      return visit((SmtCallExpr) smtExpr);
    }
    if (smtExpr instanceof BoolConstant)
    {
      return visit((BoolConstant) smtExpr);
    }
    if (smtExpr instanceof SmtLetExpr)
    {
      return visit((SmtLetExpr) smtExpr);
    }
    if (smtExpr instanceof SmtIteExpr)
    {
      return visit((SmtIteExpr) smtExpr);
    }
    if (smtExpr instanceof UninterpretedConstant)
    {
      this.visit((UninterpretedConstant) smtExpr);
      return null;
    }
    else
    {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Sort visit(SmtSort sort)
  {
    if (sort instanceof UninterpretedSort)
    {
      return visit((UninterpretedSort) sort);
    }
    else if (sort instanceof SetSort)
    {
      return visit((SetSort) sort);
    }
    else if (sort instanceof TupleSort)
    {
      return visit((TupleSort) sort);
    }
    else if (sort instanceof IntSort)
    {
      return visit((IntSort) sort);
    }
    else if (sort instanceof RealSort)
    {
      return visit((RealSort) sort);
    }
    else if (sort instanceof StringSort)
    {
      return visit((StringSort) sort);
    }
    else if (sort instanceof BoolSort)
    {
      return visit((BoolSort) sort);
    }
    else
    {
      throw new UnsupportedOperationException();
    }
  }

  public List<Triplet<String, Declaration, Term>> getTermSymbols()
  {
    return termSymbols;
  }

  public Map<String, Sort> getSortSymbols()
  {
    return sortSymbols;
  }
}
