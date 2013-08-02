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

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Predicate.not;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.and;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.o2n;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.one2n;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.opt;
import static com.sonar.sslr.impl.matcher.GrammarFunctions.Standard.or;

import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.api.ObjectiveCKeyword;
import org.sonar.objectivec.api.ObjectiveCTokenType;

import com.sonar.sslr.api.GenericTokenType;


/**
 * Extensively based off of https://github.com/iamdc/Objective-C-Grammar/blob/master/ObjC.Grm
 * and http://pornel.net/objective-c-grammar
 */
public class ObjectiveCGrammarImpl extends ObjectiveCGrammar {

    @SuppressWarnings("deprecation")
    public ObjectiveCGrammarImpl() {

        program.is(opt(programContent), EOF);

        programContent.is(or(typeDeclaration, categoryInterface, categoryImplementation, protocolDeclaration, classImplementation, classInterface, classDeclarationList, protocolDeclarationList, functionDefinition, functionDeclaration), opt(programContent));

        identifier.is(IDENTIFIER);

        arrayLiteral.is("@","[", opt(expression, o2n(and(",", postfixExpression))), "]");

        dictionaryLiteral.is("@","{", opt(dictionaryElement, o2n(and(",", dictionaryElement))), "}");
        dictionaryElement.is(postfixExpression, ":", postfixExpression);

        autorelease();
        synchronization();
        exceptionHandling();
        category();
        protocolDeclaration();
        classDeclaration();
        classInterface();
        classImplementation();
        expressions();
    }

    private void autorelease() {
        autorealeasePool.is(ObjectiveCKeyword.AT_AUTORELEASEPOOL, statement);
    }

    private void synchronization() {
        synchronizedStatement.is(ObjectiveCKeyword.AT_SYNCHRONIZED, "(", expression,  ")", statement);
    }

    @SuppressWarnings("deprecation")
    private void exceptionHandling() {
        tryBlock.is(ObjectiveCKeyword.TRY, statement, or(
                and(one2n(catchClause), opt(finallyStatement)),
                finallyStatement
                ));

        catchClause.is(ObjectiveCKeyword.CATCH, "(", or(parameterDeclaration, "..."), ")", statement);

        finallyStatement.is(ObjectiveCKeyword.FINALLY, statement);
    }

    private void category() {
        categoryInterface.is(ObjectiveCKeyword.AT_INTERFACE, className, "(", opt(categoryName), ")", opt(protocolReferenceList), opt(instanceVariables), opt(interfaceDeclarationList), ObjectiveCKeyword.END);

        categoryName.is(GenericTokenType.IDENTIFIER);

        categoryImplementation.is(ObjectiveCKeyword.AT_IMPLEMENTATION, className, "(", categoryName, ")", opt(instanceVariables), o2n(implementationDefinitionList), ObjectiveCKeyword.END);
    }

    @SuppressWarnings("deprecation")
    private void protocolDeclaration() {
        protocolDeclarationList.is(ObjectiveCKeyword.AT_PROTOCOL, protocolList, ";");

        protocolDeclaration.is(ObjectiveCKeyword.AT_PROTOCOL, protocolName, opt(protocolReferenceList), protocolInterfaceDeclaration, ObjectiveCKeyword.END);

        protocolInterfaceDeclaration.is(opt(interfaceDeclarationList), o2n(qualifiedProtocolInterfaceDeclaration));

        qualifiedProtocolInterfaceDeclaration.is(or(ObjectiveCKeyword.AT_OPTIONAL, ObjectiveCKeyword.AT_REQUIRED), opt(interfaceDeclarationList));
    }

    private void classDeclaration() {
        classDeclarationList.is(ObjectiveCKeyword.AT_CLASS, classList, ";");

        classList.is(className, o2n(and(",", className)));
    }

