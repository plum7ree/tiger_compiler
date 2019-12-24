parser grammar TigerParser;

options { tokenVocab=TigerLexer; }

tigerProgram
    : MAIN LET declarationSegment IN BEGIN statSeq END
    ;

declarationSegment
    : typeDeclaration* varDeclaration* functionDeclaration*
    ;

typeDeclaration
    : TYPE ID '=' type ';'
    ;

type
    : typeId
    | ARRAY '[' INTLIT ']' OF typeId
    | ID
    ;

typeId
    : INT
    | FLOAT
    ;

varDeclaration
    : VAR idList ':' type optionalInit? ';'
    ;

idList
    : ID (',' ID)*
    ;

optionalInit
    : ':=' literal
    ;

functionDeclaration
    : FUNC ID '(' paramList? ')' ':' type BEGIN statSeq END ';'
    ;

paramList
    : param (',' param)*
    ;

param
    : ID ':' type
    ;

statSeq
    : stat+
    ;

stat
    : lvalue ':=' expr ';'                              # Assignment
    | ID ':=' fncall ';'                                # FncallAssignment
    | IF expr THEN statSeq (ELSE statSeq)? ENDIF ';'    # IfBlock
    | WHILE expr DO statSeq ENDDO ';'                   # WhileBlock
    | FOR ID ':=' expr TO expr DO statSeq ENDDO ';'     # ForBlock
    | LET declarationSegment IN statSeq END ';'         # LetBlock
    | BREAK ';'                                         # Break
    | RETURN expr ';'                                   # Return
    ;

fncall
    : ID '(' expressionList? ')'
    ;

expressionList
    : expr (',' expr)*
    ;

expr: atom
    | <assoc=right> expr op='**' expr
    | expr op='*' expr
    | expr op='/' expr
    | expr op='+' expr
    | expr op='-' expr
    | expr op='==' expr
    | expr op='!=' expr
    | expr op='>' expr
    | expr op='<' expr
    | expr op='>=' expr
    | expr op='<=' expr
    | expr op='&' expr
    | expr op='|' expr
    ;

atom
    : literal
    | lvalue
    | '(' expr ')'
    ;

literal
    : INTLIT
    | FLOATLIT
    ;

lvalue
    : ID ('[' expr ']')?
    ;
