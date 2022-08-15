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
import io.github.cvc5.modes.*;

import javax.swing.*;
import java.util.*;

public class Cvc5ApiVisitor extends AbstractSmtAstVisitor
{
  protected final Solver solver;
  private final StringBuilder stringBuilder;

  protected Map<String, Sort> sortSymbols = new HashMap<>();
  // here we are using a list instead of a map to handle scopes for declared terms.
  // Innermost terms are closest to the end of the list.
  // Out of scope terms should be removed.
  protected List<Triplet<String, Declaration, Term>> termSymbols = new ArrayList<>();

  // should be managed with push and pop
  protected List<Pair<Term, Assertion>> currentAssertions = new ArrayList<>();
  protected int assertionsSizeBeforeLastPush = 0;

  protected SmtSettings smtSettings;

  public Cvc5ApiVisitor(SmtSettings smtSettings, Solver solver)
  {
    this.smtSettings = smtSettings;
    this.solver = solver;
    stringBuilder = new StringBuilder();
    stringBuilder.append("import static io.github.cvc5.Kind.*;\n"
        + "\n"
        + "import io.github.cvc5.*;\n"
        + "import java.util.HashSet;\n"
        + "import java.util.Set;\n"
        + "\n"
        + "public class Main\n"
        + "{\n"
        + "  public static void main(String[] args) throws CVC5ApiException\n"
        + "  {\n"
        + "    Solver solver = new Solver();");
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
          stringBuilder.append("    Sort sort" + Integer.toHexString(sort.hashCode())
              + " = solver.declareSort(\"" + smtSort.getName() + "\", 0);\n");

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
    if (expr.getOp() != SmtBinaryExpr.Op.TUPLE_SELECT)
    {
      Kind k = getKind(expr.getOp());
      Term A = visit(expr.getA());
      Term B = visit(expr.getB());
      Term ret = solver.mkTerm(k, A, B);
      stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
          + "  = solver.mkTerm(" + k + ", term" + Integer.toHexString(A.hashCode()) + ", term"
          + Integer.toHexString(B.hashCode()) + ");\n");
      stringBuilder.append("    //" + ret + ");\n");
      return ret;
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
    Sort ret = solver.getIntegerSort();
    stringBuilder.append(
        "    Sort sort" + Integer.toHexString(ret.hashCode()) + " = solver.getIntegerSort();\n");
    return ret;
  }

  @Override
  public Term visit(SmtQtExpr smtQtExpr)
  {
    int size = termSymbols.size();
    Kind k = getKind(smtQtExpr.getOp());
    Term[] boundVars = new Term[smtQtExpr.getVariables().size()];
    stringBuilder.append("    Term[] boundVars" + Integer.toHexString(boundVars.hashCode())
        + " = new Term[" + smtQtExpr.getVariables().size() + "];\n");
    List<SmtVariable> smtVariables = smtQtExpr.getVariables();
    for (int i = 0; i < smtVariables.size(); i++)
    {
      String symbol = TranslatorUtils.sanitizeWithBars(smtVariables.get(i));
      Sort sort = visit(smtVariables.get(i).getSort());
      Term term = solver.mkVar(sort, symbol);
      stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
          + " = solver.mkVar(sort" + Integer.toHexString(sort.hashCode()) + ", \"" + symbol
          + "\");\n");
      boundVars[i] = term;
      stringBuilder.append("    boundVars" + Integer.toHexString(boundVars.hashCode()) + "[" + i
          + "] = term" + Integer.toHexString(term.hashCode()) + ";\n");
      termSymbols.add(new Triplet<>(symbol, smtVariables.get(i), boundVars[i]));
    }
    Term body = visit(smtQtExpr.getExpr());
    Term bvl = solver.mkTerm(VARIABLE_LIST, boundVars);
    stringBuilder.append("    Term term" + Integer.toHexString(bvl.hashCode())
        + " = solver.mkTerm(VARIABLE_LIST, boundVars" + Integer.toHexString(boundVars.hashCode())
        + ");\n");
    stringBuilder.append("    // " + bvl + ");\n");
    termSymbols.subList(size, termSymbols.size()).clear();

