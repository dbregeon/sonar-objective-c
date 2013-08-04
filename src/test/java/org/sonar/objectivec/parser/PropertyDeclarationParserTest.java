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
import org.sonar.objectivec.api.ObjectiveCKeyword;
import org.sonar.objectivec.api.ObjectiveCPunctuator;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public final class PropertyDeclarationParserTest {
    private Parser<ObjectiveCGrammar> parser;

    @Test
    public void parserHandlesPropertyDeclarationWithNoAttributes() {
        givenAParserForPropertyDeclaration();
        final AstNode node = parser().parse("@property int a;");
        assertThat(node.getNumberOfChildren(), equalTo(2));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCKeyword.AT_PROPERTY));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) parser().getGrammar().structDeclaration));
        assertThat(node.getTokens().size(), equalTo(4));
    }

    @Test
    public void parserHandlesPropertyDeclarationWithAttributes() {
        givenAParserForPropertyDeclaration();
        final AstNode node = parser().parse("@property (readonly) int a;");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCKeyword.AT_PROPERTY));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttributesDeclaration));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().structDeclaration));
        assertThat(node.getTokens().size(), equalTo(7));
    }

    @Test
    public void parserHandlesEmptyPropertyAttributesDeclaration() {
        givenAParserForPropertyAttributesDeclaration();
        final AstNode node = parser().parse("()");
        assertThat(node.getNumberOfChildren(), equalTo(2));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LEFT_PARENTHESIS));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RIGHT_PARENTHESIS));
        assertThat(node.getTokens().size(), equalTo(2));
    }

    @Test
    public void parserHandlesNonEmptyPropertyAttributesDeclaration() {
        givenAParserForPropertyAttributesDeclaration();
        final AstNode node = parser().parse("(readonly)");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.LEFT_PARENTHESIS));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttributes));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.RIGHT_PARENTHESIS));
        assertThat(node.getTokens().size(), equalTo(3));
    }

    @Test
    public void parserHandlesSinglePropertyAttributes() {
        givenAParserForPropertyAttributes();
        final AstNode node = parser().parse("readonly");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttribute));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesSinglePropertyAttributesWithTrailingComma() {
        givenAParserForPropertyAttributes();
        final AstNode node = parser().parse("readonly, ");
        assertThat(node.getNumberOfChildren(), equalTo(2));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttribute));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getTokens().size(), equalTo(2));
    }

    @Test
    public void parserHandlesMultiplePropertyAttributes() {
        givenAParserForPropertyAttributes();
        final AstNode node = parser().parse("readonly, weak");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttribute));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttributes));
        assertThat(node.getTokens().size(), equalTo(3));
    }

    @Test
    public void parserHandlesMultiplePropertyAttributesWithTrailingComma() {
        givenAParserForPropertyAttributes();
        final AstNode node = parser().parse("readonly, weak, ");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttribute));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.COMMA));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) parser().getGrammar().propertyAttributes));
        assertThat(node.getTokens().size(), equalTo(4));
    }

    @Test
    public void parserHandlesWeakPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("weak");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesStrongPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("strong");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesReadwritePropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("readwrite");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesReadonlyPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("readonly");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesAssignPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("assign");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesRetainPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("retain");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesCopyPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("copy");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesAtomicPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("atomic");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesNonatomicPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("nonatomic");
        assertThat(node.getNumberOfChildren(), equalTo(1));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(1));
    }

    @Test
    public void parserHandlesGetterPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("getter = getTest");
        assertThat(node.getNumberOfChildren(), equalTo(3));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.ASSIGN));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(3));
    }

    @Test
    public void parserHandlesSetterPropertyAttribute() {
        givenAParserForPropertyAttribute();
        final AstNode node = parser().parse("setter = setTest:");
        assertThat(node.getNumberOfChildren(), equalTo(4));
        assertThat(node.getChild(0).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getChild(1).getType(), equalTo((AstNodeType) ObjectiveCPunctuator.ASSIGN));
        assertThat(node.getChild(2).getType(), equalTo((AstNodeType) GenericTokenType.IDENTIFIER));
        assertThat(node.getTokens().size(), equalTo(4));
    }

    @Test(expected=RecognitionException.class)
    public void parserRejectsPropertyAttributesWithUnknownProperty() {
        givenAParserForPropertyAttributes();
        parser().parse("test");
    }

    @Test(expected=RecognitionException.class)
    public void parserRejectsPropertyAttributeWithUnknownProperty() {
        givenAParserForPropertyAttribute();
        parser().parse("test");
    }

    private Parser<ObjectiveCGrammar> givenAParserForPropertyAttribute() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().propertyAttribute);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForPropertyAttributes() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().propertyAttributes);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForPropertyAttributesDeclaration() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().propertyAttributesDeclaration);
        return parser;
    }

    private Parser<ObjectiveCGrammar> givenAParserForPropertyDeclaration() {
        parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().propertyDeclaration);
        return parser;
    }

    private Parser<ObjectiveCGrammar> parser() {
        return parser;
    }

}
