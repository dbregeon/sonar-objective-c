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

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.and;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.commentRegexp;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.o2n;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.one2n;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.opt;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.or;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;

import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCKeyword;
import org.sonar.objectivec.api.ObjectiveCPunctuator;
import org.sonar.objectivec.api.ObjectiveCTokenType;
import org.sonar.objectivec.preprocessor.PreprocessorChannel;

import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel;
import com.sonar.sslr.impl.channel.PunctuatorChannel;

public final class ObjectiveCLexer {
    private static final String INTEGER_SUFFIX = "(((U|u)(LL|ll|L|l)?)|((LL|ll|L|l)(u|U)?))";
    private static final String EXP = "([Ee][+-]?+[0-9_]++)";
    private static final String FLOAT_SUFFIX = "(f|l|F|L)";

    private ObjectiveCLexer() {
    }

    public static Lexer create() {
        return create(new ObjectiveCConfiguration());
    }

    public static Lexer create(final Preprocessor... preprocessors) {
        return create(new ObjectiveCConfiguration(), preprocessors);
    }

    public static Lexer create(final ObjectiveCConfiguration conf, final Preprocessor... preprocessors) {
        final Lexer.Builder builder = Lexer.builder()
                .withCharset(conf.getCharset())

                .withFailIfNoChannelToConsumeOneCharacter(false)

                // Comments
                .withChannel(commentRegexp("//[^\\n\\r]*+"))
                .withChannel(commentRegexp("/\\*[\\s\\S]*?\\*/"))

                // Preprocessor directives
                .withChannel(new PreprocessorChannel())

                // String Literals
                .withChannel(new StringLiteralsChannel())

                // ObjectiveC Tokens
                .withChannel(new IdentifierAndKeywordChannel(or(and("[a-zA-Z_]", o2n("\\w")), and("@", one2n("\\w"))), true, ObjectiveCKeyword.values()))
                .withChannel(new PunctuatorChannel(ObjectiveCPunctuator.values()))

                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "[0-9]++\\.[0-9]*+" + opt(EXP) + opt(FLOAT_SUFFIX)))
                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "\\.[0-9]++" + opt(EXP) + opt(FLOAT_SUFFIX)))
                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "[0-9]++" + EXP + opt(FLOAT_SUFFIX)))

                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "[1-9][0-9]*+" + opt(INTEGER_SUFFIX))) // Decimal literals
                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "0[0-7]++" + opt(INTEGER_SUFFIX))) // Octal Literals
                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "0[xX][0-9a-fA-F]++" + opt(INTEGER_SUFFIX))) // Hex Literals
                .withChannel(regexp(ObjectiveCTokenType.NUMERIC_LITERAL, "0" + opt(INTEGER_SUFFIX))) // Decimal zero

                // All other tokens
//                .withChannel(regexp(LITERAL, "[^\r\n\\s/]+"))

                .withChannel(new BlackHoleChannel("[\\s]"));

        for (final Preprocessor preprocessor : preprocessors) {
            builder.withPreprocessor(preprocessor);
        }

        return builder.build();
    }

}
