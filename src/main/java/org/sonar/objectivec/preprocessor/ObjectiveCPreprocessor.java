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

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.objectivec.api.ObjectiveCTokenType.NUMERIC_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.PREPROCESSOR;
import static org.sonar.objectivec.api.ObjectiveCTokenType.WS;
import static org.sonar.objectivec.preprocessor.CppKeyword.IFDEF;
import static org.sonar.objectivec.preprocessor.CppKeyword.IFNDEF;
import static org.sonar.objectivec.preprocessor.CppKeyword.INCLUDE;
import static org.sonar.objectivec.preprocessor.CppPunctuator.LT;
import static org.sonar.objectivec.preprocessor.CppTokenType.STRING;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.lexer.ObjectiveCLexer;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.api.PreprocessorAction;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.squid.SquidAstVisitorContext;

public final class ObjectiveCPreprocessor extends Preprocessor {
    private class State {
        public boolean skipping;
        public int nestedIfdefs;
        public File includeUnderAnalysis;

        public State(final File includeUnderAnalysis) {
            reset();
            this.includeUnderAnalysis = includeUnderAnalysis;
        }

        public final void reset() {
            skipping = false;
            nestedIfdefs = 0;
            includeUnderAnalysis = null;
        }
    }

    static class MismatchException extends Exception {
        private static final long serialVersionUID = 1L;
        private final String why;

        MismatchException(final String why) {
            this.why = why;
        }

        @Override
        public String toString() {
            return why;
        }
    }

    class Macro {
        public Macro(final String name, final List<Token> params,
                final List<Token> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        public String toString() {
            return name
                    + (params == null ? "" : "(" + serialize(params, ", ")
                            + ")") + " -> '" + serialize(body) + "'";
        }

        public String name;
        public List<Token> params;
        public List<Token> body;
    }

    private static final Logger LOG = LoggerFactory
            .getLogger("ObjectiveCPreprocessor");
    private Parser<CppGrammar> pplineParser = null;
    private Parser<CppGrammar> includeBodyParser = null;
    private final MapChain<String, Macro> macros = new MapChain<String, Macro>();
    private final Set<File> analysedFiles = new HashSet<File>();
    private final SourceCodeProvider codeProvider;
    private final SquidAstVisitorContext<ObjectiveCGrammar> context;
    private final ExpressionEvaluator ifExprEvaluator;

    // state which is not shared between files
    private State state = new State(null);
    private final Stack<State> stateStack = new Stack<State>();

    public ObjectiveCPreprocessor(
            final SquidAstVisitorContext<ObjectiveCGrammar> context) {
        this(context, new ObjectiveCConfiguration());
    }

    public ObjectiveCPreprocessor(
            final SquidAstVisitorContext<ObjectiveCGrammar> context,
            final ObjectiveCConfiguration conf) {
        this.context = context;
        this.ifExprEvaluator = new ExpressionEvaluator(conf, this);

        codeProvider = new SourceCodeProvider(conf);

        pplineParser = CppParser.create(conf);
        includeBodyParser = CppParser.createExpandedIncludeBodyParser(conf);
    }

