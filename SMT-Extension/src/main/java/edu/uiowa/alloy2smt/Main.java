/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt;

import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.alloy2smt.utils.AlloySettings;
import edu.uiowa.smt.printers.Cvc5Visitor;
import edu.uiowa.smt.printers.SmtLibPrinter;
import edu.uiowa.smt.smtAst.Declaration;
import edu.uiowa.smt.smtAst.SmtScript;
import io.github.cvc5.*;
import io.github.cvc5.modes.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.cli.*;

public class Main
{
  public static final String SEP = File.separator;
  public static final String OUTPUT_DIR = System.getProperty("java.io.tmpdir");
  public static final String DEFAULT_OUTPUT_FILE = "output.smt2";

  public static final BlockModelsMode blockModelsMode = BlockModelsMode.LITERALS;

  public static boolean isValidInputFilePath(String path)
  {
    File inputFile = new File(path);

    return inputFile.exists() && inputFile.canRead() && path.endsWith(".als");
  }

  public static boolean isValidOutputFilePath(String path) throws IOException
  {
    try
    {
      Paths.get(path);
    }
    catch (InvalidPathException | NullPointerException ex)
    {
      return false;
    }

    File outputFile = new File(path);

    if (outputFile.getParentFile() != null)
    {
      outputFile.getParentFile().mkdirs();
    }
    outputFile.createNewFile();
    return true;
  }

  public static void main(String[] args)
  {
    Options options = new Options();
    CommandLineParser commandLineParser = new DefaultParser();

    options.addOption(
        Option.builder("i").longOpt("input").desc("Input Alloy model").hasArg().build());
    options.addOption(
        Option.builder("o").longOpt("output").desc("SMT-LIB model output").hasArg().build());

    try
    {
      CommandLine command = commandLineParser.parse(options, args);

      Translation translation;
      String defaultOutputFile;

      AlloySettings settings = AlloySettings.Default;
      settings.putSolverOption(AlloySettings.PRODUCE_UNSAT_CORES, "true");
      settings.putSolverOption(AlloySettings.DAG_THRESH, "0");
      if (command.hasOption("i"))
      {
        String inputFile = command.getOptionValue("i").trim();

        if (isValidInputFilePath(inputFile))
        {
          String alloy =
              new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);

          translation = Utils.translate(alloy, settings);
          defaultOutputFile = OUTPUT_DIR + SEP + new File(inputFile).getName() + ".smt2";
        }
        else
        {
          throw new Exception("Can not open file " + inputFile);
        }
      }
      else
      {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine())
        {
          stringBuilder.append(scanner.nextLine()).append("\n");
        }

        translation = Utils.translate(stringBuilder.toString(), AlloySettings.Default);
        defaultOutputFile = DEFAULT_OUTPUT_FILE + ".smt2";
      }

      File outputFile = null;

      if (command.hasOption("o"))
      {
        if (isValidOutputFilePath(command.getOptionValue("o")))
        {
          outputFile = new File(command.getOptionValue("o").trim());
        }
      }
      if (outputFile == null)
      {
        outputFile = new File(defaultOutputFile);
      }

      SmtScript optimizedScript = translation.getOptimizedSmtScript();
      outputTranslation(translation, outputFile, optimizedScript);
      Solve(translation, optimizedScript, settings);
    }
    catch (ParseException exception)
    {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar alloy2smt.jar ", options);
    }
    catch (Exception exception)
    {
      exception.printStackTrace();
    }
  }

  private static void outputTranslation(
      Translation translation, File outputFile, SmtScript optimizedScript)
      throws FileNotFoundException
  {
    try (Formatter formatter = new Formatter(outputFile))
    {
      formatter.format("%s\n", optimizedScript);

      System.out.println("\n");
      System.out.println(optimizedScript);

      // translate all alloy commands
      for (int i = 0; i < translation.getCommands().size(); i++)
      {
        String commandTranslation = translation.getOptimizedSmtScript(i).toString();

        commandTranslation = SmtLibPrinter.PUSH + "\n" + commandTranslation
            + SmtLibPrinter.CHECK_SAT + "\n" + SmtLibPrinter.GET_MODEL + "\n" + SmtLibPrinter.POP
            + "\n";

        formatter.format("%s\n", commandTranslation);
        System.out.println("\n" + commandTranslation);
        System.out.println("\nThe SMT-LIB model was generated at: " + outputFile.getAbsolutePath());
      }
    }
  }

  private static void Solve(
      Translation translation, SmtScript optimizedScript, AlloySettings settings)
      throws CVC5ApiException
  {
    Cvc5Visitor cvc5Visitor = optimizedScript.toCvc5(settings);
    Solver solver = cvc5Visitor.getSolver();
    for (int i = 0; i < translation.getCommands().size(); i++)
    {
      solver.push();
      System.out.println(
          "-------------------------------------------------------------------------------------------");
      System.out.println("Solving command: " + translation.getCommands().get(i));
      cvc5Visitor.visit(translation.getOptimizedSmtScript(i));
      Result result = solver.checkSat();
      System.out.println("Sat result: " + result);
      if (result.isSat())
      {
        List<Triplet<String, Declaration, Term>> termSymbols = cvc5Visitor.getTermSymbols();
        Term[] terms = new Term[termSymbols.size()];
        for (int j = 0; j < termSymbols.size(); j++)
        {
          terms[j] = termSymbols.get(j).third;
        }
        Set<Sort> sorts = new HashSet<>();
        Map<String, Sort> sortSymbols = cvc5Visitor.getSortSymbols();
        for (Map.Entry<String, Sort> entry : sortSymbols.entrySet())
        {
          if (entry.getValue().isUninterpretedSort())
          {
            sorts.add(entry.getValue());
          }
        }
        System.out.println(solver.getModel(sorts.toArray(new Sort[0]), terms));
        System.out.println(
            "-------------------------------------------------------------------------------------------");
        solver.blockModel(blockModelsMode);
        Result result2 = solver.checkSat();
        if (result2.isSat())
        {
          System.out.println("A second instance was found after blocking models with "
              + blockModelsMode + " mode:");
          System.out.println(solver.getModel(sorts.toArray(new Sort[0]), terms));
        }
        else if (result2.isUnsat())
        {
          System.out.println("No more instances were found after blocking models with "
              + blockModelsMode + " mode.");
        }
      }
      if (result.isUnsat())
      {
        Term[] unsatCore = solver.getUnsatCore();
        System.out.println("Unsat core: " + Arrays.asList(unsatCore));
      }
      solver.pop();
    }
  }
}
