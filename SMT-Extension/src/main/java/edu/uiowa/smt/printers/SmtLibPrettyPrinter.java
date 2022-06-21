package edu.uiowa.smt.printers;

import edu.uiowa.smt.TranslatorUtils;
import edu.uiowa.smt.smtAst.*;
import io.github.cvc5.Term;
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
  public Term visit(SmtUnaryExpr unaryExpression)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(" + unaryExpression.getOp() + " ");
    tabsCount++;
    this.visit(unaryExpression.getExpr());
    stringBuilder.append(")");
    tabsCount -= 2;
    return null;
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
      this.visit(expr.getA());
      stringBuilder.append(" ");
      this.visit(expr.getB());
      stringBuilder.append(")");
      tabsCount -= 2;
    }
    else
    {
      stringBuilder.append("((_ " + expr.getOp() + " ");
      stringBuilder.append(((IntConstant) expr.getA()).getValue());
      stringBuilder.append(") ");
      this.visit(expr.getB());
      stringBuilder.append(")");
    }
    return null;
  }

  @Override
  public Term visit(SmtMultiArityExpr expr)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(" + expr.getOp() + " ");
    tabsCount++;
    if (expr.getExprs().size() == 1)
    {
      this.visit(expr.getExprs().get(0));
    }
    else if (expr.getExprs().size() > 1)
    {
      for (int i = 0; i < expr.getExprs().size() - 1; ++i)
      {
        this.visit(expr.getExprs().get(i));
        stringBuilder.append(" ");
      }
      this.visit(expr.getExprs().get(expr.getExprs().size() - 1));
    }
    else
    {
      throw new RuntimeException("");
    }
    stringBuilder.append(")");
    tabsCount -= 2;
    return null;
  }

  @Override
  public Term visit(SmtQtExpr expr)
  {
    tabsCount++;
    stringBuilder.append("\n");
    printTabs();
    stringBuilder.append("(" + expr.getOp() + " (");
    for (SmtVariable boundVariable : expr.getVariables())
    {
      this.visit(boundVariable);
    }
    stringBuilder.append(") ");
    tabsCount++;
    this.visit(expr.getExpr());
    stringBuilder.append(")");
    tabsCount -= 2;
    return null;
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
    return null;
  }

  @Override
  public Term visit(SmtExpr smtExpr)
  {
    super.visit(smtExpr);
    if (!smtExpr.getComment().isEmpty())
    {
      stringBuilder.append("; " + smtExpr.getComment() + "\n");
      printTabs();
    }
    return null;
  }
}
