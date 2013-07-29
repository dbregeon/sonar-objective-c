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
package org.sonar.objectivec.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum ObjectiveCPunctuator implements TokenType {

    // these are really only c operators
    // http://en.wikipedia.org/wiki/Operators_in_C_and_C%2B%2B

    PLUSPLUS("++"),
    PLUSEQ("+="),
    PLUS("+"),

    MINUSMINUS("--"),
    MINUSEQ("-="),
    MINUS("-"),

    STAREQ("*="),
    STAR("*"),

    SLASHEQ("/="),
    SLASH("/"),

    // Comparison/relational operators
    EQ("=="),
    NOT_EQ("!="),
    LT("<"),
    GT(">"),
    LT_EQ("<="),
    GT_EQ(">="),


    LTLT("<<"),
    LTLTEQ("<<="),

    GTGT(">>"),
    GTGTEQ(">>="),

    ASSIGN("="),

    TILDE("~"),

    AMP("&"),
    AMPEQ("&="),
    AMPAMPEX("&&="),

    BAR("|"),
    BAREQ("|="),
    BARBAREQ("||="),

    CARETEQ("^="),
    CARET("^"),

    PERCENT("%"),
    PERCENTEQ("%="),

    LCURLYBRACE("{"),
    RCURLYBRACE("}"),
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    LBRACKET("["),
    RBRACKET("]"),

    QUESTION("?"),
    COLON(":"),
    SEMICOLON(";"),
    COMMA(","),

    MINUSLT("->"),
    MINUSLTSTAR("->*"),
    DOTSTAR(".*"),

    AND("&&"),
    OR("||"),
    NOT("!"),

    DOUBLE_QUOTES("\""),
    AT("@"),
    DOT("."),
    TRIPLE_DOT("...");

    private final String value;

    private ObjectiveCPunctuator(final String word) {
        value = word;
    }

    public String getName() {
        return name();
    }

    public String getValue() {
        return value;
    }

    public boolean hasToBeSkippedFromAst(final AstNode node) {
        return false;
    }

}
