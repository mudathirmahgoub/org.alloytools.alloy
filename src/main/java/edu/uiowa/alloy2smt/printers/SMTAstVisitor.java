/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.printers;

import edu.uiowa.alloy2smt.smtAst.*;

public interface SMTAstVisitor 
{
    public void visit(BinaryExpression bExpr);

    public void visit(Sort intSort);
    
    public void visit(IntSort intSort);   

    public void visit(QuantifiedExpression aThis);

    public void visit(RealSort aThis);

    public void visit(SetSort aThis);

    public void visit(StringSort aThis);

    public void visit(TupleSort aThis);

    public void visit(UnaryExpression aThis);

    public void visit(UninterpretedSort aThis);

    public void visit(IntConstant aThis);

    public void visit(ConstantExpression aThis);

    public void visit(FunctionDeclaration aThis);

    public void visit(FunctionDefinition aThis);

    public void visit(ConstantDeclaration aThis);

    public void visit(BooleanConstant aThis);

    public void visit(Assertion assertion);

    public void visit(MultiArityExpression expression);

    public void visit(FunctionCallExpression functionCallExpression);

    public void visit(BoundVariableDeclaration boundVariableDeclaration);

    public void visit(BoolSort aThis);

    public void visit(LetExpression aThis);

    public void visit(ITEExpression aThis);
}
