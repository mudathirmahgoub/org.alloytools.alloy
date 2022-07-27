package edu.uiowa.smt;

import edu.uiowa.smt.parser.Cvc5SmtModelVisitor;
import edu.uiowa.smt.parser.antlr.Cvc5SmtLexer;
import edu.uiowa.smt.parser.antlr.Cvc5SmtParser;
import edu.uiowa.smt.smtAst.SmtModel;
import edu.uiowa.smt.smtAst.SmtUnsatCore;
import edu.uiowa.smt.smtAst.SmtValues;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class SmtResult
{
  public String smtProgram;
  public String satResult;
  public String model;
  public SmtModel smtModel;
  private Cvc5SmtModelVisitor visitor;

  public SmtResult()
  {
  }

  public SmtResult(String smtProgram, String satResult)
  {
    this.smtProgram = smtProgram;
    this.satResult = satResult;
  }

  public SmtModel parseModel(String model)
  {
    Cvc5SmtParser parser = getCvc5SmtParser(model);

    ParseTree tree = parser.model();
    visitor = new Cvc5SmtModelVisitor();

    SmtModel smtModel = (SmtModel) visitor.visit(tree);

    return smtModel;
  }

  public SmtValues parseValues(String values)
  {
    if (this.visitor == null)
    {
      throw new RuntimeException("Result.parseValues method should only be called after Result.parseModel is called");
    }
    Cvc5SmtParser parser = getCvc5SmtParser(values);
    ParseTree tree = parser.getValue();
    SmtValues smtValues = (SmtValues) this.visitor.visit(tree);
    return smtValues;
  }

  public SmtUnsatCore parseUnsatCore(String core)
  {
    Cvc5SmtParser parser = getCvc5SmtParser(core);
    ParseTree tree = parser.getUnsatCore();
    Cvc5SmtModelVisitor visitor = new Cvc5SmtModelVisitor();
    SmtUnsatCore smtUnsatCore = (SmtUnsatCore) visitor.visit(tree);
    return smtUnsatCore;
  }

  private Cvc5SmtParser getCvc5SmtParser(String values)
  {
    CharStream charStream = CharStreams.fromString(values);
    Cvc5SmtLexer lexer = new Cvc5SmtLexer(charStream);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    return new Cvc5SmtParser(tokenStream);
  }
}
