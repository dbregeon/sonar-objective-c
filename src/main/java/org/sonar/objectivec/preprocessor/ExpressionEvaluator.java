/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 François Helg, Cyril Picat and OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.objectivec.preprocessor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.objectivec.ObjectiveCConfiguration;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;

public final class ExpressionEvaluator {
    public static final Logger LOG = LoggerFactory.getLogger("Evaluator");

    private final Parser<CppGrammar> parser;
    private final ObjectiveCPreprocessor preprocessor;

    public ExpressionEvaluator(final ObjectiveCConfiguration conf,
            final ObjectiveCPreprocessor preprocessor) {
        parser = CppParser.createConstantExpressionParser(conf);

        this.preprocessor = preprocessor;
    }

    public boolean eval(final String constExpr) {
        return evalToInt(constExpr) != 0;
    }

    public boolean eval(final AstNode constExpr) {
        return evalToInt(constExpr) != 0;
    }

    private long evalToInt(final String constExpr) {
        AstNode constExprAst = null;
        try {
            constExprAst = parser.parse(constExpr);
        } catch (final com.sonar.sslr.api.RecognitionException re) {
            LOG.warn("Error evaluating expression '{}', assuming 0", constExpr);
            return 0;
        }

        return evalToInt(constExprAst);
    }

    private long evalToInt(final AstNode exprAst) {
        LOG.trace("Evaluating expression: {}", exprAst);

        final int noChildren = exprAst.getNumberOfChildren();
        if (noChildren == 0) {
            return evalLeaf(exprAst);
        } else if (noChildren == 1) {
            return evalOneChildAst(exprAst);
        }

        return evalComplexAst(exprAst);
    }

    private long evalLeaf(final AstNode exprAst) {
        // Evaluation of leafs
        //
        final String nodeType = exprAst.getName();
        if ("NUMBER".equals(nodeType)) {
            return evalNumber(exprAst.getTokenValue());
        } else if ("CHARACTER".equals(nodeType)) {
            return evalCharacter(exprAst.getTokenValue());
        } else if ("IDENTIFIER".equals(nodeType)) {
            final String value = preprocessor.valueOf(exprAst.getTokenValue());
            return value == null ? 0 : evalToInt(value);
        } else {
            throw new EvaluationException("Unknown expression type '"
                    + nodeType + "'");
        }
    }

    private long evalOneChildAst(final AstNode exprAst) {
        // Evaluation of booleans and 'pass-through's
        //
        final String nodeType = exprAst.getName();
        if ("bool".equals(nodeType)) {
            return evalBool(exprAst.getTokenValue());
        }
        return evalToInt(exprAst.getChild(0));
    }

    private long evalComplexAst(final AstNode exprAst) {
        // More complex expressions with more than one child
        //
        final String nodeType = exprAst.getName();
        if ("unaryExpression".equals(nodeType)) {
            return evalUnaryExpression(exprAst);
        } else if ("conditionalExpression".equals(nodeType)) {
            return evalConditionalExpression(exprAst);
        } else if ("logicalOrExpression".equals(nodeType)) {
            return evalLogicalOrExpression(exprAst);
        } else if ("logicalAndExpression".equals(nodeType)) {
            return evalLogicalAndExpression(exprAst);
        } else if ("inclusiveOrExpression".equals(nodeType)) {
            return evalInclusiveOrExpression(exprAst);
        } else if ("exclusiveOrExpression".equals(nodeType)) {
            return evalExclusiveOrExpression(exprAst);
        } else if ("andExpression".equals(nodeType)) {
            return evalAndExpression(exprAst);
        } else if ("equalityExpression".equals(nodeType)) {
            return evalEqualityExpression(exprAst);
        } else if ("relationalExpression".equals(nodeType)) {
            return evalRelationalExpression(exprAst);
        } else if ("shiftExpression".equals(nodeType)) {
            return evalShiftExpression(exprAst);
        } else if ("additiveExpression".equals(nodeType)) {
            return evalAdditiveExpression(exprAst);
        } else if ("multiplicativeExpression".equals(nodeType)) {
            return evalMultiplicativeExpression(exprAst);
        } else if ("primaryExpression".equals(nodeType)) {
            return evalPrimaryExpression(exprAst);
        } else if ("definedExpression".equals(nodeType)) {
            return evalDefinedExpression(exprAst);
        } else if ("functionlikeMacro".equals(nodeType)) {
            return evalFunctionlikeMacro(exprAst);
        } else {
            throw new EvaluationException("Unknown expression type '"
                    + nodeType + "'");
        }
    }

    // ///////////////// Primitives //////////////////////
    long evalBool(final String boolValue) {
        return boolValue.equalsIgnoreCase("true") ? 1 : 0;
    }

