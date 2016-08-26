/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
lexer grammar SightlyLexer;

ESC_EXPR: '\\${'.*? '}';

EXPR_START: '${' -> pushMode(ExpressionMode);

TEXT_PART: .; //$hello ${expr}


mode ExpressionMode;

EXPR_END: '}' -> popMode;


BOOL_CONSTANT: 'true' | 'false';

DOT: '.';

LBRACKET: '(';

RBRACKET: ')';

AND_OP: '&&';

OR_OP: '||';

NOT_OP: '!';

COMMA: ',';

ARRAY_START: '[';

ARRAY_END: ']';

ASSIGN: '=';

OPTION_SEP: '@';

TERNARY_Q_OP: '?';

TERNARY_BRANCHES_OP: ':';

LT: '<';

LEQ: '<=';

GEQ: '>=';

GT: '>';

EQ: '==';

NEQ: '!=';

// tokens

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|':')*
    ;

INT :	'0'..'9'+
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')+ EXPONENT?
//    |   '.' ('0'..'9')+ EXPONENT?  --> conflicts with a.2 notation
    |   ('0'..'9')+ EXPONENT
    ;

COMMENT: '<!--/*' .*? '*/-->' -> skip;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) -> channel(HIDDEN)
    ;


STRING
    :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
    |  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
    ;

//CHAR:  '\'' ( ESC_SEQ | ~('\''|'\\') ) '\''
//    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
