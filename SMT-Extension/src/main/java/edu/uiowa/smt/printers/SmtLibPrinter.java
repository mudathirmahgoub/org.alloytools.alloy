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

public class SmtLibPrinter extends AbstractSmtAstVisitor
{
  public final static String CHECK_SAT = "(check-sat)";
  public final static String GET_MODEL = "(get-model)";
  public final static String GET_UNSAT_CORE = "(get-unsat-core)";
  public final static String BLOCK_MODEL = "(block-model)";
  public final static String PUSH = "(push 1)";
  public final static String POP = "(pop 1)";
  protected SmtSettings smtSettings;
  protected StringBuilder stringBuilder = new StringBuilder();

  public SmtLibPrinter(SmtSettings smtSettings)
  {
    this.smtSettings = smtSettings;
  }

  public SmtLibPrinter()
  {
    this.smtSettings = SmtSettings.Default;
  }

  public String getSmtLib()
  {
    return stringBuilder.toString();
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
        stringBuilder.append("(declare-sort ");
        stringBuilder.append(smtSort.getName());
        stringBuilder.append(" 0)\n");
        try
        {
          Sort sort = solver.declareSort(smtSort.getName(), 0);
          sortMap.put(smtSort.getName(), sort);
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
      stringBuilder.append("(" + expr.getOp() + " ");
      Term A = visit(expr.getA());
      stringBuilder.append(" ");
      Term B = visit(expr.getB());
      stringBuilder.append(")");
      return solver.mkTerm(k, A, B);
    }
    else
    {
      stringBuilder.append("((_ " + expr.getOp() + " ");
      int index = Integer.parseInt(((IntConstant) expr.getA()).getValue());
      stringBuilder.append(index);
      stringBuilder.append(") ");
      Term tuple = visit(expr.getB());
      stringBuilder.append(")");
      return tupleSelect(tuple, index);
    }
  }

  @Override
  public Sort visit(IntSort intSort)
  {
    stringBuilder.append(intSort.getName());
    return solver.getIntegerSort();
  }

  @Override
  public Term visit(SmtQtExpr smtQtExpr)
  {
    stringBuilder.append("(" + smtQtExpr.getOp() + " (");
    Kind k = getKind(smtQtExpr.getOp());
    Term[] boundVars = new Term[smtQtExpr.getVariables().size()];
    List<SmtVariable> smtVariables = smtQtExpr.getVariables();
    for (int i = 0; i < smtVariables.size(); i++)
    {
      visit(smtVariables.get(i));
      Sort sort = visit(smtVariables.get(i).getSort());
      boundVars[i] = solver.mkVar(sort, smtVariables.get(i).getName());
    }
    stringBuilder.append(") ");
    Term body = visit(smtQtExpr.getExpr());
    stringBuilder.append(")");
    Term bvl = solver.mkTerm(VARIABLE_LIST, boundVars);
    return solver.mkTerm(k, new Term[] {bvl, body});
  }

  @Override
  public Sort visit(RealSort realSort)
  {
    stringBuilder.append("Real");
    return solver.getRealSort();
  }

  @Override
  public Sort visit(SetSort setSort)
  {
    stringBuilder.append("(Set ");
    Sort sort = visit(setSort.elementSort);
    stringBuilder.append(")");
    return solver.mkSetSort(sort);
  }

  @Override
  public Sort visit(StringSort stringSort)
  {
    stringBuilder.append("String");
    return solver.getStringSort();
  }

  @Override
  public Sort visit(TupleSort tupleSort)
  {
    stringBuilder.append("(Tuple ");
    Sort[] sorts = new Sort[tupleSort.elementSorts.size()];
    for (int i = 0; i < tupleSort.elementSorts.size() - 1; ++i)
    {
      sorts[i] = visit(tupleSort.elementSorts.get(i));
      stringBuilder.append(" ");
    }
    int index = tupleSort.elementSorts.size() - 1;
    sorts[index] = visit(tupleSort.elementSorts.get(index));
    stringBuilder.append(")");
    return solver.mkTupleSort(sorts);
  }

