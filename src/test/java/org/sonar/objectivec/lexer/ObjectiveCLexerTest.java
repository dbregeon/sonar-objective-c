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
package org.sonar.objectivec.lexer;

import static com.sonar.sslr.test.lexer.LexerMatchers.hasComment;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasToken;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.objectivec.api.ObjectiveCKeyword;
import org.sonar.objectivec.api.ObjectiveCPunctuator;
import org.sonar.objectivec.api.ObjectiveCTokenType;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;

public final class ObjectiveCLexerTest {

    private static Lexer lexer;

    @BeforeClass
    public static void init() {
        lexer = ObjectiveCLexer.create();
    }

    @Test
    public void lexMultiLinesComment() {
        assertThat(lexer.lex("/* My Comment \n*/"), hasComment("/* My Comment \n*/"));
        assertThat(lexer.lex("/**/"), hasComment("/**/"));
    }

    @Test
    public void lexInlineComment() {
        assertThat(lexer.lex("// My Comment \n new line"), hasComment("// My Comment "));
        assertThat(lexer.lex("//"), hasComment("//"));
    }

    @Test
    public void lexEndOflineComment() {
        assertThat(lexer.lex("[self init]; // My Comment end of line"), hasComment("// My Comment end of line"));
        assertThat(lexer.lex("[self init]; //"), hasComment("//"));
    }

    @Test
    public void lexMethodInvocation() {
        final List<Token> tokens = lexer.lex("[self init];");
        assertThat(tokens.size(), equalTo(6));
        assertThat(tokens, hasToken("[", ObjectiveCPunctuator.LBRACKET));
        assertThat(tokens, hasToken("self", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("init", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("]", ObjectiveCPunctuator.RBRACKET));
        assertThat(tokens, hasToken(";", ObjectiveCPunctuator.SEMICOLON));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexArrayLiteral() {
        final List<Token> tokens = lexer.lex("@[1, 2]");
        assertThat(tokens.size(), equalTo(7));
        assertThat(tokens, hasToken("@", ObjectiveCPunctuator.AT));
        assertThat(tokens, hasToken("[", ObjectiveCPunctuator.LBRACKET));
        assertThat(tokens, hasToken("1", ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(tokens, hasToken(",", ObjectiveCPunctuator.COMMA));
        assertThat(tokens, hasToken("2", ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(tokens, hasToken("]", ObjectiveCPunctuator.RBRACKET));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexIfStatement() {
        final List<Token> tokens = lexer.lex("if(test) {toto = test}");
        assertThat(tokens.size(), equalTo(10));
        assertThat(tokens, hasToken("if", ObjectiveCKeyword.IF));
        assertThat(tokens, hasToken("(", ObjectiveCPunctuator.LEFT_PARENTHESIS));
        assertThat(tokens, hasToken("test", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken(")", ObjectiveCPunctuator.RIGHT_PARENTHESIS));
        assertThat(tokens, hasToken("{", ObjectiveCPunctuator.LCURLYBRACE));
        assertThat(tokens, hasToken("toto", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("=", ObjectiveCPunctuator.ASSIGN));
        assertThat(tokens, hasToken("test", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("}", ObjectiveCPunctuator.RCURLYBRACE));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexEmptyLine() {
        final List<Token> tokens = lexer.lex("\n");
        assertThat(tokens.size(), equalTo(1));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexStringLiteral() {
        final List<Token> tokens = lexer.lex("@\"test\"");
        assertThat(tokens.size(), equalTo(2));
        assertThat(tokens, hasToken("@\"test\"", ObjectiveCTokenType.STRING_LITERAL));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexNumericLiteral() {
        final List<Token> tokens = lexer.lex(".1f");
        assertThat(tokens.size(), equalTo(2));
        assertThat(tokens, hasToken(".1f", ObjectiveCTokenType.NUMERIC_LITERAL));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexLocalVariableAssignment() {
        final List<Token> tokens = lexer.lex("NSString * test = toto;");
        assertThat(tokens.size(), equalTo(7));
        assertThat(tokens, hasToken("NSString", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("*", ObjectiveCPunctuator.STAR));
        assertThat(tokens, hasToken("test", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("=", ObjectiveCPunctuator.ASSIGN));
        assertThat(tokens, hasToken("toto", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken(";", ObjectiveCPunctuator.SEMICOLON));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }

    @Test
    public void lexNestedMethodInvocation() {
        final List<Token> tokens = lexer.lex("[[UIBarButtonItem alloc] initWithTitle:@\"Back\"]");
        assertThat(tokens.size(), equalTo(10));
        assertThat(tokens, hasToken("[", ObjectiveCPunctuator.LBRACKET));
        assertThat(tokens, hasToken("[", ObjectiveCPunctuator.LBRACKET));
        assertThat(tokens, hasToken("UIBarButtonItem", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("alloc", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken("]", ObjectiveCPunctuator.RBRACKET));
        assertThat(tokens, hasToken("initWithTitle", GenericTokenType.IDENTIFIER));
        assertThat(tokens, hasToken(":", ObjectiveCPunctuator.COLON));
        assertThat(tokens, hasToken("@\"Back\"", ObjectiveCTokenType.STRING_LITERAL));
        assertThat(tokens, hasToken("]", ObjectiveCPunctuator.RBRACKET));
        assertThat(tokens, hasToken(GenericTokenType.EOF));
    }


}
