package edu.uiowa.smt.smtAst;

import static io.github.cvc5.Kind.*;

import edu.uiowa.smt.TranslatorUtils;
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
      // todo: handle this
      throw new UnsupportedOperationException(((SmtSort) smtExpr).getName());
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
  public void visit(SmtModel model)
  {
  }

  @Override
  public void visit(SmtScript script)
  {
    for (SmtSort sort : script.getSorts())
    {
      visit(sort);
    }
    for (Declaration function : script.getFunctions())
    {
      visit(function);
    }
    for (Assertion assertion : script.getAssertions())
    {
      visit(assertion);
    }
  }

  @Override
  public Term visit(SmtBinaryExpr expr)
  {
    Kind k = getKind(expr.getOp());
    Term A = visit(expr.getA());
    Term B = visit(expr.getB());
    try
    {
      return solver.mkTerm(k, A, B);
    }
    catch (Exception e)
    {
      String message = expr.getA() + " " + k.toString() + " " + expr.getB() + e.getMessage();
      throw new RuntimeException(message, e);
    }
  }

  public Kind getKind(SmtBinaryExpr.Op op)
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
  public Sort visit(IntSort intSort)
  {
    return solver.getIntegerSort();
  }

  @Override
  public Term visit(SmtQtExpr expr)
  {
    Kind k = getKind(expr.getOp());
    Term[] vars = new Term[expr.getVariables().size()];
    for (int i = 0; i < expr.getVariables().size(); i++)
    {
      vars[i] = visit(expr.getVariables().get(i));
    }
    Term body = visit(expr.getExpr());
    Term bvl = solver.mkTerm(VARIABLE_LIST, vars);
    return solver.mkTerm(k, new Term[] {bvl, body});
  }

  public Kind getKind(SmtQtExpr.Op op)
  {
    switch (op)
    {
      case FORALL: return FORALL;
      case EXISTS: return EXISTS;
      default: throw new UnsupportedOperationException();
    }
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
    for (int i = 0; i < tupleSort.elementSorts.size(); i++)
    {
      sorts[i] = visit(tupleSort.elementSorts.get(i));
    }
    return solver.mkTupleSort(sorts);
  }

  @Override
  public Term visit(SmtUnaryExpr expr)
  {
    Kind k = getKind(expr.getOp());
    Term term;
    if (k == SET_EMPTY)
    {
      Sort sort = this.visit(expr.getSort());
      term = solver.mkEmptySet(sort);
    }
    else if (k == SET_UNIVERSE)
    {
      Sort sort = this.visit(expr.getSort());
      term = solver.mkUniverseSet(sort);
    }
    else
    {
      term = visit(expr.getExpr());
    }
    // todo: handle universe set and empty set cases
    return solver.mkTerm(k, term);
  }

  public Kind getKind(SmtUnaryExpr.Op op)
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
  public Sort visit(UninterpretedSort uninterpretedSort)
  {
    return getUninterpretedSort(uninterpretedSort);
  }

  protected Sort getUninterpretedSort(UninterpretedSort uninterpretedSort)
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
  public Term visit(IntConstant intConstant)
  {
    int value = Integer.parseInt(intConstant.getValue());
    return solver.mkInteger(value);
  }

  @Override
  public Term visit(Variable variable)
  {
    return getTerm(variable);
  }

  public Term getTerm(Variable variable)
  {
    String symbol = TranslatorUtils.sanitizeWithBars(variable.getDeclaration());
    for (int i = termSymbols.size() - 1; i >= 0; i--)
    {
      if (termSymbols.get(i).first.equals(symbol)
          && termSymbols.get(i).second.equals(variable.getDeclaration()))
      {
        return termSymbols.get(i).third;
      }
    }
    // create term for this variable
    Term term = visit(variable.getDeclaration());
    // ToDo: review when there is a collision in names in different scopes
    return term;
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
  public Term visit(BoolConstant booleanConstant)
  {
    return null;
  }

  @Override
  public void visit(Assertion assertion)
  {
    visit(assertion.getSmtExpr());
  }

  @Override
  public Term visit(SmtMultiArityExpr expression)
  {
    for (SmtExpr expr : expression.getExprs())
    {
      visit(expr);
    }
    return null;
  }

  protected Kind getKind(SmtMultiArityExpr.Op op)
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
  public Term visit(SmtCallExpr callExpression)
  {
    for (SmtExpr expr : callExpression.getArguments())
    {
      visit(expr);
    }
    return null;
  }

  @Override
  public Term visit(SmtVariable smtVariable)
  {
    Sort sort = visit(smtVariable.getSort());
    String symbol = TranslatorUtils.sanitizeWithBars(smtVariable);
    Term term = solver.mkVar(sort, symbol);
    termSymbols.add(new Triplet<>(symbol, smtVariable, term));
    return term;
  }

  @Override
  public Sort visit(BoolSort boolSort)
  {
    return solver.getBooleanSort();
  }

  @Override
  public Term visit(SmtLetExpr letExpression)
  {
    for (Map.Entry<SmtVariable, SmtExpr> entry : letExpression.getLetVariables().entrySet())
    {
      visit(entry.getKey());
      visit(entry.getValue());
    }
    visit(letExpression.getSmtExpr());
    // ToDo: find a way for terms
    return null;
  }

  @Override
  public Term visit(SmtIteExpr iteExpression)
  {
    Term condition = visit(iteExpression.getCondExpr());
    Term thenTerm = visit(iteExpression.getThenExpr());
    Term elseTerm = visit(iteExpression.getElseExpr());
    return solver.mkTerm(ITE, condition, thenTerm, elseTerm);
  }

  @Override
  public void visit(UninterpretedConstant uninterpretedConstant)
  {
    visit(uninterpretedConstant.getSort());
  }

  @Override
  public void visit(SmtSettings smtSettings)
  {
  }

  @Override
  public void visit(SmtValues smtValues)
  {
  }

  @Override
  public void visit(ExpressionValue expressionValue)
  {
  }

  @Override
  public void visit(SmtUnsatCore smtUnsatCore)
  {
  }
}
