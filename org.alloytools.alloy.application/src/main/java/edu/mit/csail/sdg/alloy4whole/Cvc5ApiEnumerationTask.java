package edu.mit.csail.sdg.alloy4whole;

import static edu.mit.csail.sdg.alloy4.A4Preferences.ImplicitThis;

import edu.mit.csail.sdg.alloy4.A4Preferences;
import edu.mit.csail.sdg.alloy4.Version;
import edu.mit.csail.sdg.alloy4.WorkerEngine;
import edu.mit.csail.sdg.alloy4whole.instances.AlloySolution;
import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.alloy2smt.Utils;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.smt.printers.Cvc5ApiVisitor;
import edu.uiowa.smt.smtAst.Declaration;
import edu.uiowa.smt.smtAst.SmtModel;
import io.github.cvc5.*;
import io.github.cvc5.modes.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class Cvc5ApiEnumerationTask implements WorkerEngine.WorkerTask
{
  private final String xmlFileName;
  private Translation translation;
  private AlloySolution alloySolution;
  private Map<String, String> alloyFiles;
  private int commandIndex;
  private String originalFileName;

  Cvc5ApiEnumerationTask(String xmlFileName) throws Exception
  {
    this.xmlFileName = xmlFileName;
  }

  @Override
  public void run(WorkerEngine.WorkerCallback workerCallback) throws Exception
  {
    try
    {
      if (!xmlFileName.equals(Cvc5ApiTask.lastXmlFile))
      {
        workerCallback.callback(new Object[] {
            "pop", "You can only enumerate the solutions of the last executed command."});
        return;
      }


      // (block-model)
      if (A4Preferences.CvcBlockModel.get().equals(A4Preferences.CvcLiterals))
      {
        Cvc5ApiTask.cvc5ApiVisitor.blockModel(BlockModelsMode.LITERALS);
      }
      else
      {
        Cvc5ApiTask.cvc5ApiVisitor.blockModel(BlockModelsMode.VALUES);
      }
      // (check-sat)
      Result result = Cvc5ApiTask.cvc5ApiVisitor.checkSat();

      if (result.isSat())
      {
        // get a new model and save it
        prepareInstance(commandIndex);
        // tell alloySolution user interface that the last instance has changed
        workerCallback.callback(new Object[] {"declare", xmlFileName});
      }
      else if (result.isUnsat())
      {
        workerCallback.callback(new Object[] {"pop",
            "There are no more satisfying instances.\n\n"
                + "Note: due to symmetry breaking and other optimizations,\n"
                + "some equivalent solutions may have been omitted."});
      }
      else
      {
        workerCallback.callback(new Object[] {"pop", "cvc5 Api solver returned unknown."});
      }
    }
    catch (Exception exception)
    {
      JOptionPane.showMessageDialog(null, exception);
      StringWriter stringWriter = new StringWriter();
      exception.printStackTrace(new PrintWriter(stringWriter));
      throw new Exception(stringWriter.toString());
    }
  }

  private Translation translateToSMT() throws IOException
  {
    int resolutionMode = (Version.experimental && ImplicitThis.get()) ? 2 : 1;
    Cvc5ApiTask.setAlloySettings();
    Translation translation =
        Utils.translate(alloyFiles, originalFileName, resolutionMode, Cvc5ApiTask.alloySettings);
    return translation;
  }

  private void prepareInstance(int commandIndex) throws Exception
  {
    Cvc5ApiVisitor cvc5ApiVisitor = Cvc5ApiTask.cvc5ApiVisitor;
    List<Triplet<String, Declaration, Term>> termSymbols = cvc5ApiVisitor.getTermSymbols();
    Term[] terms = new Term[termSymbols.size()];
    for (int j = 0; j < termSymbols.size(); j++)
    {
      terms[j] = termSymbols.get(j).third;
    }
    Set<Sort> sorts = new HashSet<>();
    Map<String, Sort> sortSymbols = cvc5ApiVisitor.getSortSymbols();
    for (Map.Entry<String, Sort> entry : sortSymbols.entrySet())
    {
      if (entry.getValue().isUninterpretedSort())
      {
        sorts.add(entry.getValue());
      }
    }

    String smtModel = cvc5ApiVisitor.getModel();
    Command command = translation.getCommands().get(commandIndex);

    SmtModel model = Cvc5ApiTask.parseModel(smtModel);

    File xmlFile = new File(xmlFileName);

    String xmlFilePath = xmlFile.getAbsolutePath();

    Cvc5ApiTask.writeModelToAlloyXmlFile(
        translation, model, xmlFilePath, originalFileName, command, alloySolution.getAlloyFiles());
  }
}