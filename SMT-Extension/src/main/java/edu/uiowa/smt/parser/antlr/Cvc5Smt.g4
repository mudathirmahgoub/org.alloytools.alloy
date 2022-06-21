grammar Cvc5Smt;

// parser rules

model : '(' sortDeclaration* functionDefinition* ')' ;

sortDeclaration :  '(' 'declare-sort' sortName arity ')' ;

functionDefinition : '(' 'define-fun' functionName '(' smtVariable* ')' sort
                        expression ')' ;

smtVariable : '(' variableName sort ')' ;

sort :  sortName | '(' tupleSort ')' | '(' setSort ')' ;

setSort : 'Set' sort ;

tupleSort : 'Tuple' sort+ ;

sortName : Identifier ;

arity : Integer ;

functionName : Identifier ;

variableName : Identifier ;

expression :    constant
                | variable
                | unaryExpression
                | binaryExpression
                | ternaryExpression
                | multiArityExpression
                | quantifiedExpression
                | functionCallExpression
                | '(' expression ')';


unaryExpression : UnaryOperator expression ;

binaryExpression : BinaryOperator expression expression ;

ternaryExpression : TernaryOperator expression expression expression ;

multiArityExpression :  MultiArityOperator expression+ ;

quantifiedExpression : Quantifier '(' smtVariable+ ')' '(' expression ')' ;

functionCallExpression : Identifier expression+ ;

variable : Identifier;

constant :  boolConstant
            | integerConstant
            | uninterpretedConstant
            | emptySet ;

boolConstant : True | False;

integerConstant : '-' Integer | Integer ;

uninterpretedConstant : '(' 'as' UninterpretedPrefix Integer Identifier ')';

emptySet : 'as' 'set.empty' '(' 'Set' sort ')' ;

getValue : '(' ('(' expression expression ')' )+ ')';

getUnsatCore : '(' Identifier* ')';

// lexer rules

True : 'true' ;

False : 'false' ;

Quantifier : 'forall' | 'exists' ;

UnaryOperator : 'not' | 'set.singleton' | 'set.complement' | 'rel.transpose' | 'rel.tclosure' ;

BinaryOperator : '=' | '>' | '>=' | '<' | '<='
                | '+' | '-' | '*' | '/' | 'mod'
                | '=>'
                | 'set.union' | 'set.inter' | 'set.minus' | 'set.member' | 'set.subset'
                | 'rel.join' | 'rel.product' ;

TernaryOperator : 'ite' ;

MultiArityOperator : 'tuple' | 'set.insert' | 'distinct' | 'or' | 'and' ;

UninterpretedPrefix : '@a';

Identifier : IdentifierLetter (IdentifierLetter | Digit)* | ('|' .*? '|');

IdentifierLetter : 'a'..'z'|'A'..'Z'|'_'|'/' | '\'' | '"' | '$' | '.' | '-';

Integer : Digit+ ;

Digit : '0'..'9' ;

Comment :  ';' ~( '\r' | '\n' )* -> skip ;

Whitespace :  [ \t\r\n]+ -> skip ;