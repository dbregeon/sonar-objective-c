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

public final class MethodDefinitionsParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesInstanceMethodWithNoParameter() {
        givenAParserForInstanceMethods();
        parser().setRootRule(parser().getGrammar().methodDeclaration);
        final AstNode node = parser().parse("- (NSString *) test;");
        assertThat(node.getNumberOfChildren(), equalTo(4));
    }

    @Test
    public void parserHandlesInstanceMethodWithBlockParameter() {
        givenAParserForInstanceMethods();
        parser().setRootRule(parser().getGrammar().methodDeclaration);
        final AstNode node = parser().parse("- (void) onSuccess:(void (^)(NSData *newData, NSHTTPURLResponse *urlResponse)) handler;");
        assertThat(node.getNumberOfChildren(), equalTo(4));
    }


    @Test
    public void parserHandlesEmptyInstanceMethodWithNoParameter() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test\n{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesEmptyInstanceMethodWithOneParameter() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test:(double) param\n{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesEmptyInstanceMethodWithOnePointerParameter() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test:(NSString *) param\n{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesEmptyInstanceMethodWithAnonymousFunctionParameter() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (void) dispatch:(void(^)()) block\n{\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesMethodDefinitionWithAssigment() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test:(double) param {\n test = toto;\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesMethodDefinitionWithLocalVariableDeclaration() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test:(double) param {\n NSString * test;\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesMethodDefinitionWithLocalVariableDeclarationAndArrayLiteralAssigment() {
        givenAParserForInstanceMethods();
        final AstNode node = parser().parse("- (NSString *) test:(double) param {\n NSArray * array = @[@\"test\", @\"test2\"];\n}\n");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    private Parser<ObjectiveCGrammar> givenAParserForInstanceMethods() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().instanceMethodDefinition);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
