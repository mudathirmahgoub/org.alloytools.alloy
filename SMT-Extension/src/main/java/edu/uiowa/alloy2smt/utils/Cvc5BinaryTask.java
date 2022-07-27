package edu.uiowa.alloy2smt.utils;

import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.smt.cvc5.Cvc5BinaryProcess;
import edu.uiowa.smt.printers.SmtLibPrinter;

import java.util.ArrayList;
import java.util.List;

public class Cvc5BinaryTask
{
  public Cvc5BinaryProcess cvc5BinaryProcess;

  public Cvc5BinaryTask()
  {
  }

  public List<CommandResult> run(Translation translation, boolean includeScope) throws Exception
  {
    List<CommandResult> commandResults = new ArrayList<>();
    String smtScript = translation.getOptimizedSmtScript().print(translation.getAlloySettings());
    if (smtScript != null)
    {
      cvc5BinaryProcess = Cvc5BinaryProcess.start();

      cvc5BinaryProcess.sendCommand(smtScript);

      // surround each command except the last one with (push) and (pop)
      for (int index = 0; index < translation.getCommands().size(); index++)
      {
        // (push)
        cvc5BinaryProcess.sendCommand(SmtLibPrinter.PUSH);
        CommandResult commandResult = solveCommand(index, includeScope, translation);
        // (pop)
        cvc5BinaryProcess.sendCommand(SmtLibPrinter.POP);
        commandResults.add(commandResult);
      }
      return commandResults;
    }
    return new ArrayList<>();
  }

  public CommandResult run(Translation translation, boolean includeScope, int commandIndex) throws Exception
  {
    String smtScript = translation.getOptimizedSmtScript().print(translation.getAlloySettings());
    cvc5BinaryProcess = Cvc5BinaryProcess.start();

    cvc5BinaryProcess.sendCommand(smtScript);

    CommandResult commandResult = solveCommand(commandIndex, includeScope, translation);

    return commandResult;
  }

  private CommandResult solveCommand(int index, boolean includeScope, Translation translation) throws Exception
  {
    Command command = translation.getCommands().get(index);
    String commandTranslation = translation.getOptimizedSmtScript(index).toString();
    String result = cvc5BinaryProcess.sendCommand(commandTranslation + SmtLibPrinter.CHECK_SAT);
    String smt = translation.getOptimizedSmtScript() + commandTranslation + SmtLibPrinter.CHECK_SAT;
    CommandResult commandResult = new CommandResult(index, command, smt, result);

    if (result.equals("sat"))
    {
      commandResult.model = cvc5BinaryProcess.sendCommand(SmtLibPrinter.GET_MODEL);
      commandResult.smtModel = commandResult.parseModel(commandResult.model);
    }
    return commandResult;
  }
}