    long evalNumber(final String intValue) {
        // the if expressions arent allowed to contain floats
        long number = 0;
        try {
            number = Long.decode(stripSuffix(intValue)).longValue();
        } catch (final java.lang.NumberFormatException nfe) {
            number = Long.MAX_VALUE;
            LOG.warn(
                    "Cannot decode the number '{}' falling back to max long ({}) instead",
                    number);
        }

        return number;
    }

    long evalCharacter(final String charValue) {
        // TODO: replace this simplification by something more sane
        return charValue.equals("'\0'") ? 0 : 1;
    }

    // ////////////// logical expressions ///////////////////////////
    long evalLogicalOrExpression(final AstNode exprAst) {
        final int noChildren = exprAst.getNumberOfChildren();
        boolean result = eval(exprAst.getChild(0));
        for (int i = 2; i < noChildren && result != true; i += 2) {
            final AstNode operand = exprAst.getChild(i);
            result = result || eval(operand);
        }

        return result ? 1 : 0;
    }

    long evalLogicalAndExpression(final AstNode exprAst) {
        final int noChildren = exprAst.getNumberOfChildren();
        boolean result = eval(exprAst.getChild(0));
        for (int i = 2; i < noChildren && result != false; i += 2) {
            final AstNode operand = exprAst.getChild(i);
            result = result && eval(operand);
        }

        return result ? 1 : 0;
    }

    long evalEqualityExpression(final AstNode exprAst) {
        String operator = exprAst.getChild(1).getTokenValue();
        final AstNode lhs = exprAst.getChild(0);
        AstNode rhs = exprAst.getChild(2);
        boolean result;
        if (operator.equals("==")) {
            result = evalToInt(lhs) == evalToInt(rhs);
        } else if (operator.equals("!=")) {
            result = evalToInt(lhs) != evalToInt(rhs);
        } else {
            throw new EvaluationException("Unknown equality operator '"
                    + operator + "'");
        }

        final int noChildren = exprAst.getNumberOfChildren();
        for (int i = 4; i < noChildren; i += 2) {
            operator = exprAst.getChild(i - 1).getTokenValue();
            rhs = exprAst.getChild(i);
            if (operator.equals("==")) {
                result = result == eval(rhs);
            } else if (operator.equals("!=")) {
                result = result != eval(rhs);
            } else {
                throw new EvaluationException("Unknown equality operator '"
                        + operator + "'");
            }
        }

        return result ? 1 : 0;
    }

    long evalRelationalExpression(final AstNode exprAst) {
        String operator = exprAst.getChild(1).getTokenValue();
        final AstNode lhs = exprAst.getChild(0);
        AstNode rhs = exprAst.getChild(2);
        boolean result;
        if (operator.equals("<")) {
            result = evalToInt(lhs) < evalToInt(rhs);
        } else if (operator.equals(">")) {
            result = evalToInt(lhs) > evalToInt(rhs);
        } else if (operator.equals("<=")) {
            result = evalToInt(lhs) <= evalToInt(rhs);
        } else if (operator.equals(">=")) {
            result = evalToInt(lhs) >= evalToInt(rhs);
        } else {
            throw new EvaluationException("Unknown relational operator '"
                    + operator + "'");
        }

        int resultAsInt;
        final int noChildren = exprAst.getNumberOfChildren();
        for (int i = 4; i < noChildren; i += 2) {
            operator = exprAst.getChild(i - 1).getTokenValue();
            rhs = exprAst.getChild(i);

            resultAsInt = result ? 1 : 0;
            if (operator.equals("<")) {
                result = resultAsInt < evalToInt(rhs);
            } else if (operator.equals(">")) {
                result = resultAsInt > evalToInt(rhs);
            } else if (operator.equals("<=")) {
                result = resultAsInt <= evalToInt(rhs);
            } else if (operator.equals(">=")) {
                result = resultAsInt >= evalToInt(rhs);
            } else {
                throw new EvaluationException("Unknown relational operator '"
                        + operator + "'");
            }
        }

        return result ? 1 : 0;
    }

    // ///////////////// bitwise expressions ///////////////////////
    long evalAndExpression(final AstNode exprAst) {
        final int noChildren = exprAst.getNumberOfChildren();
        long result = evalToInt(exprAst.getChild(0));
        for (int i = 2; i < noChildren; i += 2) {
            final AstNode operand = exprAst.getChild(i);
            result &= evalToInt(operand);
        }

        return result;
    }

    long evalInclusiveOrExpression(final AstNode exprAst) {
        final int noChildren = exprAst.getNumberOfChildren();
        long result = evalToInt(exprAst.getChild(0));
        for (int i = 2; i < noChildren; i += 2) {
            final AstNode operand = exprAst.getChild(i);
            result |= evalToInt(operand);
        }

        return result;
    }