  @Override
  public Term visit(SmtUnaryExpr expr)
  {
    stringBuilder.append("(" + expr.getOp() + " ");
    Term term = visit(expr.getExpr());
    stringBuilder.append(")");
    Kind k = getKind(expr.getOp());
    if (k == SET_EMPTY)
    {
      Sort sort = super.visit(expr.getSort());
      term = solver.mkEmptySet(sort);
    }
    else if (k == SET_UNIVERSE)
    {
      Sort sort = super.visit(expr.getSort());
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
    stringBuilder.append(uninterpretedSort.getName());
    return getUninterpretedSort(uninterpretedSort);
  }

  @Override
  public Term visit(IntConstant intConstant)
  {
    int value = Integer.parseInt(intConstant.getValue());
    if (value >= 0)
    {
      stringBuilder.append(intConstant.getValue());
    }
    else
    {
      stringBuilder.append("(- " + -value + ")");
    }
    return solver.mkInteger(value);
  }

  @Override
  public Term visit(Variable variable)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(variable.getDeclaration());
    stringBuilder.append(symbol);
    return getTerm(variable.getDeclaration());
  }

  @Override
  public Term visit(FunctionDeclaration declaration)
  {
    stringBuilder.append("(declare-fun ");
    String symbol = TranslatorUtils.sanitizeWithBars(declaration);
    stringBuilder.append(symbol + " (");

    List<SmtSort> inputSorts = declaration.getInputSorts();
    Sort[] sorts = new Sort[inputSorts.size()];
    for (int i = 0; i < inputSorts.size(); i++)
    {
      sorts[i] = visit(inputSorts.get(i));
    }
    stringBuilder.append(") ");
    Sort sort = visit(declaration.getSort());
    stringBuilder.append(")\n");
    Term term = solver.declareFun(symbol, sorts, sort);
    termSymbols.add(new Triplet<>(symbol, declaration, term));
    return term;
  }