    @SuppressWarnings("deprecation")
    private void classInterface() {
        classInterface.is(ObjectiveCKeyword.AT_INTERFACE, className, opt(superClassReference), opt(protocolReferenceList), opt(instanceVariables), opt(interfaceDeclarationList), ObjectiveCKeyword.END);

        interfaceDeclarationList.is(or(
                declaration,
                methodDeclaration,
                propertyDeclaration),
                opt(interfaceDeclarationList)
        );

        methodDeclaration.is(or("+", "-"), opt(methodType), methodSelector, ";");

        propertyDeclaration.is(ObjectiveCKeyword.AT_PROPERTY, opt(propertyAttributesDeclaration), structDeclaration);

        propertyAttributesDeclaration.is("(", propertyAttributes, ")");

        propertyAttributes.is(propertyAttribute, o2n(and(",", propertyAttribute)));

        propertyAttribute.is(or(
                ObjectiveCKeyword.ASSIGN,
                ObjectiveCKeyword.READWRITE,
                ObjectiveCKeyword.READONLY,
                ObjectiveCKeyword.RETAIN,
                ObjectiveCKeyword.COPY,
                ObjectiveCKeyword.ATOMIC,
                ObjectiveCKeyword.NONATOMIC,
                ObjectiveCKeyword.STRONG,
                ObjectiveCKeyword.WEAK
                ));
    }

