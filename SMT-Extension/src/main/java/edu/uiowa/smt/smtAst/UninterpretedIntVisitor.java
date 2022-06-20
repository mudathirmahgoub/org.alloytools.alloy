package edu.uiowa.smt.smtAst;

import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.smtAst.AbstractSmtAstVisitor;
import edu.uiowa.smt.smtAst.UninterpretedSort;
import io.github.cvc5.Sort;

public class UninterpretedIntVisitor extends AbstractSmtAstVisitor
{
  private boolean uninterpretedIntUsed = false;

  public UninterpretedIntVisitor()
  {
  }

  public boolean isUninterpretedIntUsed()
  {
    return uninterpretedIntUsed;
  }

  @Override
  public Sort visit(UninterpretedSort uninterpretedSort)
  {
    if (uninterpretedSort.equals(AbstractTranslator.uninterpretedInt))
    {
      this.uninterpretedIntUsed = true;
    }
    return sortMap.get(uninterpretedSort.getName());
  }
}