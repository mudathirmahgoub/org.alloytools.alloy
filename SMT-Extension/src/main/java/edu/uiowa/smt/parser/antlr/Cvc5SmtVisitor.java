// Generated from Cvc5Smt.g4 by ANTLR 4.7.2
package edu.uiowa.smt.parser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link Cvc5SmtParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface Cvc5SmtVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#model}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel(Cvc5SmtParser.ModelContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#sortDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortDeclaration(Cvc5SmtParser.SortDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(Cvc5SmtParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#smtVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmtVariable(Cvc5SmtParser.SmtVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#sort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort(Cvc5SmtParser.SortContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#setSort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetSort(Cvc5SmtParser.SetSortContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#tupleSort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleSort(Cvc5SmtParser.TupleSortContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#sortName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortName(Cvc5SmtParser.SortNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#arity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArity(Cvc5SmtParser.ArityContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(Cvc5SmtParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#variableName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableName(Cvc5SmtParser.VariableNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(Cvc5SmtParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(Cvc5SmtParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#binaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryExpression(Cvc5SmtParser.BinaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#ternaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpression(Cvc5SmtParser.TernaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#multiArityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiArityExpression(Cvc5SmtParser.MultiArityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#quantifiedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuantifiedExpression(Cvc5SmtParser.QuantifiedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#functionCallExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallExpression(Cvc5SmtParser.FunctionCallExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(Cvc5SmtParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(Cvc5SmtParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#boolConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolConstant(Cvc5SmtParser.BoolConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#integerConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerConstant(Cvc5SmtParser.IntegerConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#uninterpretedConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUninterpretedConstant(Cvc5SmtParser.UninterpretedConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#emptySet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptySet(Cvc5SmtParser.EmptySetContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#getValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetValue(Cvc5SmtParser.GetValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cvc5SmtParser#getUnsatCore}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetUnsatCore(Cvc5SmtParser.GetUnsatCoreContext ctx);
}