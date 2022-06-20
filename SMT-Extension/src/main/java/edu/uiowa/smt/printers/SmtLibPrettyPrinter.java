package edu.uiowa.smt.printers;

import static io.github.cvc5.Kind.*;

import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;
import io.github.cvc5.*;
import java.util.List;
import java.util.Map;

public class SmtLibPrettyPrinter extends SmtLibPrinter
{
  private int tabsCount = 0;

  private void printTabs()
  {
    for (int i = 0; i < tabsCount; i++)
    {
      stringBuilder.append(" ");
    }
  }

  public SmtLibPrettyPrinter(SmtSettings smtSettings)
  {
    super(smtSettings);
  }

  public SmtLibPrettyPrinter()
  {
    super();
  }

  @Override
  public Term visit(SmtUnaryExpr expr)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(" + expr.getOp() + " ");
    tabsCount++;
    Term term = visit(expr.getExpr());
    stringBuilder.append(")");
    tabsCount -= 2;
    Kind k = getKind(expr.getOp());
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
    return term;
  }

  @Override
  public Term visit(SmtBinaryExpr expr)
  {
    if (expr.getOp() != SmtBinaryExpr.Op.TUPSEL)
    {
      tabsCount++;
      stringBuilder.append("\n");
      printTabs();
      stringBuilder.append("(" + expr.getOp() + " ");
      tabsCount++;
      Kind k = getKind(expr.getOp());
      Term A = visit(expr.getA());
      stringBuilder.append(" ");
      Term B = visit(expr.getB());
      stringBuilder.append(")");
      tabsCount -= 2;
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
  public Term visit(SmtMultiArityExpr multiArityExpression)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(" + multiArityExpression.getOp() + " ");
    Kind k = getKind(multiArityExpression.getOp());
    tabsCount++;
    Term[] terms = new Term[multiArityExpression.getExprs().size()];
    if (multiArityExpression.getExprs().size() == 1)
    {
      terms[0] = visit(multiArityExpression.getExprs().get(0));
    }
    else if (multiArityExpression.getExprs().size() > 1)
    {
      for (int i = 0; i < multiArityExpression.getExprs().size() - 1; ++i)
      {
        terms[i] = visit(multiArityExpression.getExprs().get(i));
        stringBuilder.append(" ");
      }
      int index = multiArityExpression.getExprs().size() - 1;
      terms[index] = visit(multiArityExpression.getExprs().get(index));
      return solver.mkTerm(k, terms);
    }
    else
    {
      throw new RuntimeException("");
    }
    stringBuilder.append(")");
    tabsCount -= 2;
    return solver.mkTerm(k, terms);
  }

  @Override
  public Term visit(SmtQtExpr smtQtExpr)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
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
    tabsCount++;
    Term body = visit(smtQtExpr.getExpr());
    stringBuilder.append(")");
    tabsCount -= 2;
    Term bvl = solver.mkTerm(VARIABLE_LIST, boundVars);
    return solver.mkTerm(k, new Term[] {bvl, body});
  }

  @Override
  public Term visit(SmtLetExpr let)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(let (");
    for (Map.Entry<SmtVariable, SmtExpr> letVar : let.getLetVariables().entrySet())
    {
      tabsCount++;
      stringBuilder.append("\n");
      printTabs();
      stringBuilder.append("(");
      stringBuilder.append(TranslatorUtils.sanitizeWithBars(letVar.getKey())).append(" ");
      this.visit(letVar.getValue());
      stringBuilder.append(")");
      tabsCount--;
    }
    stringBuilder.append(") ");
    tabsCount++;
    this.visit(let.getSmtExpr());
    stringBuilder.append(")");
    tabsCount -= 2;
    // ToDo: figure what do here
    return null;
  }

  @Override
  public Term visit(SmtExpr smtExpr)
  {
    Term term = super.visit(smtExpr);
    if (!smtExpr.getComment().isEmpty())
    {
      stringBuilder.append("; " + smtExpr.getComment() + "\n");
      printTabs();
    }
    return term;
  }
}
