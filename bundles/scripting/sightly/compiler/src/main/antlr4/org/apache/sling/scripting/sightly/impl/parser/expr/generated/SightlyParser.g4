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
parser grammar SightlyParser;

options {
    language = Java;
    tokenVocab = SightlyLexer;
}

@header {
import org.apache.sling.scripting.sightly.compiler.expression.nodes.*;
import org.apache.sling.scripting.sightly.compiler.expression.*;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
}


interpolation returns [Interpolation interp]
@init { $interp = new Interpolation(); }
    :    (
        textFrag { $interp.addText($textFrag.str); }
        | expression { $interp.addExpression($expression.expr.withRawText($expression.text)); }
        )* //perhaphs too restrictive
    ;

textFrag returns [String str]
@init { StringBuilder sb = new StringBuilder(); }
    :   (TEXT_PART { sb.append($TEXT_PART.text); })+
        { $str = sb.toString(); }
    | (ESC_EXPR { sb.append($ESC_EXPR.text); })+
        { $str = sb.toString().substring(1); }
    ;

expression returns [Expression expr]
@init { ExpressionNode exNode = NullLiteral.INSTANCE; Map<String, ExpressionNode> opts = Collections.emptyMap(); }
    :   EXPR_START (exprNode {exNode = $exprNode.node;})?
        (OPTION_SEP optionList {opts = $optionList.options;})?
        EXPR_END
        { $expr = new Expression(exNode, opts); }
    ;


optionList returns [Map<String, ExpressionNode> options]
@init { $options = new HashMap<String, ExpressionNode>(); }
    :    f=option { $options.put($f.name, ($f.value != null) ? $f.value : NullLiteral.INSTANCE); }
        (COMMA r=option { $options.put($r.name, $r.value); })*
    ;

option returns [String name, ExpressionNode value]
    :    ID { $name = $ID.text; } (ASSIGN exprNode { $value = $exprNode.node; } )?
    ;


exprNode returns [ExpressionNode node]
    :   condition=binaryOp TERNARY_Q_OP thenBranch=binaryOp TERNARY_BRANCHES_OP elseBranch=binaryOp
        {$node = new TernaryOperator($condition.node, $thenBranch.node, $elseBranch.node);}
    |   binaryOp {$node = $binaryOp.node;}
    ;

binaryOp returns [ExpressionNode node] //is there any priority precedence between AND & OR ?
    :   left=comparisonTerm { $node = $left.node; }
        (operator right=comparisonTerm { $node = new BinaryOperation($operator.op, $node, $right.node); })*
    ;
    
operator returns [BinaryOperator op]
    :    AND_OP { $op = BinaryOperator.AND; } | OR_OP { $op = BinaryOperator.OR; }
    ;

comparisonTerm returns [ExpressionNode node]
    :   factor { $node = $factor.node; }
    |   left=factor comparisonOp right=factor { $node = new BinaryOperation($comparisonOp.op, $left.node, $right.node); }
    ;

comparisonOp returns [BinaryOperator op]
    : GT { $op = BinaryOperator.GT; }
    | LT { $op = BinaryOperator.LT; }
    | LEQ { $op = BinaryOperator.LEQ; }
    | GEQ { $op = BinaryOperator.GEQ; }
    | EQ { $op = BinaryOperator.STRICT_EQ; }
    | NEQ { $op = BinaryOperator.STRICT_NEQ; }
    ;

factor returns [ExpressionNode node]
    :    (pa=term { $node = $pa.node; })
    |     (NOT_OP notTerm=term { $node = new UnaryOperation(UnaryOperator.NOT, $notTerm.node); })
    ;

term returns [ExpressionNode node]
    : simple { $node = $simple.node; }
        ( ARRAY_START exprNode ARRAY_END { $node = new PropertyAccess($node, $exprNode.node); }
        | DOT field { $node = new PropertyAccess($node, $field.node); })*
    ;

field returns [ExpressionNode node]
    : ID { $node = new StringConstant($ID.getText()); }
    ;

simple returns [ExpressionNode node]
    :    atom { $node = $atom.node; }
    |    LBRACKET exprNode RBRACKET { $node = $exprNode.node; }
    |   ARRAY_START valueList ARRAY_END { $node = new ArrayLiteral($valueList.values); }
    |   ARRAY_START ARRAY_END { $node = new ArrayLiteral(Collections.<ExpressionNode>emptyList()); }
    ;

valueList returns [List<ExpressionNode> values]
@init { $values = new ArrayList<ExpressionNode>(); }
    :    (f=exprNode { $values.add($f.node); }) (COMMA p=exprNode { $values.add($p.node); })*
    ;

atom returns [Atom node]
    :    stringConst { $node = $stringConst.node; }
    |    ID { $node = new Identifier($ID.text); }
    |    numText=(FLOAT | INT) { $node = new NumericConstant($numText.text); }
    |    boolText=BOOL_CONSTANT { $node = new BooleanConstant($boolText.text); }
    ;

stringConst returns [StringConstant node]
    :   STRING { $node = ParserHelper.createStringConstant($STRING.text); }
    ;