    @SuppressWarnings("deprecation")
    private void classImplementation() {
        classImplementation.is(ObjectiveCKeyword.AT_IMPLEMENTATION, className, opt(superClassReference), opt(instanceVariables), o2n(implementationDefinitionList), ObjectiveCKeyword.END);

        implementationDefinitionList.is(or(
                functionDefinition,
                declaration,
                classMethodDefinition,
                instanceMethodDefinition,
                propertyImplementation
                ));

        functionDefinition.is(functionSignature, compoundStatement);

        functionDeclaration.is(functionSignature, ";");

        functionSignature.is(or(storageClassSpecifier, typeSpecifier, typeQualifier, functionSpecifier), or(declarator, functionSignature));

        compoundStatement.is("{", o2n(blockItem), "}");

        blockItem.is(or(
                declaration,
                statement
                ));

        declaration.is(
                or(
                        typeDeclaration,
                        one2n(declarationSpecifier, opt(initDeclaratorList))
                ), ";"
        );

        typeDeclaration.is(ObjectiveCKeyword.TYPEDEF, typeDeclarationSpecifier, typeDeclaratorList, ";");

        typeDeclarationSpecifier.is(not(and(typeDeclaratorList, ";")), or(storageClassSpecifier, typeSpecifier, typeQualifier, functionSpecifier), opt(typeDeclarationSpecifier));

        typeDeclaratorList.is(typeDeclarator, opt(and(",", typeDeclaratorList)));

        typeDeclarator.is(opt(pointer), typeDirectDeclarator);

        typeDirectDeclarator.is(or(
                    and("(", typeDeclarator, ")"),
                    identifier),
                o2n(or(
                        and("[", opt(constantExpression), "]"),
                        and("(", opt(parameterTypeList), ")"),
                        and("(", opt(identifierList), ")")
                        ))
                );

        statement.is(or(
                and(expression, ";"),
                compoundStatement,
                labeledStatement,
                selectionStatement,
                iterationStatement,
                jumpStatement,
                tryBlock,
                synchronizedStatement,
                autorealeasePool
                ));

        labeledStatement.is(or(
                and(GenericTokenType.IDENTIFIER, ":", statement),
                and(ObjectiveCKeyword.CASE, constantExpression, ":", statement),
                and(ObjectiveCKeyword.DEFAULT, ":", statement)
                ));

        selectionStatement.is(or(
                and(ObjectiveCKeyword.IF, "(", expression, ")", statement, opt(elseStatement)),
                and(ObjectiveCKeyword.SWITCH, "(", expression, ")", statement)
                ));

        elseStatement.is(ObjectiveCKeyword.ELSE, statement);

        iterationStatement.is(or(
                and(ObjectiveCKeyword.WHILE, "(", expression, ")", statement),
                and(ObjectiveCKeyword.DO, statement, ObjectiveCKeyword.WHILE, "(", expression, ")", ";"),
                and(ObjectiveCKeyword.FOR, "(", expression, ";", expression, ";", opt(expression), ")", statement),
                and(ObjectiveCKeyword.FOR, "(", declaration, expression, ";", opt(expression), ")", statement),
                and(ObjectiveCKeyword.FOR, "(", forInIterationVariable, ObjectiveCKeyword.IN, expression, ")")
                ));

        forInIterationVariable.is(or(
                parameterDeclaration,
                GenericTokenType.IDENTIFIER
                ));

        jumpStatement.is(or(
                and(ObjectiveCKeyword.GOTO, GenericTokenType.IDENTIFIER, ";"),
                and(ObjectiveCKeyword.CONTINUE, ";"),
                and(ObjectiveCKeyword.BREAK, ";"),
                and(ObjectiveCKeyword.RETURN, opt(expression), ";")
                ));

        classMethodDefinition.is("+", methodDefinition);

        instanceMethodDefinition.is("-", methodDefinition);

        methodDefinition.is(opt(methodType), methodSelector, opt(initDeclaratorList), compoundStatement);

        propertyImplementation.is(or(
                and(ObjectiveCKeyword.AT_SYNTHESIZE, propertySynthesizeList, ";"),
                and(ObjectiveCKeyword.AT_DYNAMIC, identifierList, ";")
                ));

        propertySynthesizeList.is(
                propertySynthesizeItem,
                o2n(and(",", propertySynthesizeItem))
                );

        propertySynthesizeItem.is(GenericTokenType.IDENTIFIER, opt("=", GenericTokenType.IDENTIFIER));

        constantExpression.is(conditionalExpression);

        methodType.is("(", or(anonymousFunctionDeclarator, typeNameWithUnknowType), ")");

        typeNameWithUnknowType.is(one2n(specifierQualifierWithUnknownType), opt(abstractDeclarator));

        specifierQualifierWithUnknownType.is(or(typeSpecifierWithUnknownType, typeQualifier, protocolQualifier));

        typeSpecifierWithUnknownType.is(or(typeSpecifier, and(identifier, opt(protocolReferenceList))));

        methodSelector.is(or(
                and(keywordSelector, opt(or(
                        and(",", "..."),
                        and(",", parameterTypeList)
                ))),
                GenericTokenType.IDENTIFIER
                ));

        keywordSelector.is(keywordDeclarator, o2n(keywordDeclarator));

        keywordDeclarator.is(or(
                and(":", opt(methodType), GenericTokenType.IDENTIFIER),
                and(GenericTokenType.IDENTIFIER, ":", opt(methodType), GenericTokenType.IDENTIFIER)
                ));

        className.is(GenericTokenType.IDENTIFIER);

        superClassReference.is(":", superClassName);

        superClassName.is(GenericTokenType.IDENTIFIER);

        instanceVariables.is("{", o2n(and(opt(visibilitySpecification), structDeclaration, opt(instanceVariables))), "}");

        visibilitySpecification.is(or(
                ObjectiveCKeyword.AT_PRIVATE,
                ObjectiveCKeyword.AT_PUBLIC,
                ObjectiveCKeyword.AT_PROTECTED
                ));

        structDeclarationList.is(structDeclaration, opt(structDeclaration));

        structDeclaration.is(or(and(specifierQualifier, structDeclaration), and(structDeclaratorList, ";")));

        specifierQualifier.is(or(
                typeSpecifier,
                typeQualifier,
                protocolQualifier
                ));

        protocolQualifier.is(or(ObjectiveCKeyword.OUT, ObjectiveCKeyword.IN, ObjectiveCKeyword.INOUT, ObjectiveCKeyword.BYCOPY, ObjectiveCKeyword.BYREF, ObjectiveCKeyword.ONEWAY));

        builtInType.is(or(
                ObjectiveCKeyword.VOID,
                ObjectiveCKeyword.CHAR,
                ObjectiveCKeyword.SHORT,
                ObjectiveCKeyword.INT,
                ObjectiveCKeyword.LONG,
                ObjectiveCKeyword.FLOAT,
                ObjectiveCKeyword.DOUBLE
                ));

        typeSpecifier.is(or(
                builtInType,
                ObjectiveCKeyword.SIGNED,
                ObjectiveCKeyword.UNSIGNED,
                structOrUnionSpecifier,
                enumSpecifier,
                and(typedefedIdentifier, opt(protocolReferenceList))
                ));

        anonymousFunctionDeclarator.is(typeSpecifier, "(", "^", opt(identifier), ")", "(", opt(parameterTypeList), ")");

        structOrUnionSpecifier.is(or(
                and(structOrUnion, opt(GenericTokenType.IDENTIFIER), "{", o2n(structDeclaration), "}"),
                and(structOrUnion, opt(GenericTokenType.IDENTIFIER), "{", ObjectiveCKeyword.AT_DEFS, "(", className, ")", "}"),
                and(structOrUnion, opt(GenericTokenType.IDENTIFIER))
                ));

        structOrUnion.is(or(
                ObjectiveCKeyword.STRUCT,
                ObjectiveCKeyword.UNION
                ));

        enumSpecifier.is(or(
                and(ObjectiveCKeyword.ENUM, opt(GenericTokenType.IDENTIFIER), "{", enumeratorList, "}"),
                and(ObjectiveCKeyword.ENUM, opt(GenericTokenType.IDENTIFIER), "{", enumeratorList, ",", "}"),
                and(ObjectiveCKeyword.ENUM, GenericTokenType.IDENTIFIER)
                ));

        enumeratorList.is(enumerator, o2n(and( ",", enumerator)));

        enumerator.is(or(
                GenericTokenType.IDENTIFIER,
                and(GenericTokenType.IDENTIFIER, "=", constantExpression)
                ));

        typeQualifier.is(or(
                ObjectiveCKeyword.CONST,
                ObjectiveCKeyword.VOLATILE
                ));

        structDeclaratorList.is(structDeclarator, o2n(and(",", structDeclarator)));

        structDeclarator.is(or(
                and(opt(declarator), ":", constantExpression),
                declarator
                ));

        storageClassSpecifier.is(or(
                ObjectiveCKeyword.EXTERN,
                ObjectiveCKeyword.STATIC,
                ObjectiveCKeyword.AUTO,
                ObjectiveCKeyword.REGISTER,
                block
                ));

        declspec.is("(", "__declspec", ")");

        block.is("__block");

        initDeclaratorList.is(initDeclarator, o2n(and(",", initDeclarator)));

        initDeclarator.is(declarator, opt("=", initializer));

        initializer.is(or(
                assignmentExpression,
                and("{", initializerList, "}"),
                and("{", initializerList, ",", "}")
        ));

        initializerList.is(opt(designation), initializer, opt(and(",", initializerList)));

        designation.is(one2n(designator), "=");

        designator.is(or(
                and("[", constantExpression, "]"),
                and(".", identifier)
                ));

        declarator.is(opt(pointer), directDeclarator);

        directDeclarator.is(or(
                GenericTokenType.IDENTIFIER,
                and("(", declarator, ")")),
                o2n(or(
                        and("[", opt(constantExpression), "]"),
                        and("(", opt(parameterTypeList), ")"),
                        and("[", opt(identifierList), "]")
                        ))
                );

        parameterTypeList.is(
                parameterList,
                opt(and(",", "..."))
        );

        parameterList.is(parameterDeclaration, opt(and(",", parameterList)));

        parameterDeclaration.is(or(storageClassSpecifier, typeSpecifier, typeQualifier, functionSpecifier), or(declarator, parameterDeclaration, opt(abstractDeclarator)));

        abstractDeclarator.is(or(
                and(opt(pointer), directAbstractDeclarator),
                pointer
                ));

        directAbstractDeclarator.is(opt("(", abstractDeclarator, ")"), o2n(or(
                and("(", opt(parameterTypeList), ")"),
                and("[", opt(assignmentExpression), "]"),
                and("[", "*", "]")
                ))
                );

        declarationSpecifier.is(or(storageClassSpecifier, typeSpecifier, typeQualifier, functionSpecifier), opt(and(not(and(initDeclarator, or(";", ",", ")"))), declarationSpecifier)));

        functionSpecifier.is(ObjectiveCKeyword.INLINE);


        identifierList.is(GenericTokenType.IDENTIFIER, o2n(and(",", GenericTokenType.IDENTIFIER)));

        pointer.is(or(
                and("*", opt(typeQualifierList)),
                and("*", pointer),
                and("*", typeQualifierList, pointer)
                ));

        typeQualifierList.is(typeQualifier, opt(and(",", typeQualifierList)));

        protocolReferenceList.is("<", protocolList, ">");

        protocolList.is(protocolName, o2n(and(",", protocolName)));

        protocolName.is(GenericTokenType.IDENTIFIER);

        argumentExpressionList.is(assignmentExpression, o2n(and(",", assignmentExpression)));

        protocolExpression.is(ObjectiveCKeyword.AT_PROTOCOL, "(", GenericTokenType.IDENTIFIER, ")");

        encodeExpression.is(ObjectiveCKeyword.AT_ENCODE, "(", GenericTokenType.IDENTIFIER, ")");
    }