    @Override
    public PreprocessorAction process(final List<Token> tokens) {
        final Token token = tokens.get(0);
        final TokenType ttype = token.getType();
        final File file = getFileUnderAnalysis();
        final String filePath = file == null ? token.getURI().toString() : file
                .getAbsolutePath();

        if (ttype == PREPROCESSOR) {

            AstNode lineAst = null;
            try {
                lineAst = pplineParser.parse(token.getValue()).getChild(0);
            } catch (final com.sonar.sslr.api.RecognitionException re) {
                LOG.warn("Cannot parse '{}', ignoring...", token.getValue());
                return new PreprocessorAction(1, Lists.newArrayList(Trivia
                        .createSkippedText(token)), new ArrayList<Token>());
            }

            final String lineKind = lineAst.getName();

            if ("ifdefLine".equals(lineKind)) {
                return handleIfdefLine(lineAst, token, filePath);
            } else if ("elseLine".equals(lineKind)) {
                return handleElseLine(lineAst, token, filePath);
            } else if ("endifLine".equals(lineKind)) {
                return handleEndifLine(lineAst, token, filePath);
            } else if ("ifLine".equals(lineKind)) {
                return handleIfLine(lineAst, token, filePath);
            } else if ("elifLine".equals(lineKind)) {
                return handleElIfLine(lineAst, token, filePath);
            }

            if (inSkippingMode()) {
                return new PreprocessorAction(1, Lists.newArrayList(Trivia
                        .createSkippedText(token)), new ArrayList<Token>());
            }

            if ("defineLine".equals(lineKind)) {
                return handleDefineLine(lineAst, token, filePath);
            } else if ("includeLine".equals(lineKind)) {
                return handleIncludeLine(lineAst, token, filePath);
            } else if ("importLine".equals(lineKind)) {
                return handleImportLine(lineAst, token, filePath);
            } else if ("undefLine".equals(lineKind)) {
                return handleUndefLine(lineAst, token, filePath);
            }

            // Ignore all other preprocessor directives (which are not handled
            // explicitly)
            // and strip them from the stream

            return new PreprocessorAction(1, Lists.newArrayList(Trivia
                    .createSkippedText(token)), new ArrayList<Token>());
        }

        if (ttype != EOF) {
            if (inSkippingMode()) {
                return new PreprocessorAction(1, Lists.newArrayList(Trivia
                        .createSkippedText(token)), new ArrayList<Token>());
            }

            if (ttype != STRING && ttype != NUMERIC_LITERAL) {
                return handleIdentifiersAndKeywords(tokens, token, filePath);
            }
        }

        return PreprocessorAction.NO_OPERATION;
    }

    public void beginPreprocessing(final File file) {
        // From 16.3.5 "Scope of macro definitions":
        // A macro definition lasts (independent of block structure) until
        // a corresponding #undef directive is encoun- tered or (if none
        // is encountered) until the end of the translation unit.

        LOG.debug("beginning preprocessing '{}'", file);

        analysedFiles.clear();
        macros.clearLowPrio();
        state.reset();
    }

    public String valueOf(final String macroname) {
        String result = null;
        final Macro macro = macros.get(macroname);
        if (macro != null) {
            result = serialize(macro.body);
        }
        return result;
    }

