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
package org.sonar.objectivec.parser;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.api.ObjectiveCPunctuator;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public final class ArrayLiteralParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesEmptyArrayLiteral() {
        givenAParserForArrayLiterals();
        final AstNode node = parser().parse("@[]");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LBRACKET));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RBRACKET));
        assertThat(node.getTokens().size(), equalTo(3));
    }

    @Test
    public void parserHandlesSingleElementLiteral() {
        givenAParserForArrayLiterals();
        final AstNode node = parser().parse("@[@1]");
        assertThat(node.getNumberOfChildren(), equalTo(4));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LBRACKET));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(3).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RBRACKET));
        assertThat(node.getTokens().size(), equalTo(5));
    }

    @Test
    public void parserHandlesCommaSeparatedElementLiteral() {
        givenAParserForArrayLiterals();
        final AstNode node = parser().parse("@[@1, @2]");
        assertThat(node.getNumberOfChildren(), equalTo(6));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LBRACKET));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(3).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(4).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(5).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RBRACKET));
        assertThat(node.getTokens().size(), equalTo(8));
    }

    @Test
    public void parserHandlesTrailingCommaWithMultipleElements() {
        givenAParserForArrayLiterals();
        final AstNode node = parser().parse("@[@1, @2,]");
        assertThat(node.getNumberOfChildren(), equalTo(7));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LBRACKET));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(3).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(4).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(5).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(6).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RBRACKET));
        assertThat(node.getTokens().size(), equalTo(9));
    }

    @Test
    public void parserHandlesTrailingCommaWithSingleElement() {
        givenAParserForArrayLiterals();
        final AstNode node = parser().parse("@[@1,]");
        assertThat(node.getNumberOfChildren(), equalTo(5));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LBRACKET));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().postfixExpression));
        assertThat(node.getChild(3).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(4).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RBRACKET));
        assertThat(node.getTokens().size(), equalTo(6));
    }

    @Test(expected=RecognitionException.class)
    public void parserRejectsArrayLiteralWithOnlyComma() {
        givenAParserForArrayLiterals();
        parser().parse("@[,]");
    }

    private Parser<ObjectiveCGrammar> givenAParserForArrayLiterals() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().arrayLiteral);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