  @Override
  public Term visit(FunctionDefinition definition)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(definition);
    stringBuilder.append("(define-fun ").append(symbol).append(" (");
    Term[] terms = new Term[definition.inputVariables.size()];
    for (int i = 0; i < definition.inputVariables.size(); i++)
    {
      terms[i] = visit(definition.inputVariables.get(i));
    }
    stringBuilder.append(") ");
    Sort sort = visit(definition.getSort());
    stringBuilder.append(" ").append("\n");
    Term body = visit(definition.smtExpr);
    stringBuilder.append(")");
    stringBuilder.append("\n");
    Term term = solver.defineFun(symbol, terms, sort, body);
    termSymbols.add(new Triplet<>(symbol, definition, term));
    return term;
  }

  @Override
  public Term visit(BoolConstant boolConstant)
  {
    stringBuilder.append(boolConstant.getValue());
    return solver.mkBoolean(boolConstant.getBooleanValue());
  }

  @Override
  public void visit(Assertion assertion)
  {
    stringBuilder.append("\n");
    if (!assertion.getComment().isEmpty())
    {
      // print comment
      stringBuilder.append("; " + assertion.getComment() + "\n");
    }

    stringBuilder.append("(assert ");
    if (smtSettings.produceUnsatCore && !assertion.getSymbolicName().isEmpty())
    {
      stringBuilder.append("(! ");
    }
    this.visit(assertion.getSmtExpr());
    if (smtSettings.produceUnsatCore && !assertion.getSymbolicName().isEmpty())
    {
      stringBuilder.append(
          "\n :named |" + assertion.getSymbolicName().replace("\\", "/") + "|))\n");
    }
    else
    {
      stringBuilder.append(")\n");
    }
  }

  @Override
  public Term visit(SmtMultiArityExpr expr)
  {
    stringBuilder.append("(" + expr.getOp() + " ");
    Term[] terms = new Term[expr.getExprs().size()];
    if (expr.getExprs().size() == 1)
    {
      terms[0] = visit(expr.getExprs().get(0));
    }
    else if (expr.getExprs().size() > 1)
    {
      for (int i = 0; i < expr.getExprs().size() - 1; ++i)
      {
        terms[i] = visit(expr.getExprs().get(i));
        stringBuilder.append(" ");
      }
      int index = expr.getExprs().size() - 1;
      terms[index] = visit(expr.getExprs().get(index));
    }
    else
    {
      throw new RuntimeException("");
    }
    Term term;
    if (expr.getOp() == SmtMultiArityExpr.Op.MKTUPLE)
    {
      Sort[] sorts = new Sort[expr.getExprs().size()];
      for (int i = 0; i < expr.getExprs().size(); ++i)
      {
        sorts[i] = super.visit(expr.getExprs().get(i).getSort());
      }
      term = solver.mkTuple(sorts, terms);
    }
    else
    {
      Kind k = getKind(expr.getOp());
      term = solver.mkTerm(k, terms);
    }
    stringBuilder.append(")");
    return term;
  }

  @Override
  public Term visit(SmtCallExpr smtCallExpr)
  {
    FunctionDeclaration f = smtCallExpr.getFunction();
    Term fTerm = getTerm(f);
    String symbol = TranslatorUtils.sanitizeWithBars(smtCallExpr.getFunction());
    if (smtCallExpr.getArguments().size() > 0)
    {
      stringBuilder.append("(");
      stringBuilder.append(symbol);
      stringBuilder.append(" ");
      Term[] terms = new Term[smtCallExpr.getArguments().size() + 1];
      terms[0] = fTerm;
      for (int i = 0; i < smtCallExpr.getArguments().size() - 1; ++i)
      {
        Term term = visit(smtCallExpr.getArguments().get(i));
        stringBuilder.append(" ");
        terms[i + 1] = term;
      }
      int index = smtCallExpr.getArguments().size() - 1;
      terms[index + 1] = visit(smtCallExpr.getArguments().get(index));
      stringBuilder.append(")");
      Term term = solver.mkTerm(APPLY_UF, terms);
      return term;
    }
    else
    {
      stringBuilder.append(symbol);
      return fTerm;
    }
  }

  @Override
  public Term visit(SmtVariable variable)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(variable);
    stringBuilder.append("(" + symbol + " ");
    Sort sort = visit(variable.getSort());
    stringBuilder.append(")");
    return solver.mkVar(sort, symbol);
  }

  @Override
  public Sort visit(BoolSort boolSort)
  {
    stringBuilder.append(boolSort.getName());
    return solver.getBooleanSort();
  }

  @Override
  public Term visit(SmtLetExpr let)
  {
    stringBuilder.append("(let (");
    for (Map.Entry<SmtVariable, SmtExpr> letVar : let.getLetVariables().entrySet())
    {
      stringBuilder.append("(");
      stringBuilder.append(TranslatorUtils.sanitizeWithBars(letVar.getKey())).append(" ");
      this.visit(letVar.getValue());
      stringBuilder.append(")");
    }
    stringBuilder.append(") ");
    this.visit(let.getSmtExpr());
    stringBuilder.append(")");
    // ToDo: figure this out
    return null;
  }

  @Override
  public Term visit(SmtIteExpr ite)
  {
    stringBuilder.append("(ite ");
    Term condition = visit(ite.getCondExpr());
    stringBuilder.append(" ");
    Term thenTerm = visit(ite.getThenExpr());
    stringBuilder.append(" ");
    Term elseTerm = visit(ite.getElseExpr());
    stringBuilder.append(")");
    return solver.mkTerm(ITE, condition, thenTerm, elseTerm);
  }

  @Override
  public void visit(UninterpretedConstant uninterpretedConstant)
  {
    stringBuilder.append(uninterpretedConstant.getName());
  }

  @Override
  public void visit(SmtSettings smtSettings)
  {
    for (String logic : smtSettings.getLogic())
    {
      stringBuilder.append("(set-logic " + logic + ")\n");
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
      stringBuilder.append("(set-option ");
      stringBuilder.append(":" + entry.getKey() + " ");
      stringBuilder.append(entry.getValue() + ")\n");
      solver.setOption(entry.getKey(), entry.getValue());
    }
  }

  public String printGetValue(SmtExpr smtExpr)
  {
    stringBuilder.append("(get-value (");
    visit(smtExpr);
    stringBuilder.append("))");
    return stringBuilder.toString();
  }

  @Override
  public void visit(SmtValues smtValues)
  {
    stringBuilder.append("(");
    for (ExpressionValue value : smtValues.getValues())
    {
      visit(value);
    }
    stringBuilder.append(")");
  }

  @Override
  public void visit(ExpressionValue expressionValue)
  {
    stringBuilder.append("(");
    visit(expressionValue.getSmtExpr());
    stringBuilder.append(" ");
    visit(expressionValue.getValue());
    stringBuilder.append(")");
  }

  @Override
  public void visit(SmtUnsatCore smtUnsatCore)
  {
    stringBuilder.append("(\n");
    for (String formula : smtUnsatCore.getCore())
    {
      stringBuilder.append(formula + "\n");
    }
    stringBuilder.append(")");
  }
}
