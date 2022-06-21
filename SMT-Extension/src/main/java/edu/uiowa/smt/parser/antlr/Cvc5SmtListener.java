// Generated from Cvc5Smt.g4 by ANTLR 4.7.2
package edu.uiowa.smt.parser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Cvc5SmtParser}.
 */
public interface Cvc5SmtListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#model}.
	 * @param ctx the parse tree
	 */
	void enterModel(Cvc5SmtParser.ModelContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#model}.
	 * @param ctx the parse tree
	 */
	void exitModel(Cvc5SmtParser.ModelContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#sortDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterSortDeclaration(Cvc5SmtParser.SortDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#sortDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitSortDeclaration(Cvc5SmtParser.SortDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(Cvc5SmtParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(Cvc5SmtParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#smtVariable}.
	 * @param ctx the parse tree
	 */
	void enterSmtVariable(Cvc5SmtParser.SmtVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#smtVariable}.
	 * @param ctx the parse tree
	 */
	void exitSmtVariable(Cvc5SmtParser.SmtVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#sort}.
	 * @param ctx the parse tree
	 */
	void enterSort(Cvc5SmtParser.SortContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#sort}.
	 * @param ctx the parse tree
	 */
	void exitSort(Cvc5SmtParser.SortContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#setSort}.
	 * @param ctx the parse tree
	 */
	void enterSetSort(Cvc5SmtParser.SetSortContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#setSort}.
	 * @param ctx the parse tree
	 */
	void exitSetSort(Cvc5SmtParser.SetSortContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#tupleSort}.
	 * @param ctx the parse tree
	 */
	void enterTupleSort(Cvc5SmtParser.TupleSortContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#tupleSort}.
	 * @param ctx the parse tree
	 */
	void exitTupleSort(Cvc5SmtParser.TupleSortContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#sortName}.
	 * @param ctx the parse tree
	 */
	void enterSortName(Cvc5SmtParser.SortNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#sortName}.
	 * @param ctx the parse tree
	 */
	void exitSortName(Cvc5SmtParser.SortNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#arity}.
	 * @param ctx the parse tree
	 */
	void enterArity(Cvc5SmtParser.ArityContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#arity}.
	 * @param ctx the parse tree
	 */
	void exitArity(Cvc5SmtParser.ArityContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(Cvc5SmtParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(Cvc5SmtParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#variableName}.
	 * @param ctx the parse tree
	 */
	void enterVariableName(Cvc5SmtParser.VariableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#variableName}.
	 * @param ctx the parse tree
	 */
	void exitVariableName(Cvc5SmtParser.VariableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(Cvc5SmtParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(Cvc5SmtParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(Cvc5SmtParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(Cvc5SmtParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#binaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpression(Cvc5SmtParser.BinaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#binaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpression(Cvc5SmtParser.BinaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpression(Cvc5SmtParser.TernaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpression(Cvc5SmtParser.TernaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#multiArityExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiArityExpression(Cvc5SmtParser.MultiArityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#multiArityExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiArityExpression(Cvc5SmtParser.MultiArityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#quantifiedExpression}.
	 * @param ctx the parse tree
	 */
	void enterQuantifiedExpression(Cvc5SmtParser.QuantifiedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#quantifiedExpression}.
	 * @param ctx the parse tree
	 */
	void exitQuantifiedExpression(Cvc5SmtParser.QuantifiedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#functionCallExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpression(Cvc5SmtParser.FunctionCallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#functionCallExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpression(Cvc5SmtParser.FunctionCallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(Cvc5SmtParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(Cvc5SmtParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(Cvc5SmtParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(Cvc5SmtParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#boolConstant}.
	 * @param ctx the parse tree
	 */
	void enterBoolConstant(Cvc5SmtParser.BoolConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#boolConstant}.
	 * @param ctx the parse tree
	 */
	void exitBoolConstant(Cvc5SmtParser.BoolConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#integerConstant}.
	 * @param ctx the parse tree
	 */
	void enterIntegerConstant(Cvc5SmtParser.IntegerConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#integerConstant}.
	 * @param ctx the parse tree
	 */
	void exitIntegerConstant(Cvc5SmtParser.IntegerConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#uninterpretedConstant}.
	 * @param ctx the parse tree
	 */
	void enterUninterpretedConstant(Cvc5SmtParser.UninterpretedConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#uninterpretedConstant}.
	 * @param ctx the parse tree
	 */
	void exitUninterpretedConstant(Cvc5SmtParser.UninterpretedConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#emptySet}.
	 * @param ctx the parse tree
	 */
	void enterEmptySet(Cvc5SmtParser.EmptySetContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#emptySet}.
	 * @param ctx the parse tree
	 */
	void exitEmptySet(Cvc5SmtParser.EmptySetContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#getValue}.
	 * @param ctx the parse tree
	 */
	void enterGetValue(Cvc5SmtParser.GetValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#getValue}.
	 * @param ctx the parse tree
	 */
	void exitGetValue(Cvc5SmtParser.GetValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link Cvc5SmtParser#getUnsatCore}.
	 * @param ctx the parse tree
	 */
	void enterGetUnsatCore(Cvc5SmtParser.GetUnsatCoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link Cvc5SmtParser#getUnsatCore}.
	 * @param ctx the parse tree
	 */
	void exitGetUnsatCore(Cvc5SmtParser.GetUnsatCoreContext ctx);
}