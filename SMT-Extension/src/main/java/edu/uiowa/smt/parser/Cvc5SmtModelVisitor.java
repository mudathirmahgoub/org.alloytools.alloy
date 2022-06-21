/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.smt.parser;

import edu.uiowa.smt.AbstractTranslator;
import edu.uiowa.smt.SmtEnv;
import edu.uiowa.smt.parser.antlr.Cvc5SmtBaseVisitor;
import edu.uiowa.smt.parser.antlr.Cvc5SmtParser;
import edu.uiowa.smt.smtAst.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Cvc5SmtModelVisitor extends Cvc5SmtBaseVisitor<SmtAst>
{
  private SmtEnv root = new SmtEnv();

  private static final Map<String, String> cvc5ToCvc4 = new HashMap<String, String>() {
    {
      put("as set.empty", "as emptyset");
      put("set.singleton", "singleton");
      put("set.complement", "complement");
      put("rel.transpose", "transpose");
      put("set.union", "union");
      put("set.inter", "intersection");
      put("set.insert", "insert");
      put("tuple", "mkTuple");
      put("=", "=");
      put("ite", "ite");
    };
  };

  @Override
  public SmtAst visitModel(Cvc5SmtParser.ModelContext ctx)
  {
    SmtModel model = new SmtModel();

    for (Cvc5SmtParser.SortDeclarationContext context : ctx.sortDeclaration())
    {
      model.addSort((SmtSort) this.visitSortDeclaration(context));
    }

    for (Cvc5SmtParser.FunctionDefinitionContext context : ctx.functionDefinition())
    {
      // ignore named formulas
      if (context.getText().contains("\"filename\":"))
      {
        // ToDo: support functions of named formulas
        continue;
      }
      FunctionDefinition definition = (FunctionDefinition) this.visitFunctionDefinition(context);
      model.addFunction(definition);
      if (definition.getInputVariables().size() == 0)
      {
        root.put(definition.getName(), definition.getVariable());
      }
      // ToDo: handle functions and lambda expressions
    }

    return model;
  }

  @Override
  public SmtAst visitSortDeclaration(Cvc5SmtParser.SortDeclarationContext ctx)
  {
    String sortName = ctx.sortName().getText();
    int arity = Integer.parseInt(ctx.arity().getText());
    SmtSort sort = new SmtSort(sortName, arity);
    return sort;
  }

  @Override
  public SmtAst visitSort(Cvc5SmtParser.SortContext ctx)
  {
    if (ctx.sortName() != null)
    {
      switch (ctx.sortName().getText())
      {
        case AbstractTranslator.atom: return AbstractTranslator.atomSort;
        case AbstractTranslator.intSortName: return AbstractTranslator.intSort;
        case AbstractTranslator.uninterpretedIntName: return AbstractTranslator.uninterpretedInt;
        case AbstractTranslator.boolSortName: return AbstractTranslator.boolSort;
        default:
          throw new UnsupportedOperationException(
              String.format("Unknown sort '%s'", ctx.sortName().getText()));
      }
    }

    if (ctx.tupleSort() != null)
    {
      return this.visitTupleSort(ctx.tupleSort());
    }

    if (ctx.setSort() != null)
    {
      return this.visitSetSort(ctx.setSort());
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public SmtAst visitTupleSort(Cvc5SmtParser.TupleSortContext ctx)
  {
    List<SmtSort> sorts = new ArrayList<>(ctx.sort().size());

    for (Cvc5SmtParser.SortContext sortContext : ctx.sort())
    {
      SmtSort sort = (SmtSort) this.visitSort(sortContext);
      sorts.add(sort);
    }

    return new TupleSort(sorts);
  }

  @Override
  public SmtAst visitSetSort(Cvc5SmtParser.SetSortContext ctx)
  {
    SmtSort elementSort = (SmtSort) this.visitSort(ctx.sort());
    return new SetSort(elementSort);
  }

  @Override
  public SmtAst visitFunctionDefinition(Cvc5SmtParser.FunctionDefinitionContext ctx)
  {
    String name = ctx.functionName().getText();
    name = processName(name);
    List<SmtVariable> smtVariables =
        ctx.smtVariable()
            .stream()
            .map(argument -> (SmtVariable) this.visitSmtVariable(argument))
            .collect(Collectors.toList());
    Map<String, SmtExpr> arguments =
        smtVariables.stream().collect(Collectors.toMap(v -> v.getName(), v -> v.getVariable()));
    SmtEnv smtEnv = new SmtEnv(root);
    smtEnv.putAll(arguments);
    SmtSort returnSort = (SmtSort) visitSort(ctx.sort());

    SmtExpr smtExpr = (SmtExpr) this.visitExpression(ctx.expression(), smtEnv);

    FunctionDefinition definition =
        new FunctionDefinition(name, smtVariables, returnSort, smtExpr, true);

    return definition;
  }

  private String processName(String name)
  {
    return name.replaceAll("\\|", "").trim();
  }

  @Override
  public SmtAst visitSmtVariable(Cvc5SmtParser.SmtVariableContext ctx)
  {
    String name = processName(ctx.variableName().getText());
    SmtSort sort = (SmtSort) this.visitSort(ctx.sort());
    return new SmtVariable(name, sort, true);
  }

  public SmtAst visitExpression(Cvc5SmtParser.ExpressionContext ctx, SmtEnv smtEnv)
  {
    if (ctx.constant() != null)
    {
      return this.visitConstant(ctx.constant());
    }
    if (ctx.variable() != null)
    {
      return this.visitVariable(ctx.variable(), smtEnv);
    }
    if (ctx.unaryExpression() != null)
    {
      return this.visitUnaryExpression(ctx.unaryExpression(), smtEnv);
    }
    if (ctx.binaryExpression() != null)
    {
      return this.visitBinaryExpression(ctx.binaryExpression(), smtEnv);
    }
    if (ctx.ternaryExpression() != null)
    {
      return this.visitTernaryExpression(ctx.ternaryExpression(), smtEnv);
    }
    if (ctx.multiArityExpression() != null)
    {
      return this.visitMultiArityExpression(ctx.multiArityExpression(), smtEnv);
    }
    if (ctx.quantifiedExpression() != null)
    {
      return this.visitQuantifiedExpression(ctx.quantifiedExpression(), smtEnv);
    }
    if (ctx.functionCallExpression() != null)
    {
      return this.visitFunctionCallExpression(ctx.functionCallExpression(), smtEnv);
    }
    if (ctx.expression() != null)
    {
      return this.visitExpression(ctx.expression(), smtEnv);
    }
    throw new UnsupportedOperationException();
  }

  public SmtAst visitUnaryExpression(Cvc5SmtParser.UnaryExpressionContext ctx, SmtEnv smtEnv)
  {
    SmtExpr smtExpr = (SmtExpr) this.visitExpression(ctx.expression(), smtEnv);
    SmtUnaryExpr.Op operator = SmtUnaryExpr.Op.getOp(cvc5ToCvc4.get(ctx.UnaryOperator().getText()));
    return operator.make(smtExpr);
  }

  public SmtAst visitBinaryExpression(Cvc5SmtParser.BinaryExpressionContext ctx, SmtEnv smtEnv)
  {
    SmtExpr left = (SmtExpr) this.visitExpression(ctx.expression(0), smtEnv);
    SmtExpr right = (SmtExpr) this.visitExpression(ctx.expression(1), smtEnv);

    SmtBinaryExpr.Op operator = SmtBinaryExpr.Op.getOp(cvc5ToCvc4.get(ctx.BinaryOperator().getText()));
    return operator.make(left, right);
  }

  public SmtAst visitTernaryExpression(Cvc5SmtParser.TernaryExpressionContext ctx, SmtEnv smtEnv)
  {
    List<SmtExpr> smtExprs =
        ctx.expression()
            .stream()
            .map(expression -> (SmtExpr) this.visitExpression(expression, smtEnv))
            .collect(Collectors.toList());

    return new SmtIteExpr(smtExprs.get(0), smtExprs.get(1), smtExprs.get(2));
  }

  public SmtAst visitMultiArityExpression(
      Cvc5SmtParser.MultiArityExpressionContext ctx, SmtEnv smtEnv)
  {
    List<SmtExpr> smtExprs =
        ctx.expression()
            .stream()
            .map(expression -> (SmtExpr) this.visitExpression(expression, smtEnv))
            .collect(Collectors.toList());
    String opText = ctx.MultiArityOperator().getText();
    SmtMultiArityExpr.Op operator = SmtMultiArityExpr.Op.getOp(cvc5ToCvc4.get(opText));

    return operator.make(smtExprs);
  }

  public SmtAst visitQuantifiedExpression(
      Cvc5SmtParser.QuantifiedExpressionContext ctx, SmtEnv smtEnv)
  {
    List<SmtVariable> smtVariables =
        ctx.smtVariable()
            .stream()
            .map(argument -> (SmtVariable) this.visitSmtVariable(argument))
            .collect(Collectors.toList());
    Map<String, SmtExpr> variables =
        smtVariables.stream().collect(Collectors.toMap(v -> v.getName(), v -> v.getVariable()));
    SmtEnv newSmtEnv = new SmtEnv(smtEnv);
    newSmtEnv.putAll(variables);
    SmtExpr smtExpr = (SmtExpr) this.visitExpression(ctx.expression(), newSmtEnv);

    SmtQtExpr.Op operator = SmtQtExpr.Op.getOp(cvc5ToCvc4.get(ctx.Quantifier().getText()));
    return operator.make(smtExpr, smtVariables);
  }

  public SmtAst visitFunctionCallExpression(
      Cvc5SmtParser.FunctionCallExpressionContext ctx, SmtEnv smtEnv)
  {
    List<SmtExpr> smtExprs =
        ctx.expression()
            .stream()
            .map(expression -> (SmtExpr) this.visitExpression(expression, smtEnv))
            .collect(Collectors.toList());
    Variable function = (Variable) smtEnv.get(processName(ctx.Identifier().getText()));
    SmtExpr call = new SmtCallExpr((FunctionDeclaration) function.getDeclaration(), smtExprs);
    return call;
  }

  @Override
  public SmtAst visitConstant(Cvc5SmtParser.ConstantContext ctx)
  {
    if (ctx.boolConstant() != null)
    {
      return this.visitBoolConstant(ctx.boolConstant());
    }
    if (ctx.integerConstant() != null)
    {
      return this.visitIntegerConstant(ctx.integerConstant());
    }
    if (ctx.uninterpretedConstant() != null)
    {
      return this.visitUninterpretedConstant(ctx.uninterpretedConstant());
    }
    if (ctx.emptySet() != null)
    {
      return this.visitEmptySet(ctx.emptySet());
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public SmtAst visitBoolConstant(Cvc5SmtParser.BoolConstantContext ctx)
  {
    if (ctx.True() != null)
    {
      return BoolConstant.True;
    }
    else
    {
      return BoolConstant.False;
    }
  }

  @Override
  public SmtAst visitIntegerConstant(Cvc5SmtParser.IntegerConstantContext ctx)
  {
    int constant = Integer.parseInt(ctx.getText());
    return IntConstant.getInstance(constant);
  }

  @Override
  public SmtAst visitUninterpretedConstant(Cvc5SmtParser.UninterpretedConstantContext ctx)
  {
    if (ctx.Identifier().getText().equals("Atom"))
    {
      return new UninterpretedConstant(ctx.getText(), AbstractTranslator.atomSort);
    }
    if (ctx.Identifier().getText().equals("UInt"))
    {
      return new UninterpretedConstant(ctx.getText(), AbstractTranslator.uninterpretedInt);
    }
    throw new UnsupportedOperationException(
        String.format("Unknown constant value '%s'", ctx.getText()));
  }

  @Override
  public SmtAst visitEmptySet(Cvc5SmtParser.EmptySetContext ctx)
  {
    SmtSort elementSort = (SmtSort) this.visitSort(ctx.sort());
    SmtSort setSort = new SetSort(elementSort);
    return SmtUnaryExpr.Op.EMPTYSET.make(setSort);
  }

  public SmtAst visitVariable(Cvc5SmtParser.VariableContext ctx, SmtEnv smtEnv)
  {
    String variableName = processName(ctx.getText());
    if (!smtEnv.containsKey(variableName))
    {
      throw new RuntimeException(String.format("The variable '%s' is undefined", variableName));
    }
    SmtExpr variable = smtEnv.get(variableName);
    return variable;
  }

  @Override
  public SmtAst visitGetValue(Cvc5SmtParser.GetValueContext ctx)
  {
    List<ExpressionValue> values = new ArrayList<>();

    for (int i = 0; i < ctx.expression().size(); i = i + 2)
    {
      SmtExpr smtExpr = (SmtExpr) visitExpression(ctx.expression(i), root);
      SmtExpr value = (SmtExpr) visitExpression(ctx.expression(i + 1), root);
      ExpressionValue expressionValue = new ExpressionValue(smtExpr, value);
      values.add(expressionValue);
    }

    return new SmtValues(values);
  }

  @Override
  public SmtAst visitGetUnsatCore(Cvc5SmtParser.GetUnsatCoreContext ctx)
  {
    List<String> core =
        ctx.Identifier().stream().map(i -> processName(i.getText())).collect(Collectors.toList());

    return new SmtUnsatCore(core);
  }

  @Override
  public SmtAst visitExpression(Cvc5SmtParser.ExpressionContext ctx)
  {
    throw new UnsupportedOperationException(
        "Use the overloaded method visitExpression(Cvc5SmtParser.ExpressionContext ctx, Map<String, Variable> arguments)");
  }
}