/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.smtAst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionDeclaration extends Declaration
{
  private final List<SmtSort> inputSorts;

  public FunctionDeclaration(String name, List<SmtSort> inputSort, SmtSort outputSort, boolean isOriginal)
  {
    super(name, outputSort, isOriginal);

    this.inputSorts = inputSort;

    if (this.inputSorts.isEmpty())
    {
      variable = new Variable(this);
    }
    else
    {
      variable = null;
    }
  }

  public FunctionDeclaration(String name, SmtSort inputSort, SmtSort outputSort, boolean isOriginal)
  {
    super(name, outputSort, isOriginal);
    this.inputSorts = Arrays.asList(inputSort);

    if (this.inputSorts.isEmpty())
    {
      variable = new Variable(this);
    }
    else
    {
      variable = null;
    }
  }

  public FunctionDeclaration(String name, SmtSort outputSort, boolean isOriginal)
  {
    super(name, outputSort, isOriginal);
    this.inputSorts = new ArrayList<>();
    this.variable = new Variable(this);
  }

  public FunctionDeclaration(String name, boolean isOriginal, SmtSort outputSort, SmtSort... inputSorts)
  {
    super(name, outputSort, isOriginal);
    this.inputSorts = Arrays.asList(inputSorts);

    if (this.inputSorts.isEmpty())
    {
      variable = new Variable(this);
    }
    else
    {
      variable = null;
    }
  }

  public List<SmtSort> getInputSorts()
  {
    return this.inputSorts;
  }

  @Override
  public Variable getVariable()
  {
    if (this.variable != null)
    {
      return this.variable;
    }
    // this is a function call
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(SmtAstVisitor visitor)
  {
    visitor.visit(this);
  }

  public SmtSort getSort(int index)
  {
    if (index >= this.inputSorts.size())
    {
      throw new RuntimeException("Argument index out of range");
    }

    return inputSorts.get(index);
  }
}
