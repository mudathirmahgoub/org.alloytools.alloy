grammar Smt;

// parser rules

// cvc5 prints a model as a list of definitions, CVC4 prefixed it with 'model'
model : '(' 'model'? sortDeclaration* functionDefinition* ')' ;

sortDeclaration :  '(' 'declare-sort' sortName arity ')' ;

functionDefinition : '(' 'define-fun' functionName '(' argument* ')' sort
                        expression ')' ;

argument : '(' argumentName sort ')' ;

sort :  sortName | '(' tupleSort ')' | '(' setSort ')' ;

setSort : 'Set' sort ;

tupleSort : 'Tuple' sort+ ;

sortName : Identifier ;

arity : Integer ;

functionName : Identifier ;

argumentName : Identifier ;

expression :    constant
                | variable
                | unaryExpression
                | binaryExpression
                | ternaryExpression
                | multiArityExpression
                | '(' expression ')';


unaryExpression : UnaryOperator expression ;

binaryExpression : BinaryOperator expression expression ;

ternaryExpression : TernaryOperator expression expression expression ;

multiArityExpression :  MultiArityOperator expression+ ;

variable : Identifier;

constant :  boolConstant
            | integerConstant
            | uninterpretedConstant
            | emptySet ;

boolConstant : True | False;

integerConstant : '-' Integer | Integer ;

// cvc5 prints uninterpreted values as (as @Atom_0 Atom)
uninterpretedConstant : (AtomPrefix | UninterpretedIntPrefix) Integer
                        | 'as' (AtomPrefix | UninterpretedIntPrefix) Integer sortName ;

emptySet : 'as' 'set.empty' '(' 'Set' sort ')' ;

getValue : '(' ('(' expression expression ')' )+ ')';

// lexer rules

True: 'true' ;

False: 'false' ;

UnaryOperator : 'not' | 'set.singleton' | 'set.complement' | 'rel.transpose' | 'rel.tclosure' ;

BinaryOperator : '=' | '>' | '>=' | '<' | '<='
                | '+' | '-' | '*' | '/' | 'mod'
                | 'or' | 'and' | '=>'
                | 'set.union' | 'set.inter' | 'set.minus' | 'set.member' | 'set.subset'
                | 'rel.join' | 'rel.product' ;

TernaryOperator : 'ite' ;

MultiArityOperator : 'tuple' | 'set.insert' | 'distinct' ;

AtomPrefix : '@Atom_';

UninterpretedIntPrefix : '@UninterpretedInt_' ;

Identifier : IdentifierLetter (IdentifierLetter | Digit)* | ('|' .*? '|');

IdentifierLetter : 'a'..'z'|'A'..'Z'|'_'|'/' | '\'' | '"' | '$';

Integer : Digit+ ;

Digit : '0'..'9' ;

Comment :  ';' ~( '\r' | '\n' )* -> skip ;

Whitespace :  [ \t\r\n]+ -> skip ;