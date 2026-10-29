/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 * 
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt;
import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.alloy2smt.translators.TranslatorOptions;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.printers.SmtLibPrettyPrinter;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.Scanner;

public class Main
{
    public static final String SEP                  = File.separator;
    public static final String OUTPUT_DIR           = System.getProperty("java.io.tmpdir");
    public static final String DEFAULT_OUTPUT_FILE  = "output.smt2";

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

    /**
     * Writes a standalone SMT-LIB script for each command of the model: name_index.smt2.
     * A command that can not be translated is reported on the standard error.
     */
    private static void exportCommands(String inputFile, String directory, TranslatorOptions options, boolean includeScope) throws IOException
    {
        Translation translation = Utils.translateFromFile(inputFile, options);
        String name = new File(inputFile).getName().replaceFirst("\\.als$", "");
        Files.createDirectories(Paths.get(directory));
        for (int i = 0; i < translation.getCommands().size(); i++)
        {
            try
            {
                String script = translation.translateCommandScript(i, includeScope);
                Command alloyCommand = translation.getCommands().get(i);
                String header = String.format("; %s\n; %s\n", inputFile, alloyCommand.toString().replace('\n', ' '));
                Files.write(Paths.get(directory, name + "_" + i + ".smt2"), (header + script).getBytes(StandardCharsets.UTF_8));
            }
            catch (Exception exception)
            {
                System.err.println(inputFile + " command " + i + ": " + exception);
            }
        }
    }

    public static void main(String[] args)
    {
        Options             options             = new Options();
        CommandLineParser   commandLineParser   = new DefaultParser();        
        
        options.addOption(Option.builder("i").longOpt("input").desc("Input Alloy model").hasArg().build());
        options.addOption(Option.builder("o").longOpt("output").desc("SMT-LIB model output").hasArg().build());
        options.addOption(Option.builder("d").longOpt("directory").desc("Write one standalone SMT-LIB script per command in this directory").hasArg().build());
        options.addOption(Option.builder("q").longOpt("quantifiers").desc("Translation of quantifiers: 'hybrid' (forall, exists, set.filter; default), 'fo' (forall, exists) or 'set' (set.all, set.some, set.filter)").hasArg().build());
        options.addOption(Option.builder("s").longOpt("scope").desc("Bound the signatures by the scope of each command").build());

        try
        {
            CommandLine command = commandLineParser.parse(options, args);

            TranslatorOptions translatorOptions = new TranslatorOptions();
            if (command.hasOption("q"))
            {
                String quantifiers = command.getOptionValue("q").trim();
                switch (quantifiers)
                {
                    case "fo": translatorOptions.quantifiers = TranslatorOptions.Quantifiers.FIRST_ORDER; break;
                    case "set": translatorOptions.quantifiers = TranslatorOptions.Quantifiers.SET; break;
                    default: translatorOptions.quantifiers = TranslatorOptions.Quantifiers.HYBRID; break;
                }
            }
            boolean includeScope = command.hasOption("s");

            if (command.hasOption("d"))
            {
                exportCommands(command.getOptionValue("i").trim(), command.getOptionValue("d").trim(), translatorOptions, includeScope);
                return;
            }

            Translation translation;
            String      defaultOutputFile;

            if (command.hasOption("i"))
            {
                String inputFile = command.getOptionValue("i").trim();

                if (isValidInputFilePath(inputFile))
                {
                    translation       = Utils.translateFromFile(inputFile, translatorOptions);
                    defaultOutputFile = OUTPUT_DIR + SEP + new File(inputFile).getName() + ".smt2";
                } else
                {
                    throw new Exception("Can not open file " + inputFile);
                }
            }
            else
            {
                StringBuilder stringBuilder = new StringBuilder();
                Scanner scanner             = new Scanner(System.in);

                while(scanner.hasNextLine())
                {
                    stringBuilder.append(scanner.nextLine()).append("\n");
                }

                translation         = Utils.translate(stringBuilder.toString());
                defaultOutputFile   = DEFAULT_OUTPUT_FILE + ".smt2";
            }

            File    outputFile  = null;

            if(command.hasOption("o"))
            {
                if(isValidOutputFilePath(command.getOptionValue("o")))
                {
                    outputFile = new File(command.getOptionValue("o").trim());
                }
            }
            if(outputFile == null)
            {
                outputFile = new File(defaultOutputFile);
            }
            try (Formatter formatter = new Formatter(outputFile))
            {
                // push and pop need incremental solving, set before the logic
                formatter.format("%s\n", "(set-option :incremental true)");
                formatter.format("%s\n", translation.getSmtScript());

                System.out.println("\n");
                System.out.println(translation.getSmtScript());

                // translate all alloy commands
                for (int i = 0; i < translation.getCommands().size(); i++)
                {
                    String commandTranslation = translation.translateCommand(i, includeScope);

                    commandTranslation         =    SmtLibPrettyPrinter.PUSH + "\n" + commandTranslation +
                            SmtLibPrettyPrinter.CHECK_SAT + "\n" + SmtLibPrettyPrinter.GET_MODEL + "\n" +
                            SmtLibPrettyPrinter.POP + "\n";

                    formatter.format("%s\n", commandTranslation);
                    System.out.println("\n" + commandTranslation);
                }
            }
            System.out.println("\nThe SMT-LIB model was generated at: " + outputFile.getAbsolutePath());
        }
        catch (ParseException exception)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "java -jar alloy2smt.jar ", options );
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
}
