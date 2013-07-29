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

import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.lexer.ObjectiveCLexer;
import org.sonar.objectivec.preprocessor.ObjectiveCPreprocessor;
import org.sonar.squid.api.SourceProject;

import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.events.ExtendedStackTrace;
import com.sonar.sslr.impl.events.ParsingEventListener;
import com.sonar.sslr.squid.SquidAstVisitorContext;
import com.sonar.sslr.squid.SquidAstVisitorContextImpl;

public class ObjectiveCParser {
    private static class ObjectiveCParseEventPropagator extends ParsingEventListener {
        private final ObjectiveCPreprocessor objectivecpp;
        private final SquidAstVisitorContext<ObjectiveCGrammar> astVisitorContext;

        ObjectiveCParseEventPropagator(final ObjectiveCPreprocessor pp, final SquidAstVisitorContext<ObjectiveCGrammar> astVisitorContext) {
          objectivecpp = pp;
          this.astVisitorContext = astVisitorContext;
        }

        @Override
        public void beginLex() {
            objectivecpp.beginPreprocessing(astVisitorContext.getFile());
        }
     }

    private static ObjectiveCParseEventPropagator parseEventPropagator;

    private ObjectiveCParser() {
    }

    public static Parser<ObjectiveCGrammar> create(final ObjectiveCConfiguration conf) {
        final SquidAstVisitorContextImpl<ObjectiveCGrammar> context = new SquidAstVisitorContextImpl<ObjectiveCGrammar>(new SourceProject("ObjectiveC Project"));
        return create(context, conf);
      }

    public static Parser<ObjectiveCGrammar> create(final SquidAstVisitorContext<ObjectiveCGrammar> context, final ObjectiveCConfiguration conf) {
        final ObjectiveCPreprocessor objectivecpp = new ObjectiveCPreprocessor(context, conf);
        parseEventPropagator = new ObjectiveCParseEventPropagator(objectivecpp, context);
        return Parser.builder((ObjectiveCGrammar) new ObjectiveCGrammarImpl())
            .withLexer(ObjectiveCLexer.create(conf, objectivecpp))
            .setParsingEventListeners(parseEventPropagator).build();
      }

      public static Parser<ObjectiveCGrammar> createDebugParser(final SquidAstVisitorContext<ObjectiveCGrammar> context,
          final ExtendedStackTrace stackTrace) {
        final ObjectiveCConfiguration conf = new ObjectiveCConfiguration();
        final ObjectiveCPreprocessor objectivecpp = new ObjectiveCPreprocessor(context, conf);
        parseEventPropagator = new ObjectiveCParseEventPropagator(objectivecpp, context);
        return Parser.builder((ObjectiveCGrammar) new ObjectiveCGrammarImpl())
            .withLexer(ObjectiveCLexer.create(conf, objectivecpp))
            .setParsingEventListeners(parseEventPropagator)
            .setExtendedStackTrace(stackTrace)
            .build();
      }
}