    @SuppressWarnings("deprecation")
    private void expressions() {
        expression.is(
                assignmentExpression,
                o2n(and(",", assignmentExpression)
        ));
        assignmentExpression.is(or(
                and(anonymousFunctionDeclarator, "=", blockExpression),
                and(unaryExpression, or("=", "+=", "-=", "/=", "*=", "%=", "^=", "<<=", ">>=", "&=", "|="), assignmentExpression),
                conditionalExpression
        ));
        unaryExpression.is(or(
                postfixExpression,
                and(unaryOperator, castExpression),
                and("++", unaryExpression),
                and("--", unaryExpression),
                and(ObjectiveCKeyword.SIZEOF, or(
                        and("(", typeName, ")"),
                        unaryExpression
                        ))
        ));
        postfixExpression.is(or(
                primaryExpression,
                and("(", typeName, ")", "{", initializerList, ",", "}"),
                and("(", typeName, ")", "{", initializerList, "}")),
                o2n(or(
                        and("[", expression, "]"),
                        and("(", opt(argumentExpressionList), ")"),
                        and(".", identifier),
                        and("->", identifier),
                        and("++"),
                        and("--")
                        ))

        );

        unaryOperator.is(or(
                "&",
                "*",
                "+",
                "-",
                "~",
                "!"
                ));

        primaryExpression.is(or(
                identifier,
                constant,
                and("(", expression, ")"),
                arrayLiteral,
                dictionaryLiteral,
//                nsStringLiteral,
                messageExpression,
                selectorExpression,
                protocolExpression,
                encodeExpression,
                blockExpression
        ));

        blockExpression.is("^", opt(identifier), opt(and("(", parameterTypeList, ")")), compoundStatement);

        messageExpression.is("[", receiver, messageSelector, "]");

        receiver.is(or(
                expression,
                typedefedIdentifier
        ));

        typedefedIdentifier.is(or("BOOL", "IMP", "SEL", "Class", "id", identifier));

        constant.is(or(
                ObjectiveCTokenType.NUMERIC_LITERAL,
//                hexLiteral,
                ObjectiveCTokenType.CHARACTER,
                ObjectiveCTokenType.STRING_LITERAL,
                nsNumberLiteral
        ));

        nsNumberLiteral.is("@", "(", ObjectiveCTokenType.NUMERIC_LITERAL, ")");

        conditionalExpression.is(logicalOrExpression, o2n(and("?", expression, ":", conditionalExpression)));

        logicalOrExpression.is(logicalAndExpression, o2n(and("||", logicalAndExpression)));

        logicalAndExpression.is(inclusiveOrExpression, o2n(and("&&", inclusiveOrExpression)));

        inclusiveOrExpression.is(exclusiveOrExpression, o2n(and("|", exclusiveOrExpression)));

        exclusiveOrExpression.is(andExpression, o2n(and("^", andExpression)));

        andExpression.is(equalityExpression, o2n(and("&", equalityExpression)));

        equalityExpression.is(or(
                and(relationalExpression, "==", relationalExpression),
                and(relationalExpression, "!=", relationalExpression),
                relationalExpression
                ));

        relationalExpression.is(shiftExpression,
                o2n(or(
                        and("<", shiftExpression),
                        and(">", shiftExpression),
                        and("<=", shiftExpression),
                        and(">=", shiftExpression)
                )));

        shiftExpression.is(additiveExpression,
                o2n(or(
                        and("<<", additiveExpression),
                        and(">>", additiveExpression)
                 )));

        additiveExpression.is(multiplicativeExpression,
                o2n(or(
                    and("+", multiplicativeExpression),
                    and("-", multiplicativeExpression)
                )));
        multiplicativeExpression.is(castExpression,
                o2n(or(
                        and("*", castExpression),
                        and("/", castExpression),
                        and("%", castExpression)
                )));

        castExpression.is(or(
                and("(", typeName, ")", castExpression),
                unaryExpression
                ));

        messageSelector.is(GenericTokenType.IDENTIFIER,
                o2n(keywordArgument));
        keywordArgument.is(or(
                and(GenericTokenType.IDENTIFIER, ":", expression),
                and(":", expression)
             ));

        selectorExpression.is(ObjectiveCKeyword.AT_SELECTOR, "(", selectorName, ")");
        selectorName.is(
                GenericTokenType.IDENTIFIER,
                one2n(keywordName)
                );
        keywordName.is(opt(GenericTokenType.IDENTIFIER), ":");

        typeName.is(one2n(specifierQualifier), opt(abstractDeclarator));
    }
}