    Term ret = solver.mkTerm(k, new Term[] {bvl, body});

    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + "  = solver.mkTerm(" + k + ", new Term[] { term" + Integer.toHexString(bvl.hashCode())
        + ", term" + Integer.toHexString(body.hashCode()) + "});\n");
    stringBuilder.append("    // " + ret + ");\n");
    return ret;
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
    Sort ret = solver.mkSetSort(sort);
    stringBuilder.append("    Sort sort" + Integer.toHexString(ret.hashCode())
        + "  = solver.mkSetSort(sort" + Integer.toHexString(sort.hashCode()) + ");\n");
    return ret;
  }

  @Override
  public Sort visit(TupleSort tupleSort)
  {
    Sort[] sorts = new Sort[tupleSort.elementSorts.size()];
    stringBuilder.append("    Sort[] sorts" + Integer.toHexString(sorts.hashCode()) + " = new Sort["
        + tupleSort.elementSorts.size() + "];\n");
    for (int i = 0; i < tupleSort.elementSorts.size(); ++i)
    {
      sorts[i] = visit(tupleSort.elementSorts.get(i));
      stringBuilder.append("    sorts" + Integer.toHexString(sorts.hashCode()) + "[" + i
          + "] = sort" + Integer.toHexString(sorts[i].hashCode()) + ";\n");
    }
    Sort ret = solver.mkTupleSort(sorts);

    stringBuilder.append("    Sort sort" + Integer.toHexString(ret.hashCode())
        + " = solver.mkTupleSort(sorts" + Integer.toHexString(sorts.hashCode()) + ");\n");
    return ret;
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
      stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
          + "  = solver.mkEmptySet(sort" + Integer.toHexString(sort.hashCode()) + ");\n");
      stringBuilder.append("    // " + term + ");\n");
    }
    else if (k == SET_UNIVERSE)
    {
      Sort sort = visit(expr.getSort());
      term = solver.mkUniverseSet(sort);
      stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
          + "  = solver.mkUniverseSet(sort" + Integer.toHexString(sort.hashCode()) + ");\n");
      stringBuilder.append("    // " + term + ");\n");
    }
    else
    {
      Term ret = solver.mkTerm(k, term);
      stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
          + "  = solver.mkTerm(" + k + ", term" + Integer.toHexString(term.hashCode()) + ");\n");
      stringBuilder.append("    // " + ret + ");\n");
      term = ret;
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
    Term ret = solver.mkInteger(value);
    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + " = solver.mkInteger(" + value + ");\n");
    stringBuilder.append("    // " + ret + ");\n");
    return ret;
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
    stringBuilder.append("    Sort[] sorts" + Integer.toHexString(sorts.hashCode()) + " = new Sort["
        + inputSorts.size() + "];\n");
    for (int i = 0; i < inputSorts.size(); i++)
    {
      sorts[i] = visit(inputSorts.get(i));
      stringBuilder.append("    sorts" + Integer.toHexString(sorts.hashCode()) + "[" + i
          + "] = sort" + Integer.toHexString(sorts[i].hashCode()) + ";\n");
    }

    Sort sort = visit(declaration.getSort());
    Term term = solver.declareFun(symbol, sorts, sort);
    stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
        + " = solver.declareFun(\"" + symbol + "\", sorts" + Integer.toHexString(sorts.hashCode())
        + ", sort" + Integer.toHexString(sort.hashCode()) + ");\n");
    stringBuilder.append("    // solver.declareFun(symbol, sorts, sort); term: " + term + "\n");
    termSymbols.add(new Triplet<>(symbol, declaration, term));
    return term;
  }

  @Override
  public Term visit(FunctionDefinition definition)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(definition);
    Term[] terms = new Term[definition.inputVariables.size()];

    stringBuilder.append("    Term[] terms" + Integer.toHexString(terms.hashCode()) + " = new Term["
        + definition.inputVariables.size() + "];\n");

    for (int i = 0; i < definition.inputVariables.size(); i++)
    {
      terms[i] = visit(definition.inputVariables.get(i));
      stringBuilder.append("    terms" + Integer.toHexString(terms.hashCode()) + "[" + i
          + "] = term" + Integer.toHexString(terms[i].hashCode()) + ";\n");
      stringBuilder.append("    // " + terms[i] + "\n");
    }

    Sort sort = visit(definition.getSort());
    Term body = visit(definition.smtExpr);
    Term term = solver.defineFun(symbol, terms, sort, body);
    stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
        + " = solver.defineFun(\"" + symbol + "\", terms" + Integer.toHexString(terms.hashCode())
        + ", sort" + Integer.toHexString(sort.hashCode()) + ", term"
        + Integer.toHexString(body.hashCode()) + ");\n");
    stringBuilder.append(
        "    // Term term = solver.defineFun(symbol, terms, sort, body);" + term + "\n");
    termSymbols.add(new Triplet<>(symbol, definition, term));
    return term;
  }

  @Override
  public Term visit(BoolConstant boolConstant)
  {
    Term ret = solver.mkBoolean(boolConstant.getBooleanValue());

    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + " = solver.mkBoolean(" + boolConstant.getBooleanValue() + ");\n");
    stringBuilder.append("    // " + ret + "\n");

    return ret;
  }

  @Override
  public void visit(Assertion assertion)
  {
    Term term = visit(assertion.getSmtExpr());
    currentAssertions.add(new Pair<>(term, assertion));
    solver.assertFormula(term);

    stringBuilder.append(
        "    solver.assertFormula(term" + Integer.toHexString(term.hashCode()) + ");\n");
    stringBuilder.append("    // solver.assertFormula(term); term: " + term + "\n");
  }

  @Override
  public Term visit(SmtMultiArityExpr expr)
  {
    Term[] terms = new Term[expr.getExprs().size()];
    stringBuilder.append("    Term[] terms" + Integer.toHexString(terms.hashCode()) + " = new Term["
        + expr.getExprs().size() + "];\n");
    for (int i = 0; i < expr.getExprs().size(); ++i)
    {
      terms[i] = visit(expr.getExprs().get(i));
      stringBuilder.append("    terms" + Integer.toHexString(terms.hashCode()) + "[" + i
          + "] = term" + Integer.toHexString(terms[i].hashCode()) + ";\n");
      stringBuilder.append("    // terms" + Integer.toHexString(terms.hashCode()) + "[" + i
          + "] = " + terms[i] + "\n");
    }

    if (expr.getOp() == SmtMultiArityExpr.Op.TUPLE)
    {
      Sort[] sorts = new Sort[expr.getExprs().size()];
      stringBuilder.append("    Sort[] sorts" + Integer.toHexString(sorts.hashCode())
          + " = new Sort[" + expr.getExprs().size() + "];\n");
      for (int i = 0; i < expr.getExprs().size(); ++i)
      {
        sorts[i] = visit(expr.getExprs().get(i).getSort());
        stringBuilder.append("    sorts" + Integer.toHexString(sorts.hashCode()) + "[" + i
            + "] = sort" + Integer.toHexString(sorts[i].hashCode()) + ";\n");
      }
      Term ret = solver.mkTuple(sorts, terms);
      stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
          + " = solver.mkTuple(sorts" + Integer.toHexString(sorts.hashCode()) + ", terms"
          + Integer.toHexString(terms.hashCode()) + ");\n");
      stringBuilder.append("    // " + ret + ");\n");
      return ret;
    }
    Kind k = getKind(expr.getOp());
    Term ret = solver.mkTerm(k, terms);

    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode()) + " = solver.mkTerm("
        + k + ", terms" + Integer.toHexString(terms.hashCode()) + ");\n");
    stringBuilder.append("    // " + ret + ");\n");
    return ret;
  }

  @Override
  public Term visit(SmtCallExpr smtCallExpr)
  {
    FunctionDeclaration f = smtCallExpr.getFunction();
    Term fTerm = getTerm(f);
    if (smtCallExpr.getArguments().size() > 0)
    {
      Term[] terms = new Term[smtCallExpr.getArguments().size() + 1];
      stringBuilder.append("    Term[] terms" + Integer.toHexString(terms.hashCode())
          + " = new Term[" + (smtCallExpr.getArguments().size() + 1) + "];\n");
      terms[0] = fTerm;
      stringBuilder.append("    terms" + Integer.toHexString(terms.hashCode()) + "[0] = term"
          + Integer.toHexString(fTerm.hashCode()) + ";\n");
      for (int i = 0; i < smtCallExpr.getArguments().size(); ++i)
      {
        Term term = visit(smtCallExpr.getArguments().get(i));
        terms[i + 1] = term;
        stringBuilder.append("    terms" + Integer.toHexString(terms.hashCode()) + "[" + (i + 1)
            + "] = term" + Integer.toHexString(term.hashCode()) + ";\n");
      }

      Term term = solver.mkTerm(APPLY_UF, terms);
      stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
          + " = solver.mkTerm(APPLY_UF, terms" + Integer.toHexString(terms.hashCode()) + ");\n");
      stringBuilder.append("    // " + term + ");\n");
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
    Term ret = solver.mkVar(sort, symbol);
    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + " = solver.mkVar(sort" + Integer.toHexString(sort.hashCode()) + ", \"" + symbol
        + "\");\n");
    stringBuilder.append("    // " + ret + ");\n");
    return ret;
  }

  @Override
  public Sort visit(BoolSort boolSort)
  {
    Sort ret = solver.getBooleanSort();
    stringBuilder.append(
        "    Sort sort" + Integer.toHexString(ret.hashCode()) + " = solver.getBooleanSort();\n");
    return ret;
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
      stringBuilder.append("    Term term" + Integer.toHexString(term.hashCode())
          + " = solver.defineFun(\"" + symbol + "\", new Term[0], sort"
          + Integer.toHexString(sort.hashCode()) + ", term" + Integer.toHexString(body.hashCode())
          + ");\n");
      stringBuilder.append("    // solver.defineFun(symbol, new Term[0], sort, body); term: " + term
          + ", body: " + body + "\n");
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
    Term ret = solver.mkTerm(ITE, condition, thenTerm, elseTerm);
    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + " = solver.mkTerm(ITE, term" + Integer.toHexString(condition.hashCode()) + ", term"
        + Integer.toHexString(thenTerm.hashCode()) + ", term"
        + Integer.toHexString(elseTerm.hashCode()) + ");\n");
    stringBuilder.append(
        "    // solver.mkTerm(ITE, condition, thenTerm, elseTerm); term: " + ret + "\n");
    return ret;
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
        stringBuilder.append("    solver.setLogic(\"" + logic + "\");\n");
        Map<String, String> options = smtSettings.getSolverOptions();
        for (Map.Entry<String, String> entry : options.entrySet())
        {
          solver.setOption(entry.getKey(), entry.getValue());
          stringBuilder.append(
              "    solver.setOption(\"" + entry.getKey() + "\", \"" + entry.getValue() + "\");\n");
        }
      }
      catch (CVC5ApiException e)
      {
        throw new RuntimeException(e);
      }
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

  @Override
  public Sort visit(StringSort stringSort)
  {
    return solver.getStringSort();
  }

  public List<Triplet<String, Declaration, Term>> getTermSymbols()
  {
    return termSymbols;
  }

  public Map<String, Sort> getSortSymbols()
  {
    return sortSymbols;
  }

  public Term tupleSelect(Term tuple, int index)
  {
    Term projection = tuple.getSort().getDatatype().getConstructor(0).getSelector(index).getTerm();
    stringBuilder.append("    Term term" + Integer.toHexString(projection.hashCode())
        + " = tuple.getSort().getDatatype().getConstructor(0).getSelector(" + index
        + ").getTerm();\n");
    Term ret = solver.mkTerm(APPLY_SELECTOR, projection, tuple);
    stringBuilder.append("    Term term" + Integer.toHexString(ret.hashCode())
        + " = solver.mkTerm(APPLY_SELECTOR,  term" + Integer.toHexString(projection.hashCode())
        + ", term" + Integer.toHexString(tuple.hashCode()) + ");\n");
    stringBuilder.append(
        "    // solver.mkTerm(APPLY_SELECTOR, projection, tuple); term: " + ret + "\n");
    return ret;
  }

  public final Sort getUninterpretedSort(UninterpretedSort uninterpretedSort)
  {
    if (sortSymbols.containsKey(uninterpretedSort.getName()))
    {
      return sortSymbols.get(uninterpretedSort.getName());
    }
    else
    {
      try
      {
        Sort sort = solver.declareSort(uninterpretedSort.getName(), 0);
        sortSymbols.put(uninterpretedSort.getName(), sort);
        return sort;
      }
      catch (CVC5ApiException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  public final Kind getKind(SmtUnaryExpr.Op op)
  {
    switch (op)
    {
      case NOT: return NOT;
      case SET_COMPLEMENT: return SET_COMPLEMENT;
      case RELATION_TRANSPOSE: return RELATION_TRANSPOSE;
      case RELATION_TCLOSURE: return RELATION_TCLOSURE;
      case SET_SINGLETON: return SET_SINGLETON;
      case SET_CHOOSE: return SET_CHOOSE;
      case SET_UNIVERSE: return SET_UNIVERSE;
      case SET_EMPTY: return SET_EMPTY;
      default: throw new UnsupportedOperationException(op.toString());
    }
  }
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
      case SET_MEMBER: return SET_MEMBER;
      case SET_SUBSET: return SET_SUBSET;
      case RELATION_JOIN: return RELATION_JOIN;
      case RELATION_PRODUCT: return RELATION_PRODUCT;
      default: throw new UnsupportedOperationException(op.toString());
    }
  }
  public final Kind getKind(SmtMultiArityExpr.Op op)
  {
    switch (op)
    {
      case TUPLE: throw new UnsupportedOperationException(op.toString());
      case INSERT: return SET_INSERT;
      case DISTINCT: return DISTINCT;
      case OR: return OR;
      case AND: return AND;
      default: throw new UnsupportedOperationException();
    }
  }
  public final Kind getKind(SmtQtExpr.Op op)
  {
    switch (op)
    {
      case FORALL: return FORALL;
      case EXISTS: return EXISTS;
      default: throw new UnsupportedOperationException();
    }
  }

  public List<String> getCoreAssertions(Term[] coreTerms)
  {
    List<String> coreAssertions = new ArrayList<>();
    for (Term t : coreTerms)
    {
      for (Pair<Term, Assertion> pair : currentAssertions)
      {
        if (pair.first.equals(t))
        {
          coreAssertions.add(pair.second.getSymbolicName());
        }
      }
    }
    return coreAssertions;
  }

  public void push() throws CVC5ApiException
  {
    assertionsSizeBeforeLastPush = currentAssertions.size();
    solver.push();
    stringBuilder.append("    solver.push();\n");
  }
  public void pop() throws CVC5ApiException
  {
    currentAssertions.subList(assertionsSizeBeforeLastPush, currentAssertions.size()).clear();
    solver.pop();
    stringBuilder.append("    solver.pop();\n");
  }
  public Result checkSat() throws CVC5ApiException
  {
    Result result = solver.checkSat();
    stringBuilder.append(
        "    Result result" + Integer.toHexString(result.hashCode()) + " = solver.checkSat();\n");
    stringBuilder.append(
        "    System.out.println(result" + Integer.toHexString(result.hashCode()) + ");\n");
    return result;
  }
  public String getModel() throws CVC5ApiException
  {
    Term[] terms = new Term[termSymbols.size()];
    stringBuilder.append("    Term[] terms" + Integer.toHexString(terms.hashCode()) + " = new Term["
        + termSymbols.size() + "];\n");
    for (int i = 0; i < termSymbols.size(); i++)
    {
      terms[i] = termSymbols.get(i).third;
      stringBuilder.append("    terms" + Integer.toHexString(terms.hashCode()) + "[" + i
          + "] = term" + Integer.toHexString(terms[i].hashCode()) + ";\n");
      stringBuilder.append("    // terms" + Integer.toHexString(terms.hashCode()) + "[" + i
          + "] = " + terms[i] + "\n");
    }

    Set<Sort> sorts = new HashSet<>();
    String sortsString = "Set<Sort> sorts{} = new HashSet<>();\n";

    for (Map.Entry<String, Sort> entry : sortSymbols.entrySet())
    {
      if (entry.getValue().isUninterpretedSort())
      {
        sorts.add(entry.getValue());
        sortsString +=
            "sorts{}.add(sort" + Integer.toHexString(entry.getValue().hashCode()) + ");\n";
      }
    }

    sortsString = sortsString.replace("{}", Integer.toHexString(sorts.hashCode()));
    stringBuilder.append(sortsString);
    String model = solver.getModel(sorts.toArray(new Sort[0]), terms);
    stringBuilder.append("    String model" + Integer.toHexString(model.hashCode())
        + " = solver.getModel(sorts" + Integer.toHexString(sorts.hashCode())
        + ".toArray(new Sort[0]), terms" + Integer.toHexString(terms.hashCode()) + ");\n");
    stringBuilder.append(
        "    System.out.println(model" + Integer.toHexString(model.hashCode()) + ");\n");
    return model;
  }

  public void blockModel(BlockModelsMode blockModelsMode) throws CVC5ApiException
  {
    stringBuilder.append("    solver.blockModel(BlockModelsMode." + blockModelsMode + ");\n");
    solver.blockModel(blockModelsMode);
  }

  public Term[] getUnsatCore() throws CVC5ApiException
  {
    Term[] terms = solver.getUnsatCore();
    stringBuilder.append("    System.out.println(\"unsat core:\");\n");
    stringBuilder.append("        Term[] terms" + Integer.toHexString(terms.hashCode())
        + " = solver.getUnsatCore();\n");
    stringBuilder.append("        for(int i = 0; i < terms" + Integer.toHexString(terms.hashCode())
        + ".length; i++)\n");
    stringBuilder.append("    {\n");
    stringBuilder.append(
        "          System.out.println(terms" + Integer.toHexString(terms.hashCode()) + "[i]);\n");
    stringBuilder.append("    }\n");
    return terms;
  }

  public String getJavaCode()
  {
    return stringBuilder + "  }\n}\n";
  }
}
