lexer grammar TigerLexer;

MAIN:   'main';
ARRAY:  'array';
RECORD: 'record';
BREAK:  'break';
DO:     'do';
ELSE:   'else';
FOR:    'for';
FUNC:   'function';
IF:     'if';
IN:     'in';
LET:    'let';
OF:     'of';
THEN:   'then';
TO:     'to';
TYPE:   'type';
VAR:    'var';
WHILE:  'while';
ENDIF:  'endif';
BEGIN:  'begin';
END:    'end';
ENDDO:  'enddo';
RETURN: 'return';

COMMA:  ',';
COLON:  ':';
SEMI:   ';';
LPAREN: '(';
RPAREN: ')';
LBRACK: '[';
RBRACK: ']';
LBRACE: '{';
RBRACE: '}';
PERIOD: '.';

POW:        '**';
PLUS:       '+';
MINUS:      '-';
MULT:       '*';
DIV:        '/';
EQ:         '==';
NEQ:        '!=';
LESSER:     '<';
GREATER:    '>';
LESSEREQ:   '<=';
GREATEREQ:  '>=';
AND:        '&';
OR:         '|';
ASSIGN:     ':=';
DEF:        '=';

INT:    'int';
FLOAT:  'float';

ID : Letter LetterOrDigit*;
INTLIT : '0' | [1-9] Digits?;
FLOATLIT : Digits '.' Digits?;

COMMENT: '/*' .*? '*/' -> skip;
WS : [ \t\r\n]+ -> skip;

fragment Digits
    : [0-9]+
    ;

fragment LetterOrDigit
    : Letter | [0-9_]
    ;

fragment Letter
    : [a-zA-Z]
    ;
