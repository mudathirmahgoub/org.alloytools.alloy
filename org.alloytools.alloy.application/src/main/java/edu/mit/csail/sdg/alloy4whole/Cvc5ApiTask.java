package edu.mit.csail.sdg.alloy4whole;

import static edu.mit.csail.sdg.alloy4.A4Preferences.*;

import edu.mit.csail.sdg.alloy4.*;
import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4whole.instances.*;
import edu.mit.csail.sdg.ast.Command;
import edu.uiowa.alloy2smt.Utils;
import edu.uiowa.alloy2smt.mapping.Mapper;
import edu.uiowa.alloy2smt.mapping.MappingField;
import edu.uiowa.alloy2smt.mapping.MappingSignature;
import edu.uiowa.alloy2smt.mapping.MappingType;
import edu.uiowa.alloy2smt.translators.Translation;
import edu.uiowa.alloy2smt.utils.AlloySettings;
import edu.uiowa.alloy2smt.utils.AlloyUnsatCore;
import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.parser.Cvc5SmtModelVisitor;
import edu.uiowa.smt.parser.antlr.Cvc5SmtLexer;
import edu.uiowa.smt.parser.antlr.Cvc5SmtParser;
import edu.uiowa.smt.printers.Cvc5Visitor;
import edu.uiowa.smt.smtAst.*;
import io.github.cvc5.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Cvc5ApiTask implements WorkerEngine.WorkerTask
{
  public static final String tempDirectory = System.getProperty("java.io.tmpdir");

  private final Map<String, String> alloyFiles;
  private final String originalFileName;
  private final int resolutionMode;
  private final int targetCommandIndex;
  // store the results of "Execute All" command
  private final List<CommandResult> commandResults = new ArrayList<>();
  private WorkerEngine.WorkerCallback workerCallback;
  private Translation translation;
  public static AlloySettings alloySettings = AlloySettings.getInstance();
  public static String lastXmlFile;

  static Cvc5Visitor cvc5Visitor;

  Cvc5ApiTask(Map<String, String> alloyFiles,
              String originalFileName,
              int resolutionMode,
              int targetCommandIndex)
  {
    this.alloyFiles = alloyFiles;
    this.originalFileName = originalFileName;
    this.resolutionMode = resolutionMode;
    this.targetCommandIndex = targetCommandIndex;
  }

  @Override
  public void run(WorkerEngine.WorkerCallback workerCallback) throws Exception
  {
    try
    {
      this.workerCallback = workerCallback;

      final long startTranslate = System.currentTimeMillis();

      translation = translateToSMT();

      final long endTranslate = System.currentTimeMillis();

      SmtScript optimizedScript = translation.getOptimizedSmtScript();
      cvc5Visitor = optimizedScript.toCvc5(translation.getAlloySettings());
      Solver solver = cvc5Visitor.getSolver();

      CommandResult commandResult;

      // execute all commands if targetCommandIndex < 0
      if (targetCommandIndex < 0)
      {
        // surround each command except the last one with (push) and (pop)
        for (int index = 0; index < translation.getCommands().size() - 1; index++)
        {
          // (push)
          cvc5Visitor.push();
          commandResult = solveCommand(index, solver, cvc5Visitor);
          // (pop)
          cvc5Visitor.pop();
          this.commandResults.add(commandResult);
        }

        // solve the last command without push and pop to view multiple models if sat
        commandResult = solveCommand(translation.getCommands().size() - 1, solver, cvc5Visitor);
        this.commandResults.add(commandResult);

        // display a summary of the results
        displaySummary(workerCallback);
      }
      else // execute only the target command
      {
        // solve the target command without push and pop to view multiple models if sat
        commandResult = solveCommand(targetCommandIndex, solver, cvc5Visitor);
      }

      if (commandResult != null && commandResult.xmlFileName != null)
      {
        lastXmlFile = commandResult.xmlFileName;
      }
    }
    catch (Exception exception)
    {
      StringWriter stringWriter = new StringWriter();
      exception.printStackTrace(new PrintWriter(stringWriter));
      throw new Exception(stringWriter.toString());
    }
  }

  void displaySummary(WorkerEngine.WorkerCallback workerCallback)
  {
    if (this.commandResults.size() > 1)
    {
      callbackBold(commandResults.size() + " commands were executed. The results are:\n");
      for (CommandResult commandResult : this.commandResults)
      {
        callbackPlain("#" + (commandResult.index + 1) + ": ");

        switch (commandResult.result)
        {
          case "sat":
            callbackLink(
                commandResult.command.check ? "Counterexample found. " : "Instance found. ",
                "XML: " + commandResult.xmlFileName);

            callbackPlain(commandResult.command.label + " ");

            callbackPlain(commandResult.command.check ? "is invalid" : "is consistent");

            break;
          case "unsat":

            callbackPlain(commandResult.command.label + " ");

            callbackPlain(commandResult.command.check ? "is valid" : " is unsatisfiable");

            break;
          default: callbackPlain(commandResult.command.label + " is unknown");
        }
        callbackPlain("\n");
      }
    }
  }

  private CommandResult solveCommand(int index, Solver solver, Cvc5Visitor cvc5Visitor)
      throws Exception
  {
    cvc5Visitor.visit(translation.getOptimizedSmtScript(index));
    Command command = translation.getCommands().get(index);

    callbackBold("Executing " + command + "\n");

    if (!CvcIncludeCommandScope.get())
    {
      ErrorWarning warning = new ErrorWarning(command.pos, "The scope is ignored by cvc5");
      callbackWarning(warning);
    }

    final long startSolve = System.currentTimeMillis();

    Result result = solver.checkSat();

    final long endSolve = System.currentTimeMillis();
    long duration = (endSolve - startSolve);
    //        callbackBold("Solving time: " + duration + " ms\n");

    callbackPlain("Satisfiability: " + result + "\n");

    CommandResult commandResult = new CommandResult();
    commandResult.index = index;
    commandResult.command = command;
    commandResult.result = result.toString();

    if (result.isSat())
    {
      commandResult.xmlFileName = prepareInstance(index, duration, solver, cvc5Visitor);
    }
    if (result.isUnsat() && CvcProduceUnsatCores.get())
    {
      commandResult.unsatCore = prepareUnsatCore(index, duration, solver, cvc5Visitor);
    }
    return commandResult;
  }

  private Set<Pos> prepareUnsatCore(
      int commandIndex, long duration, Solver solver, Cvc5Visitor cvc5Visitor) throws Exception
  {
    Term[] coreTerms = solver.getUnsatCore();
    String corString =
        Arrays.asList(coreTerms).stream().map(t -> t.toString()).collect(Collectors.joining("\n"));

    callbackPlain("cvc5 found an ");
    Object[] modelMessage = new Object[] {"link", "SMT unsat core", "MSG: " + corString};
    workerCallback.callback(modelMessage);
    callbackPlain("\n");

    List<String> coreAssertions = cvc5Visitor.getCoreAssertions(coreTerms);
    String alloyCore = coreAssertions.stream().map(s -> "|" + s + "|").collect(Collectors.joining("\n"));
    alloyCore = "(" + alloyCore + ")";
    callbackPlain("cvc5 found an ");
    Object[] anotherCore = new Object[] {"link", "Alloy unsat core", "MSG: " + alloyCore};
    workerCallback.callback(anotherCore);
    callbackPlain("\n");

    SmtUnsatCore smtUnsatCore = parseUnsatCore(alloyCore);
    AlloyUnsatCore alloyUnsatCore = AlloyUnsatCore.fromSmtUnsatCore(smtUnsatCore);
    Set<Pos> positions = alloyUnsatCore.getPositions();

    String coreMessage = getCoreMessage(positions);

    Object[] message = new Object[] {"link", "Core", coreMessage};
    workerCallback.callback(message);
    callbackPlain(" contains " + positions.size() + " top-level formulas. " + duration + "ms.\n");

    return positions;
  }

  private String getCoreMessage(Set<Pos> positions)
  {
    Pair<Set<Pos>, Set<Pos>> core = new Pair<>(positions, Collections.emptySet());

    String coreMessage;
    OutputStream fs = null;
    ObjectOutputStream os = null;
    try
    {
      // generate unsat core file
      File coreFile = File.createTempFile("tmp", ".core", new File(tempDirectory));
      fs = new FileOutputStream(coreFile);
      os = new ObjectOutputStream(fs);
      os.writeObject(core);
      os.writeObject(positions);
      coreMessage = "CORE: " + coreFile.getAbsolutePath();
    }
    catch (Throwable ex)
    {
      coreMessage = "";
    }
    finally
    {
      Util.close(os);
      Util.close(fs);
    }
    return coreMessage;
  }

  private void callbackLink(String log, String link)
  {
    workerCallback.callback(new Object[] {"link", log, link});
  }

  private void callbackPlain(String log)
  {
    workerCallback.callback(new Object[] {"", log});
    workerCallback.callback(new Object[] {"", ""});
  }

  private void callbackBold(String log)
  {
    workerCallback.callback(new Object[] {"S2", "\n"});
    workerCallback.callback(new Object[] {"S2", log});
    workerCallback.callback(new Object[] {"S2", "\n"});
  }

  private void callbackWarning(ErrorWarning warning)
  {
    workerCallback.callback(new Object[] {"warning", warning});
  }

  /**
   * gets a model from cvc5 if the satResult is sat and saves it into a new xml
   * file and return its path
   *
   * @param commandIndex the index of the sat command
   * @param duration the solving duration in milli seconds
   * @return a path to the xml file where the model is saved
   * @throws Exception
   */
  private String prepareInstance(
      int commandIndex, long duration, Solver solver, Cvc5Visitor cvc5Visitor) throws Exception
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
    String smtModel = solver.getModel(sorts.toArray(new Sort[0]), terms);

    callbackPlain("cvc5 found a ");
    Object[] modelMessage = new Object[] {"link", "model", "MSG: " + smtModel};
    workerCallback.callback(modelMessage);
    callbackPlain("\n");

    //        callbackPlain(smtModel + "\n");

    Command command = translation.getCommands().get(commandIndex);

    //        smtModel= showInputDialog(smtModel);

    SmtModel model = parseModel(smtModel);

    File xmlFile = File.createTempFile("tmp", ".smt.xml", new File(tempDirectory));

    String xmlFilePath = xmlFile.getAbsolutePath();

    Instance instance = writeModelToAlloyXmlFile(
        translation, model, xmlFilePath, originalFileName, command, alloyFiles);

    // generate alloy code that restricts the model to be the instance found
    String alloyCode = instance.generateAlloyCode();
    File alloyFile = File.createTempFile("tmp", ".als", new File(tempDirectory));
    Formatter formatter = new Formatter(alloyFile);
    formatter.format("%s", alloyCode);
    formatter.close();

    String satResult = "sat";
    Object[] message =
        new Object[] {satResult, command.check, command.expects, xmlFilePath, null, duration};
    workerCallback.callback(message);

    return xmlFilePath;
  }

  public static Instance writeModelToAlloyXmlFile(Translation translation,
      SmtModel model,
      String xmlFileName,
      String originalFileName,
      Command command,
      Map<String, String> alloyFiles) throws Exception
  {
    Mapper mapper = translation.getMapper();

    Map<String, FunctionDefinition> functionsMap = new HashMap<>();

    for (FunctionDeclaration function : model.getFunctions())
    {
      FunctionDefinition definition = (FunctionDefinition) function;
      String name = function.getName();
      functionsMap.put(name, definition);
    }

    List<Signature> signatures = new ArrayList<>();

    for (MappingSignature mappingSignature : mapper.signatures)
    {
      Signature signature = getSignature(functionsMap, mappingSignature);
      signatures.add(signature);
    }

    List<Field> fields = new ArrayList<>();

    for (MappingField mappingField : mapper.fields)
    {
      Field field = getField(functionsMap, mappingField);
      fields.add(field);
    }

    Instance instance = new Instance();
    instance.signatures = signatures;
    instance.fields = fields;
    instance.bitWidth = command.bitwidth >= 0 ? command.bitwidth : 4;
    instance.maxSeq = command.maxseq >= 0 ? command.maxseq : 4;
    instance.command = command.toString();

    instance.fileName = originalFileName;

    AlloySolution alloySolution = new AlloySolution();
    alloySolution.instances = new ArrayList<>();
    alloySolution.instances.add(instance);
    alloySolution.buildDate = java.time.Instant.now().toString();
    alloySolution.alloyFiles = new ArrayList<>();

    for (Map.Entry<String, String> entry : alloyFiles.entrySet())
    {
      alloySolution.alloyFiles.add(new AlloyFile(entry.getKey(), entry.getValue()));
    }

    alloySolution.writeToXml(xmlFileName);
    return instance;
  }

  private static void addSpecialFields(
      Map<String, FunctionDefinition> functionsMap, List<Field> fields, int parentId)
      throws Exception
  {
    if (functionsMap.containsKey(AbstractTranslator.plus))
    {
      fields.add(getSpecialField(functionsMap, AbstractTranslator.plus, parentId));
    }
    if (functionsMap.containsKey(AbstractTranslator.minus))
    {
      fields.add(getSpecialField(functionsMap, AbstractTranslator.minus, parentId));
    }
    if (functionsMap.containsKey(AbstractTranslator.multiply))
    {
      fields.add(getSpecialField(functionsMap, AbstractTranslator.multiply, parentId));
    }
    if (functionsMap.containsKey(AbstractTranslator.divide))
    {
      fields.add(getSpecialField(functionsMap, AbstractTranslator.divide, parentId));
    }
    if (functionsMap.containsKey(AbstractTranslator.mod))
    {
      fields.add(getSpecialField(functionsMap, AbstractTranslator.mod, parentId));
    }
  }

  private static Signature getSignature(Map<String, FunctionDefinition> functionsMap,
      MappingSignature mappingSignature) throws Exception
  {
    Signature signature = new Signature();

    // get signature info from the mapping
    signature.label = mappingSignature.label;
    signature.id = mappingSignature.id;

    if (mappingSignature.parents.size() == 1)
    {
      signature.parentId = mappingSignature.parents.get(0);
    }

    // add types for subset
    if (mappingSignature.isSubset)
    {
      for (Integer id : mappingSignature.parents)
      {
        signature.types.add(new Type(id));
      }
    }

    signature.builtIn = mappingSignature.builtIn ? "yes" : "no";
    signature.isAbstract = mappingSignature.isAbstract ? "yes" : "no";
    signature.isOne = mappingSignature.isOne ? "yes" : "no";
    signature.isLone = mappingSignature.isLone ? "yes" : "no";
    signature.isSome = mappingSignature.isSome ? "yes" : "no";
    signature.isPrivate = mappingSignature.isPrivate ? "yes" : "no";
    signature.isMeta = mappingSignature.isMeta ? "yes" : "no";
    signature.isExact = mappingSignature.isExact ? "yes" : "no";
    signature.isEnum = mappingSignature.isEnum ? "yes" : "no";

    // the signature Int is treated differently
    if (signature.label.equals("Int"))
    {
      return signature;
    }

    // get the corresponding function from the model
    FunctionDefinition function = functionsMap.get(mappingSignature.functionName);
    if (function == null)
    {
      // throw new Exception("Can not find the function "+ mappingSignature.functionName + " for
      // signature "+ signature.label + "in the model.") ;

      // due to some optimization, some signatures may be lost. So here we assume empty atoms.
      signature.atoms = new ArrayList<>();
    }
    else
    {
      signature.atoms = getAtoms(function.smtExpr, functionsMap);
    }
    return signature;
  }

  private static Field getSpecialField(
      Map<String, FunctionDefinition> functionsMap, String fieldName, int parentId) throws Exception
  {
    Field field = new Field();
    field.label = fieldName;
    field.id = fieldName.hashCode();
    field.parentId = parentId;

    field.isPrivate = "no";
    field.isMeta = "no";

    // get the corresponding function from the model
    FunctionDefinition function = functionsMap.get(fieldName);
    if (function == null)
    {
      throw new Exception(
          "Can not find the function " + fieldName + " for field " + field.label + "in the model.");
    }
    field.tuples = getTuples(function.smtExpr, functionsMap);
    field.types = Collections.singletonList(new Types());
    // ToDo: refactor these magic numbers
    field.types.get(0).types = Arrays.stream(new int[] {parentId, parentId, parentId})
                                   .mapToObj(Type::new)
                                   .collect(Collectors.toList());

    return field;
  }

  private static Field getField(
      Map<String, FunctionDefinition> functionsMap, MappingField mappingField) throws Exception
  {
    Field field = new Field();

    // get field info from the mapping
    field.label = mappingField.label;
    field.id = mappingField.id;
    field.parentId = mappingField.parentId;

    field.isPrivate = mappingField.isPrivate ? "yes" : "no";
    field.isMeta = mappingField.isMeta ? "yes" : "no";

    // get the corresponding function from the model
    FunctionDefinition function = functionsMap.get(mappingField.functionName);
    if (function == null)
    {
      // throw new Exception("Can not find the function "+ mappingField.functionName + " for field
      // "+ field.label + "in the model.") ;

      // due to some optimization, some signatures may be lost. So here we assume empty atoms.
      field.tuples = new ArrayList<>();
    }
    else
    {
      field.tuples = getTuples(function.smtExpr, functionsMap);
    }

    field.types = getTypes(mappingField);

    return field;
  }

  private static List<Types> getTypes(MappingField mappingField)
  {
    List<Types> types = new ArrayList<>();
    for (List<MappingType> list : mappingField.types)
    {
      Types alloyTypes = new Types();
      alloyTypes.types = list.stream().map(t -> new Type(t.id)).collect(Collectors.toList());
      types.add(alloyTypes);
    }
    return types;
  }

  private static List<Tuple> getTuples(
      SmtExpr smtExpr, Map<String, FunctionDefinition> functionsMap)
  {
    List<Tuple> tuples = new ArrayList<>();

    if (smtExpr instanceof SmtUnaryExpr)
    {
      SmtUnaryExpr unary = (SmtUnaryExpr) smtExpr;
      switch (unary.getOp())
      {
        case SET_EMPTY: return new ArrayList<>();
        case SET_SINGLETON:
        {
          SmtExpr unarySmtExpr = unary.getExpr();
          if (unarySmtExpr instanceof SmtMultiArityExpr)
          {
            SmtMultiArityExpr multiArity = (SmtMultiArityExpr) unarySmtExpr;

            if (multiArity.getOp() == SmtMultiArityExpr.Op.TUPLE)
            {
              List<Atom> atoms = getAtoms(multiArity, functionsMap);
              Tuple tuple = new Tuple();
              tuple.atoms = atoms;
              return Collections.singletonList(tuple);
            }

            throw new UnsupportedOperationException();
          }
          throw new UnsupportedOperationException();
        }
        default: throw new UnsupportedOperationException();
      }
    }

    if (smtExpr instanceof SmtBinaryExpr)
    {
      SmtBinaryExpr binary = (SmtBinaryExpr) smtExpr;

      switch (binary.getOp())
      {
        case UNION:
        {
          tuples.addAll(getTuples(binary.getA(), functionsMap));
          tuples.addAll(getTuples(binary.getB(), functionsMap));
          return tuples;
        }
        default: throw new UnsupportedOperationException();
      }
    }

    throw new UnsupportedOperationException();
  }

  private static List<Atom> getAtoms(SmtExpr smtExpr, Map<String, FunctionDefinition> functions)
  {
    List<Atom> atoms = new ArrayList<>();

    if (smtExpr instanceof UninterpretedConstant)
    {
      UninterpretedConstant uninterpretedConstant = (UninterpretedConstant) smtExpr;
      if (uninterpretedConstant.getSort().equals(AbstractTranslator.atomSort))
      {
        atoms.add(new Atom(uninterpretedConstant.getName()));
      }
      if (uninterpretedConstant.getSort().equals(AbstractTranslator.uninterpretedInt))
      {
        IntConstant intConstant = (IntConstant) uninterpretedConstant.evaluate(functions);
        atoms.add(new Atom(intConstant.getValue()));
      }
      return atoms;
    }

    // ToDo: review removing this which is replaced with uninterpretedInt
    if (smtExpr instanceof IntConstant)
    {
      IntConstant intConstant = (IntConstant) smtExpr;
      atoms.add(new Atom(intConstant.getValue()));
      return atoms;
    }

    if (smtExpr instanceof SmtUnaryExpr)
    {
      SmtUnaryExpr unary = (SmtUnaryExpr) smtExpr;
      switch (unary.getOp())
      {
        case SET_EMPTY: return new ArrayList<>();
        case SET_SINGLETON:
        {
          return getAtoms(unary.getExpr(), functions);
        }
        default: throw new UnsupportedOperationException();
      }
    }

    if (smtExpr instanceof SmtBinaryExpr)
    {
      SmtBinaryExpr binary = (SmtBinaryExpr) smtExpr;

      switch (binary.getOp())
      {
        case UNION:
        {
          atoms.addAll(getAtoms(binary.getA(), functions));
          atoms.addAll(getAtoms(binary.getB(), functions));
          return atoms;
        }
        default: throw new UnsupportedOperationException();
      }
    }

    if (smtExpr instanceof SmtMultiArityExpr)
    {
      SmtMultiArityExpr multiArity = (SmtMultiArityExpr) smtExpr;
      switch (multiArity.getOp())
      {
        case TUPLE:
        {
          for (SmtExpr expr : multiArity.getExprs())
          {
            atoms.addAll(getAtoms(expr, functions));
          }

          return atoms;
        }

        default: throw new UnsupportedOperationException();
      }
    }

    throw new UnsupportedOperationException();
  }

  private Translation translateToSMT() throws IOException
  {
    setAlloySettings();

    Translation translation =
        Utils.translate(alloyFiles, originalFileName, resolutionMode, alloySettings);

    // callbackBold("Translation output");
    // callbackPlain(translation.getSmtScript());

    // output the smt file
    generateSmtFile(translation, false);
    generateSmtFile(translation, true);

    // output the mapping
    File jsonFile = File.createTempFile("tmp", ".mapping.json", new File(tempDirectory));
    translation.getMapper().writeToJson(jsonFile.getPath());

    callbackPlain("\nGenerated json mapping file: ");
    Object[] jsonMessage =
        new Object[] {"link", jsonFile.getAbsolutePath(), "CNF: " + jsonFile.getAbsolutePath()};
    workerCallback.callback(jsonMessage);
    callbackPlain("\n");
    return translation;
  }

  private void generateSmtFile(Translation translation, boolean isOptimized) throws IOException
  {
    File smtFile = File.createTempFile("tmp", ".smt2", new File(tempDirectory));
    String allCommands = translation.translateAllCommandsWithCheckSat(isOptimized);
    Formatter formatter = new Formatter(smtFile);
    formatter.format("%s", allCommands);
    formatter.close();
    if (isOptimized)
    {
      callbackPlain("\nGenerated optimized smt2 file: ");
    }
    else
    {
      callbackPlain("\nGenerated smt2 file: ");
    }
    Object[] smtMessage =
        new Object[] {"link", smtFile.getAbsolutePath(), "CNF: " + smtFile.getAbsolutePath()};
    workerCallback.callback(smtMessage);
  }

  public static void setAlloySettings()
  {
    // (set-option :tlimit 30000)
    alloySettings.putSolverOption(SmtSettings.TLIMIT, CvcTimeout.get().toString());
    //(set-option :produce-unsat-cores false)
    alloySettings.putSolverOption(
        SmtSettings.PRODUCE_UNSAT_CORES, CvcProduceUnsatCores.get().toString());
    //(set-option :finite-model-find false)
    alloySettings.putSolverOption(
        SmtSettings.FINITE_MODEL_FIND, CvcFiniteModelFind.get().toString());
    alloySettings.includeCommandScope = CvcIncludeCommandScope.get();
    alloySettings.produceUnsatCore = CvcProduceUnsatCores.get();
    alloySettings.finiteModelFinding = CvcFiniteModelFind.get();
    alloySettings.integerSingletonsOnly = CvcIntegerSingletonsOnly.get();
  }

  // ToDo: replace this with a call edu.uiowa.smt.Result.parseModel
  public static SmtModel parseModel(String model)
  {
    CharStream charStream = CharStreams.fromString(model);

    Cvc5SmtLexer lexer = new Cvc5SmtLexer(charStream);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    Cvc5SmtParser parser = new Cvc5SmtParser(tokenStream);

    ParseTree tree = parser.model();
    Cvc5SmtModelVisitor visitor = new Cvc5SmtModelVisitor();

    SmtModel smtModel = (SmtModel) visitor.visit(tree);

    return smtModel;
  }

  // ToDo: replace this with a call edu.uiowa.smt.Result.parseUnsatCore
  public static SmtUnsatCore parseUnsatCore(String smtCore)
  {
    CharStream charStream = CharStreams.fromString(smtCore);

    Cvc5SmtLexer lexer = new Cvc5SmtLexer(charStream);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    Cvc5SmtParser parser = new Cvc5SmtParser(tokenStream);

    ParseTree tree = parser.getUnsatCore();
    Cvc5SmtModelVisitor visitor = new Cvc5SmtModelVisitor();

    SmtUnsatCore smtUnsatCore = (SmtUnsatCore) visitor.visit(tree);
    return smtUnsatCore;
  }

  public static String showInputDialog(String text)
  {
    JTextArea textArea = new JTextArea(text);
    textArea.setSize(textArea.getPreferredSize().width, textArea.getPreferredSize().height);
    JOptionPane.showConfirmDialog(
        null, new JScrollPane(textArea), "Debugging", JOptionPane.OK_OPTION);
    return textArea.getText();
  }

  private class CommandResult
  {
    public int index;
    public Command command;
    public String result;
    public String xmlFileName;
    public Set<Pos> unsatCore;
  }
}
