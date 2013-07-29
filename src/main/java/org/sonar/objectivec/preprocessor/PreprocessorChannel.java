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

import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;
import org.sonar.objectivec.api.ObjectiveCTokenType;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;

public final class PreprocessorChannel extends Channel<Lexer> {
    private static final char EOF = (char) -1;

    @Override
    public boolean consume(final CodeReader code, final Lexer output) {
        final int line = code.getLinePosition();
        final int column = code.getColumnPosition();

        final char ch = code.charAt(0);
        if ((ch != '#')) {
            return false;
        }

        final String tokenValue = read(code);
        output.addToken(Token.builder().setLine(line).setColumn(column)
                .setURI(output.getURI()).setValueAndOriginalValue(tokenValue)
                .setType(ObjectiveCTokenType.PREPROCESSOR).build());

        return true;
    }

    private String read(final CodeReader code) {
        final StringBuilder sb = new StringBuilder();
        char ch;

        while (true) {
            ch = (char) code.pop();
            if (isNewline(ch) || ch == EOF) {
                break;
            }
            if (ch == '\\' && isNewline((char) code.peek())) {
                // the newline is escaped: we have a the multi line preprocessor
                // directive
                // consume both the backslash and the newline, insert a space
                // instead
                consumeNewline(code);
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void consumeNewline(final CodeReader code) {
        if ((code.charAt(0) == '\r') && (code.charAt(1) == '\n')) {
            // \r\n
            code.pop();
            code.pop();
        } else {
            // \r or \n
            code.pop();
        }
    }

    private static boolean isNewline(final char ch) {
        return (ch == '\n') || (ch == '\r');
    }
}
