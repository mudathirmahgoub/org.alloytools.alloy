package edu.uiowa.alloy2smt.utils;

import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.smt.cvc5.Cvc5Process;
import edu.uiowa.smt.printers.SmtLibPrettyPrinter;

import java.util.ArrayList;
import java.util.List;

public class Cvc5Task
{
    public Cvc5Process cvc5Process;

    private int timeout;

    public Cvc5Task(int timeout)
    {
        this.timeout = timeout;
    }

    public Cvc5Task()
    {
        this.timeout = 60000;
    }

    public List<CommandResult> run(Translation translation, boolean includeScope) throws Exception
    {
        List<CommandResult> commandResults = new ArrayList<>();
        String smtScript = translation.getSmtScript();
        if (smtScript != null)
        {
            startProcess(smtScript);
            try
            {
                for (int index = 0; index < translation.getCommands().size(); index++)
                {
                    // (push)
                    cvc5Process.sendCommand(SmtLibPrettyPrinter.PUSH);
                    CommandResult commandResult = solveCommand(index, includeScope, translation);
                    // (pop)
                    cvc5Process.sendCommand(SmtLibPrettyPrinter.POP);
                    commandResults.add(commandResult);
                }
            }
            finally
            {
                cvc5Process.destroy();
            }
            return commandResults;
        }
        return new ArrayList<>();
    }

    public CommandResult run(Translation translation, boolean includeScope, int commandIndex ) throws Exception
    {
        startProcess(translation.getSmtScript());
        try
        {
            return solveCommand(commandIndex, includeScope, translation);
        }
        finally
        {
            cvc5Process.destroy();
        }
    }

    // options that are set before the script, they can not be changed after the logic is set
    private void startProcess(String smtScript) throws Exception
    {
        cvc5Process = Cvc5Process.start();
        cvc5Process.sendCommand("(set-option :incremental true)\n(set-option :tlimit-per " + timeout + ")");
        cvc5Process.sendCommand(smtScript);
    }

    private CommandResult solveCommand(int index, boolean includeScope, Translation translation) throws Exception
    {
        String commandTranslation = translation.translateCommand(index, includeScope);
        Command command = translation.getCommands().get(index);
        String result = cvc5Process.sendCommand(commandTranslation + SmtLibPrettyPrinter.CHECK_SAT);

        String smt = translation.getSmtScript() + commandTranslation + SmtLibPrettyPrinter.CHECK_SAT;
        CommandResult commandResult = new CommandResult(index, command, smt, result);

        if (result.equals("sat"))
        {
            commandResult.model = cvc5Process.sendCommand(SmtLibPrettyPrinter.GET_MODEL);
            commandResult.smtModel = commandResult.parseModel(commandResult.model);
        }
        return commandResult;
    }
}
