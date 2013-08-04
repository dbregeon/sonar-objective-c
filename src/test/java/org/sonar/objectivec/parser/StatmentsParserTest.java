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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;

public final class StatmentsParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesEmptyCompoundStatement() {
        givenAParserForStatement();
        final AstNode node = parser().parse("{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesNonEmptyCompoundStatement() {
        givenAParserForStatement();
        final AstNode node = parser().parse("{\ntest = toto;\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesArrayLiteral() {
        givenAParserForStatement();
        final AstNode node = parser().parse("@[@\"test\", @\"test2\"];");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesAssignment() {
        givenAParserForStatement();
        final AstNode node = parser().parse("test = toto;");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }


    @Test
    public void parserHandlesIfStatement() {
        givenAParserForStatement();
        final AstNode node = parser.parse("if (test)\n{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesLocalVariableDeclaration() {
        givenAParserForDeclaration();
        final AstNode node = parser().parse("NSString * test;");
        assertThat(node.getNumberOfChildren(), equalTo(3));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationWithInitialization() {
        givenAParserForDeclaration();
        final AstNode node = parser().parse("NSString * test = toto;\n");
        assertThat(node.getNumberOfChildren(), equalTo(3));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationInABlockStatement() {
        givenAParserForStatement();
        final AstNode node = parser().parse("{\nNSString * test;\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationWithInitializationInABlockStatement() {
        givenAParserForStatement();
        final AstNode node = parser().parse("{\nNSString * test = toto;\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesMethodInvocation() {
        givenAParserForStatement();
        final AstNode node = parser().parse("[self alloc];");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesMethodInvocationWithBlockParameter() {
        givenAParserForStatement();
        final AstNode node = parser().parse("[self alloc:^(BOOL test) {\n}];");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesReturnWithBooleanExpression() {
        givenAParserForStatement();
        final AstNode node = parser().parse("return interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown;");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesForLoop() {
        givenAParserForStatement();
        final AstNode node = parser().parse("for (int i = 0; i < len; i++) {\n}");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesThrow() {
        givenAParserForStatement();
        final AstNode node = parser().parse("@throw [NSException exceptionWithName:NSInternalInconsistencyException\nreason:[NSString stringWithFormat:@\"You must override %@ in a subclass\", NSStringFromSelector(_cmd)]\nuserInfo:nil];");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    private Parser<ObjectiveCGrammar> givenAParserForStatement() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().statement);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForDeclaration() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().declaration);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
