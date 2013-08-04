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
import org.sonar.objectivec.api.ObjectiveCTokenType;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public final class NSNumberLiteralParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesSimpleLiteral() {
        givenAParserForNSNumberLiteral();
        final AstNode node = parser().parse("@1");
        assertThat(node.getNumberOfChildren(), equalTo(2));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(node.getTokens().size(), equalTo(2));
    }

    @Test
    public void parserHandlesParanthesizedNumericLiteral() {
        givenAParserForNSNumberLiteral();
        final AstNode node = parser().parse("@(1)");
        assertThat(node.getNumberOfChildren(), equalTo(4));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LEFT_PARENTHESIS));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(node.getChild(3).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RIGHT_PARENTHESIS));
        assertThat(node.getTokens().size(), equalTo(4));
    }

    @Test(expected=RecognitionException.class)
    public void parserRejectsNSNumberLiteralWithOnlyOpeningParenthesis() {
        givenAParserForNSNumberLiteral();
        parser().parse("@(1");
    }

    public void parserDoesNotParseClosingParenthesisWhenNoOpeningParanthesisWasFound() {
        givenAParserForNSNumberLiteral();
        final AstNode node = parser().parse("@1)");
        assertThat(node.getNumberOfChildren(), equalTo(2));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.AT));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(node.getTokens().size(), equalTo(2));
    }

    @Test(expected=RecognitionException.class)
    public void parserRejectsNSNumberLiteralWithNonNumericLiteral() {
        givenAParserForNSNumberLiteral();
        parser().parse("@a0");
    }

    private Parser<ObjectiveCGrammar> givenAParserForNSNumberLiteral() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().nsNumberLiteral);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