    long evalExclusiveOrExpression(final AstNode exprAst) {
        final int noChildren = exprAst.getNumberOfChildren();
        long result = evalToInt(exprAst.getChild(0));
        for (int i = 2; i < noChildren; i += 2) {
            final AstNode operand = exprAst.getChild(i);
            result ^= evalToInt(operand);
        }

        return result;
    }

    // ///////////////// other ... ///////////////////
    long evalUnaryExpression(final AstNode exprAst) {
        // only 'unary-operator cast-expression' production is allowed in
        // #if-context

        final String operator = exprAst.getChild(0).getTokenValue();
        final AstNode operand = exprAst.getChild(1);
        if (operator.equals("+")) {
            return evalToInt(operand);
        } else if (operator.equals("-")) {
            return -evalToInt(operand);
        } else if (operator.equals("!")) {
            final boolean result = !eval(operand);
            return result ? 1 : 0;
        } else if (operator.equals("~")) {
            return ~evalToInt(operand);
        } else {
            throw new EvaluationException("Unknown unary operator  '"
                    + operator + "'");
        }
    }

    long evalShiftExpression(final AstNode exprAst) {
        String operator;
        AstNode rhs;
        long result = evalToInt(exprAst.getChild(0));
        final int noChildren = exprAst.getNumberOfChildren();

        for (int i = 2; i < noChildren; i += 2) {
            operator = exprAst.getChild(i - 1).getTokenValue();
            rhs = exprAst.getChild(i);

            if (operator.equals("<<")) {
                result = result << evalToInt(rhs);
            } else if (operator.equals(">>")) {
                result = result >> evalToInt(rhs);
            } else {
                throw new EvaluationException("Unknown shift operator '"
                        + operator + "'");
            }
        }

        return result;
    }

    long evalAdditiveExpression(final AstNode exprAst) {
        String operator;
        AstNode rhs;
        long result = evalToInt(exprAst.getChild(0));
        final int noChildren = exprAst.getNumberOfChildren();

        for (int i = 2; i < noChildren; i += 2) {
            operator = exprAst.getChild(i - 1).getTokenValue();
            rhs = exprAst.getChild(i);

            if (operator.equals("+")) {
                result += evalToInt(rhs);
            } else if (operator.equals("-")) {
                result -= evalToInt(rhs);
            } else {
                throw new EvaluationException("Unknown additive operator '"
                        + operator + "'");
            }
        }

        return result;
    }

    long evalMultiplicativeExpression(final AstNode exprAst) {
        String operator;
        AstNode rhs;
        long result = evalToInt(exprAst.getChild(0));
        final int noChildren = exprAst.getNumberOfChildren();

        for (int i = 2; i < noChildren; i += 2) {
            operator = exprAst.getChild(i - 1).getTokenValue();
            rhs = exprAst.getChild(i);

            if (operator.equals("*")) {
                result *= evalToInt(rhs);
            } else if (operator.equals("/")) {
                result /= evalToInt(rhs);
            } else if (operator.equals("%")) {
                result %= evalToInt(rhs);
            } else {
                throw new EvaluationException(
                        "Unknown multiplicative operator '" + operator + "'");
            }
        }

        return result;
    }

    long evalConditionalExpression(final AstNode exprAst) {
        final AstNode decisionOperand = exprAst.getChild(0);
        final AstNode trueCaseOperand = exprAst.getChild(2);
        final AstNode falseCaseOperand = exprAst.getChild(4);
        return eval(decisionOperand) ? evalToInt(trueCaseOperand)
                : evalToInt(falseCaseOperand);
    }

    long evalPrimaryExpression(final AstNode exprAst) {
        // case "( expression )"
        return evalToInt(exprAst.getChild(1));
    }

    long evalDefinedExpression(final AstNode exprAst) {
        final int posOfMacroName = exprAst.getNumberOfChildren() == 2 ? 1 : 2;
        final String macroName = exprAst.getChild(posOfMacroName)
                .getTokenValue();
        final String value = preprocessor.valueOf(macroName);

        LOG.trace("expanding '{}' to '{}'", macroName, value);

        return value == null ? 0 : 1;
    }

    long evalFunctionlikeMacro(final AstNode exprAst) {
        final String macroName = exprAst.getChild(0).getTokenValue();
        final List<Token> tokens = exprAst.getTokens();
        final List<Token> restTokens = tokens.subList(1, tokens.size());
        final String value = preprocessor.expandFunctionLikeMacro(macroName,
                restTokens);

        LOG.trace("expanding '{}' to '{}'", macroName, value);

        return value == null ? 0 : evalToInt(value);
    }

    String stripSuffix(final String number) {
        return number.replaceAll("[LlUu]", "");
    }
}
