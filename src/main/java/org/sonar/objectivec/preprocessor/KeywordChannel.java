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

import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;

public final class KeywordChannel extends Channel<Lexer> {

    private final Map<String, TokenType> keywordsMap;
    private final StringBuilder tmpBuilder = new StringBuilder();
    private final Matcher matcher;
    private final Token.Builder tokenBuilder = Token.builder();

    public KeywordChannel(final String regexp, final TokenType[]... keywordSets) {
        final ImmutableMap.Builder<String, TokenType> keywordsMapBuilder = ImmutableMap
                .builder();
        for (final TokenType[] keywords : keywordSets) {
            for (final TokenType keyword : keywords) {
                keywordsMapBuilder.put(keyword.getValue(), keyword);
            }
        }
        this.keywordsMap = keywordsMapBuilder.build();
        matcher = Pattern.compile(regexp).matcher("");
    }

    @Override
    public boolean consume(final CodeReader code, final Lexer lexer) {
        if (code.popTo(matcher, tmpBuilder) > 0) {
            final String word = tmpBuilder.toString();
            tmpBuilder.delete(0, tmpBuilder.length());

            // do this work to strip potential whitespace between the hash and
            // the directive
            final String identifier = word.substring(1, word.length()).trim();
            final String potentialKeyword = CppPunctuator.HASH.getValue()
                    + identifier;

            final TokenType keywordType = keywordsMap.get(potentialKeyword);
            if (keywordType != null) {
                final Token token = tokenBuilder.setType(keywordType)
                        .setValueAndOriginalValue(potentialKeyword)
                        .setURI(lexer.getURI())
                        .setLine(code.getPreviousCursor().getLine())
                        .setColumn(code.getPreviousCursor().getColumn())
                        .build();

                lexer.addToken(token);
            } else {
                // if its not a keyword, then it is a sequence of a hash
                // followed by an identifier
                lexer.addToken(tokenBuilder
                        .setType(CppPunctuator.HASH)
                        .setValueAndOriginalValue(CppPunctuator.HASH.getValue())
                        .setURI(lexer.getURI())
                        .setLine(code.getPreviousCursor().getLine())
                        .setColumn(code.getPreviousCursor().getColumn())
                        .build());
                lexer.addToken(tokenBuilder.setType(IDENTIFIER)
                        .setValueAndOriginalValue(identifier)
                        .setURI(lexer.getURI())
                        .setLine(code.getPreviousCursor().getLine())
                        .setColumn(code.getPreviousCursor().getColumn())
                        .build());
            }

            return true;
        }
        return false;
    }
}
