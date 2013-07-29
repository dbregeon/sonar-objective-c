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

import org.sonar.objectivec.ObjectiveCConfiguration;

import com.sonar.sslr.impl.Parser;

public final class CppParser {
    private CppParser() {
    }

    public static Parser<CppGrammar> create(final ObjectiveCConfiguration conf) {
        return Parser.builder(new CppGrammar())
                .withLexer(CppLexer.create(conf)).build();
    }

    public static Parser<CppGrammar> createConstantExpressionParser(
            final ObjectiveCConfiguration conf) {
        final CppGrammar grammar = new CppGrammar();
        final Parser<CppGrammar> parser = Parser.builder(grammar)
                .withLexer(CppLexer.create(conf)).build();
        parser.setRootRule(grammar.constantExpression);
        return parser;
    }

    public static Parser<CppGrammar> createExpandedIncludeBodyParser(
            final ObjectiveCConfiguration conf) {
        final CppGrammar grammar = new CppGrammar();
        final Parser<CppGrammar> parser = Parser.builder(grammar)
                .withLexer(CppLexer.create(conf)).build();
        parser.setRootRule(grammar.expandedIncludeBody);
        return parser;
    }
}
