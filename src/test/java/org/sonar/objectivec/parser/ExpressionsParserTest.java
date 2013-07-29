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

public final class ExpressionsParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesNestedMethodCalls() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("[[UIBarButtonItem alloc] initWithTitle:@\"Back\"]");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesMethodCallsWithParameters() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("[button initWithTitle:@\"Back\"]");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesMethodCalls() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("[UIBarButtonItem alloc]");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesPerformSelector() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("[destinationController performSelector:@selector(setCompanyDetails:) withObject:selectedSponsor]");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationWithArrayLiteralAssigment() {
        givenAParserForExpressions();

        final AstNode node = parser().parse("NSArray * array = @[@\"test\", @\"test2\"]");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationWithDictionaryLiteralAssigment() {
        givenAParserForExpressions();

        final AstNode node = parser().parse("NSDictionary * dict = @{@\"test\" : @\"test2\"}");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesLocalVariableDeclarationWithStringAssignment() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("NSString * test = @\"toto\"");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesStringAssignment() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("test = @\"toto\"");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parserHandlesIdentifierAssignment() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("test = toto");
        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parsingHandlesBlockInstances() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("^(BOOL granted, NSError *error) {\n}\n");

        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parsingHandlesNotEqualComparison() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown");

        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parsingHandlesAssignmentOfDictionaryLiteral() {
        givenAParserForAssignmentExpressions();
        final AstNode node = parser().parse("sectionHeaderViews = @{@\"Bronze\" : _bronzeSectionHeader, @\"Silver\" : _silverSectionHeader, @\"Gold\" :_goldSectionHeader , @\"Platinum\" : _platinumSectionHeader, @\"Press\" : _pressSectionHeader}");

        assertThat(node.getNumberOfChildren(), equalTo(3));
    }

    @Test
    public void parsingHandlesIncrementOfNestedProperty() {
        givenAParserForExpressions();
        final AstNode node = parser().parse("self.pageControl.currentPage += 1");

        assertThat(node.getNumberOfChildren(), equalTo(1));
    }

    @Test
    public void parsingHandlesDictionaryLiteralWithSubscriptAccess() {
        givenAParserForDictionaryLiterals();
        parser().setRootRule(parser().getGrammar().dictionaryLiteral);
        final AstNode node = parser().parse("@{@\"screen_name\" : exhibitor[@\"twitter-id\"], @\"follow\" : @\"true\" }");

        assertThat(node.getNumberOfChildren(), equalTo(6));
    }

    private Parser<ObjectiveCGrammar> givenAParserForExpressions() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().expression);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForAssignmentExpressions() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().assignmentExpression);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForDictionaryLiterals() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().dictionaryLiteral);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