    private PreprocessorAction handleIfdefLine(final AstNode ast,
            final Token token, final String filename) {
        if (state.skipping) {
            state.nestedIfdefs++;
        } else {
            final Macro macro = macros.get(getMacroName(ast));
            final TokenType tokType = ast.getToken().getType();
            if ((tokType == IFDEF && macro == null)
                    || (tokType == IFNDEF && macro != null)) {
                LOG.trace(
                        "[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                        new Object[] { filename, token.getLine(),
                                token.getValue() });
                state.skipping = true;
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleElseLine(final AstNode ast, final Token token,
            final String filename) {
        if (state.nestedIfdefs == 0) {
            if (state.skipping) {
                LOG.trace("[{}:{}]: #else, returning to non-skipping mode",
                        filename, token.getLine());
            } else {
                LOG.trace("[{}:{}]: skipping tokens inside the #else",
                        filename, token.getLine());
            }

            state.skipping = !state.skipping;
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleEndifLine(final AstNode ast, final Token token,
            final String filename) {
        if (state.nestedIfdefs > 0) {
            state.nestedIfdefs--;
        } else {
            if (state.skipping) {
                LOG.trace("[{}:{}]: #endif, returning to non-skipping mode",
                        filename, token.getLine());
            }
            state.skipping = false;
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleIfLine(final AstNode ast, final Token token,
            final String filename) {
        if (state.skipping) {
            state.nestedIfdefs++;
        } else {
            LOG.trace("[{}:{}]: handling #if line '{}'", new Object[] {
                    filename, token.getLine(), token.getValue() });
            try {
                state.skipping = !ifExprEvaluator
                        .eval(ast.findFirstChild(pplineParser.getGrammar().constantExpression));
            } catch (final EvaluationException e) {
                LOG.error(
                        "[{}:{}]: error evaluating the expression {} assume 'true' ...",
                        new Object[] { filename, token.getLine(),
                                token.getValue() });
                LOG.error(e.toString());
                state.skipping = false;
            }

            if (state.skipping) {
                LOG.trace(
                        "[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                        new Object[] { filename, token.getLine(),
                                token.getValue() });
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleElIfLine(final AstNode ast, final Token token,
            final String filename) {
        // Handling of an elif line is similar to handling of an if line but
        // doesn't increase the nesting level
        if (state.nestedIfdefs == 0) {
            if (state.skipping) { // the preceeding clauses had been evaluated
                                  // to false
                try {
                    LOG.trace(
                            "[{}:{}]: handling #elif line '{}'",
                            new Object[] { filename, token.getLine(),
                                    token.getValue() });

                    // *this* preprocessor instance is used for evaluation, too.
                    // It *must not* be in skipping mode while evaluating
                    // expressions.
                    state.skipping = false;

                    state.skipping = !ifExprEvaluator
                            .eval(ast.findFirstChild(pplineParser.getGrammar().constantExpression));
                } catch (final EvaluationException e) {
                    LOG.error(
                            "[{}:{}]: error evaluating the expression {} assume 'true' ...",
                            new Object[] { filename, token.getLine(),
                                    token.getValue() });
                    LOG.error(e.toString());
                    state.skipping = false;
                }

                if (state.skipping) {
                    LOG.trace(
                            "[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                            new Object[] { filename, token.getLine(),
                                    token.getValue() });
                }
            } else {
                state.skipping = !state.skipping;
                LOG.trace("[{}:{}]: skipping tokens inside the #elif",
                        filename, token.getLine());
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleDefineLine(final AstNode ast, final Token token,
            final String filename) {
        // Here we have a define directive. Parse it and store the result in a
        // dictionary.

        final Macro macro = parseMacroDefinition(ast);
        if (macro != null) {
            LOG.trace("[{}:{}]: storing macro: '{}'", new Object[] { filename,
                    token.getLine(), macro });
            macros.putLowPrio(macro.name, macro);
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleIncludeLine(final AstNode ast, final Token token,
            final String filename) {
        //
        // Included files have to be scanned with the (only) goal of gathering
        // macros.
        // This is done as follows:
        //
        // a) pipe the body of the include directive through a lexer to properly
        // expand
        // all macros which may be in there.
        // b) extract the filename out of the include body and try to find it
        // c) if not done yet, process it using a special lexer, which calls
        // back only
        // if it finds relevant preprocessor directives (currently: include's
        // and define's)

        final String includeBody = serialize(
                stripEOF(ast.findFirstChild(INCLUDE).nextSibling().getTokens()),
                "");
        final String expandedIncludeBody = serialize(stripEOF(ObjectiveCLexer
                .create(this).lex(includeBody)), "");
        System.out
                .println("!!!putting into the parser: " + expandedIncludeBody);

        AstNode includeBodyAst = null;
        try {
            includeBodyAst = includeBodyParser.parse(expandedIncludeBody);
        } catch (final com.sonar.sslr.api.RecognitionException re) {
            LOG.warn("[{}:{}]: cannot parse included filename: {}'",
                    new Object[] { filename, token.getLine(),
                            expandedIncludeBody });
        }

        if (includeBodyAst != null) {
            final File includedFile = findIncludedFile(includeBodyAst);
            if (includedFile == null) {
                LOG.warn(
                        "[{}:{}]: cannot find the sources for '{}'",
                        new Object[] { filename, token.getLine(),
                                token.getValue() });
            } else if (!analysedFiles.contains(includedFile)) {
                analysedFiles.add(includedFile.getAbsoluteFile());
                LOG.trace(
                        "[{}:{}]: processing {}, resolved to file '{}'",
                        new Object[] { filename, token.getLine(),
                                token.getValue(),
                                includedFile.getAbsolutePath() });

                stateStack.push(state);
                state = new State(includedFile);

                try {
                    IncludeLexer.create(this).lex(
                            codeProvider.getSourceCode(includedFile));
                } finally {
                    state = stateStack.pop();
                }
            } else {
                LOG.trace(
                        "[{}:{}]: skipping already included file '{}'",
                        new Object[] { filename, token.getLine(), includedFile });
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    // TODO duplicated from the include but need to implement the import
    // functionality that avoids double imports.
    PreprocessorAction handleImportLine(final AstNode ast, final Token token,
            final String filename) {
        //
        // Included files have to be scanned with the (only) goal of gathering
        // macros.
        // This is done as follows:
        //
        // a) pipe the body of the include directive through a lexer to properly
        // expand
        // all macros which may be in there.
        // b) extract the filename out of the include body and try to find it
        // c) if not done yet, process it using a special lexer, which calls
        // back only
        // if it finds relevant preprocessor directives (currently: include's
        // and define's)


        if (ast != null) {
            final File includedFile = findIncludedFile(ast);
            if (includedFile == null) {
                LOG.warn(
                        "[{}:{}]: cannot find the sources for '{}'",
                        new Object[] { filename, token.getLine(),
                                token.getValue() });
            } else if (!analysedFiles.contains(includedFile)) {
                analysedFiles.add(includedFile.getAbsoluteFile());
                LOG.trace(
                        "[{}:{}]: processing {}, resolved to file '{}'",
                        new Object[] { filename, token.getLine(),
                                token.getValue(),
                                includedFile.getAbsolutePath() });

                stateStack.push(state);
                state = new State(includedFile);

                try {
                    IncludeLexer.create(this).lex(
                            codeProvider.getSourceCode(includedFile));
                } finally {
                    state = stateStack.pop();
                }
            } else {
                LOG.trace(
                        "[{}:{}]: skipping already included file '{}'",
                        new Object[] { filename, token.getLine(), includedFile });
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleUndefLine(final AstNode ast, final Token token,
            final String filename) {
        final String macroName = ast.findFirstChild(IDENTIFIER).getTokenValue();
        macros.removeLowPrio(macroName);
        return new PreprocessorAction(1, Lists.newArrayList(Trivia
                .createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleIdentifiersAndKeywords(final List<Token> tokens,
            final Token curr, final String filename) {
        //
        // Every identifier and every keyword can be a macro instance.
        // Pipe the resulting string through a lexer to create proper Tokens
        // and to expand recursively all macros which may be in there.
        //

        PreprocessorAction ppaction = PreprocessorAction.NO_OPERATION;
        final Macro macro = macros.get(curr.getValue());
        if (macro != null) {
            List<Token> replTokens = new LinkedList<Token>();
            int tokensConsumed = 0;
            final List<Token> arguments = new ArrayList<Token>();

            if (macro.params == null) {
                tokensConsumed = 1;
                replTokens = expandMacro(macro.name,
                        serialize(evaluateHashhashOperators(macro.body)));
            } else {
                final int tokensConsumedMatchingArgs = expandFunctionLikeMacro(
                        macro.name, tokens.subList(1, tokens.size()),
                        replTokens);
                if (tokensConsumedMatchingArgs > 0) {
                    tokensConsumed = 1 + tokensConsumedMatchingArgs;
                }
            }

            if (tokensConsumed > 0) {
                replTokens = reallocate(replTokens, curr);

                LOG.trace(
                        "[{}:{}]: replacing '"
                                + curr.getValue()
                                + (arguments.size() == 0 ? "" : "("
                                        + serialize(arguments, ", ") + ")")
                                + "' -> '" + serialize(replTokens) + "'",
                        filename, curr.getLine());

                ppaction = new PreprocessorAction(tokensConsumed,
                        Lists.newArrayList(Trivia.createSkippedText(tokens
                                .subList(0, tokensConsumed))), replTokens);
            }
        }

        return ppaction;
    }

    public String expandFunctionLikeMacro(final String macroName,
            final List<Token> restTokens) {
        final List<Token> expansion = new LinkedList<Token>();
        expandFunctionLikeMacro(macroName, restTokens, expansion);
        return serialize(expansion);
    }

    private int expandFunctionLikeMacro(final String macroName,
            final List<Token> restTokens, final List<Token> expansion) {
        List<Token> replTokens = null;
        final List<Token> arguments = new ArrayList<Token>();
        final int tokensConsumedMatchingArgs = matchArguments(restTokens,
                arguments);

        final Macro macro = macros.get(macroName);
        if (macro != null && macro.params.size() == arguments.size()) {
            replTokens = replaceParams(macro.body, macro.params, arguments);
            replTokens = evaluateHashhashOperators(replTokens);
            expansion.addAll(expandMacro(macro.name, serialize(replTokens)));
        }

        return tokensConsumedMatchingArgs;
    }

    private List<Token> expandMacro(final String macroName,
            final String macroExpression) {
        // C++ standard 16.3.4/2 Macro Replacement - Rescanning and further
        // replacement
        List<Token> tokens = null;
        macros.disable(macroName);
        try {
            tokens = stripEOF(ObjectiveCLexer.create(this).lex(macroExpression));
        } finally {
            macros.enable(macroName);
        }
        return tokens;
    }

    private List<Token> stripEOF(final List<Token> tokens) {
        if (tokens.get(tokens.size() - 1).getType() == EOF) {
            return tokens.subList(0, tokens.size() - 1);
        } else {
            return tokens;
        }
    }

    private String serialize(final List<Token> tokens) {
        return serialize(tokens, " ");
    }

    private String serialize(final List<Token> tokens, final String spacer) {
        final List<String> values = new LinkedList<String>();
        for (final Token t : tokens) {
            values.add(t.getValue());
        }
        return StringUtils.join(values, spacer);
    }

    private int matchArguments(final List<Token> tokens,
            final List<Token> arguments) {
        List<Token> rest = tokens;
        try {
            rest = match(rest, "(");
        } catch (final MismatchException me) {
            return 0;
        }

        try {
            do {
                rest = matchArgument(rest, arguments);
                try {
                    rest = match(rest, ",");
                } catch (final MismatchException me) {
                    break;
                }
            } while (true);

            rest = match(rest, ")");
        } catch (final MismatchException me) {
            LOG.error(me.toString());
            return 0;
        }

        return tokens.size() - rest.size();
    }

    private List<Token> match(final List<Token> tokens, final String str)
            throws MismatchException {
        if (!tokens.get(0).getValue().equals(str)) {
            throw new MismatchException("Mismatch: expected '" + str
                    + "' got: '" + tokens.get(0).getValue() + "'");
        }
        return tokens.subList(1, tokens.size());
    }

    private List<Token> matchArgument(final List<Token> tokens,
            final List<Token> arguments) throws MismatchException {
        int nestingLevel = 0;
        int tokensConsumed = 0;
        final int noTokens = tokens.size();
        final Token firstToken = tokens.get(0);
        Token currToken = firstToken;
        String curr = currToken.getValue();
        final List<Token> matchedTokens = new LinkedList<Token>();

        while (true) {
            if (nestingLevel == 0 && (",".equals(curr) || ")".equals(curr))) {
                if (tokensConsumed > 0) {
                    arguments.add(Token.builder().setLine(firstToken.getLine())
                            .setColumn(firstToken.getColumn())
                            .setURI(firstToken.getURI())
                            .setValueAndOriginalValue(serialize(matchedTokens))
                            .setType(STRING).build());
                }
                return tokens.subList(tokensConsumed, noTokens);
            }

            if (curr.equals("(")) {
                nestingLevel++;
            } else if (curr.equals(")")) {
                nestingLevel--;
            }

            tokensConsumed++;
            if (tokensConsumed == noTokens) {
                throw new MismatchException(
                        "reached the end of the stream while matching a macro argument");
            }

            matchedTokens.add(currToken);
            currToken = tokens.get(tokensConsumed);
            curr = currToken.getValue();
        }
    }

    private List<Token> replaceParams(final List<Token> body,
            final List<Token> parameters, final List<Token> arguments) {
        // Replace all parameters by according arguments
        // "Stringify" the argument if the according parameter is preceded by an
        // #

        final List<Token> newTokens = new ArrayList<Token>();
        if (body.size() != 0) {
            final List<String> defParamValues = new ArrayList<String>();
            for (final Token t : parameters) {
                defParamValues.add(t.getValue());
            }

            for (int i = 0; i < body.size(); ++i) {
                final Token curr = body.get(i);
                final int index = defParamValues.indexOf(curr.getValue());
                if (index != -1) {
                    final Token replacement = arguments.get(index);

                    // TODO: maybe we should pipe the argument through the whole
                    // expansion
                    // engine before doing the replacement
                    // String newValue = serialize(expandMacro("",
                    // replacement.getValue()));

                    String newValue = replacement.getValue();

                    if (i > 0 && body.get(i - 1).getValue().equals("#")) {
                        newTokens.remove(newTokens.size() - 1);
                        newValue = encloseWithQuotes(quote(newValue));
                    }
                    newTokens.add(Token.builder()
                            .setLine(replacement.getLine())
                            .setColumn(replacement.getColumn())
                            .setURI(replacement.getURI())
                            .setValueAndOriginalValue(newValue)
                            .setType(replacement.getType())
                            .setGeneratedCode(true).build());
                } else {
                    newTokens.add(curr);
                }
            }
        }

        return newTokens;
    }

    private List<Token> evaluateHashhashOperators(final List<Token> tokens) {
        final List<Token> newTokens = new ArrayList<Token>();

        final Iterator<Token> it = tokens.iterator();
        while (it.hasNext()) {
            final Token curr = it.next();
            if (curr.getValue().equals("##")) {
                final Token pred = predConcatToken(newTokens);
                final Token succ = succConcatToken(it);
                newTokens
                        .add(Token
                                .builder()
                                .setLine(pred.getLine())
                                .setColumn(pred.getColumn())
                                .setURI(pred.getURI())
                                .setValueAndOriginalValue(
                                        pred.getValue() + succ.getValue())
                                .setType(pred.getType()).setGeneratedCode(true)
                                .build());
            } else {
                newTokens.add(curr);
            }
        }

        return newTokens;
    }

    private Token predConcatToken(final List<Token> tokens) {
        while (!tokens.isEmpty()) {
            final Token last = tokens.remove(tokens.size() - 1);
            if (last.getType() != WS) {
                return last;
            }
        }
        return null;
    }

    private Token succConcatToken(final Iterator<Token> it) {
        Token succ = null;
        while (it.hasNext()) {
            succ = it.next();
            if (!succ.getValue().equals("##") && succ.getType() != WS) {
                break;
            }
        }
        return succ;
    }

    private String quote(final String str) {
        return StringUtils.replaceEach(str, new String[] { "\\", "\"" },
                new String[] { "\\\\", "\\\"" });
    }

    private String encloseWithQuotes(final String str) {
        return "\"" + str + "\"";
    }

    private List<Token> reallocate(final List<Token> tokens, final Token token) {
        final List<Token> reallocated = new LinkedList<Token>();
        int currColumn = token.getColumn();
        for (final Token t : tokens) {
            reallocated.add(Token.builder().setLine(token.getLine())
                    .setColumn(currColumn).setURI(token.getURI())
                    .setValueAndOriginalValue(t.getValue())
                    .setType(t.getType()).setGeneratedCode(true).build());
            currColumn += t.getValue().length() + 1;
        }

        return reallocated;
    }

    private Macro parseMacroDefinition(final AstNode defineLineAst) {
        final AstNode ast = defineLineAst.getChild(0);
        final AstNode nameNode = ast
                .findFirstChild(pplineParser.getGrammar().ppToken);
        final String macroName = nameNode.getTokenValue();

        final AstNode paramList = ast
                .findFirstChild(pplineParser.getGrammar().parameterList);
        final List<Token> macroParams = paramList == null ? ast.getName()
                .equals("objectlikeMacroDefinition") ? null
                : new LinkedList<Token>() : getParams(paramList);

        final AstNode replList = ast
                .findFirstChild(pplineParser.getGrammar().replacementList);
        final List<Token> macroBody = replList == null ? new LinkedList<Token>()
                : replList.getTokens().subList(0,
                        replList.getTokens().size() - 1);

        return new Macro(macroName, macroParams, macroBody);
    }

    private List<Token> getParams(final AstNode identListAst) {
        final List<Token> params = new ArrayList<Token>();
        if (identListAst != null) {
            for (final AstNode node : identListAst
                    .findDirectChildren(IDENTIFIER)) {
                params.add(node.getToken());
            }
        }

        return params;
    }

    private File findIncludedFile(final AstNode ast) {
        String fileName = null;
        File includedFile = null;
        boolean quoted = false;

        AstNode node = ast.findFirstChild(STRING);
        if (node != null) {
            fileName = stripQuotes(node.getTokenValue());
            quoted = true;
        } else if ((node = ast.findFirstChild(LT)) != null) {
            node = node.nextSibling();
            final StringBuilder sb = new StringBuilder();
            while (true) {
                final String value = node.getTokenValue();
                if (value.equals(">")) {
                    break;
                }
                sb.append(value);
                node = node.nextSibling();
            }

            fileName = sb.toString();
        }

        if (fileName != null) {
            includedFile = codeProvider.getSourceCodeFile(fileName, quoted);
        }

        return includedFile;
    }

    private String getMacroName(final AstNode ast) {
        return ast.findFirstChild(IDENTIFIER).getTokenValue();
    }

    private String stripQuotes(final String str) {
        return str.substring(1, str.length() - 1);
    }

    private File getFileUnderAnalysis() {
        if (state.includeUnderAnalysis == null) {
            return context.getFile();
        }
        return state.includeUnderAnalysis;
    }

    private boolean inSkippingMode() {
        return state.skipping;
    }
}
