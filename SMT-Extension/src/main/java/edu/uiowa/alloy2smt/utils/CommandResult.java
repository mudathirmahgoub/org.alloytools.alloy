package edu.uiowa.alloy2smt.utils;

import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.smt.SmtResult;

public class CommandResult extends SmtResult
{
  public final int index;
  public final Command command;

  public CommandResult(int index, Command command, String smtProgram, String satResult)
  {
    super(smtProgram, satResult);
    this.index = index;
    this.command = command;
  }
}
