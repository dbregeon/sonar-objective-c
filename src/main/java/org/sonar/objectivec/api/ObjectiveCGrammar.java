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
package org.sonar.objectivec.api;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;

public class ObjectiveCGrammar extends Grammar {

    public Rule identifierName;

    // A.1 Lexical
    public Rule nsNumberLiteral;
    public Rule arrayLiteral;
    public Rule dictionaryLiteral;
    public Rule dictionaryElement;

    public Rule program;
    public Rule programContent;

    public Rule propertyDeclaration;

    public Rule methodDeclaration;
    public Rule methodType;
    public Rule identifier;
    public Rule compoundStatement;
    public Rule statement;
    public Rule typeName;
    public Rule declaration;

    // Autorelease
    public Rule autorealeasePool;

    //Synchronization
    public Rule synchronizedStatement;

    // Exception handling
    public Rule tryBlock;
    public Rule catchClause;
    public Rule finallyStatement;

    // Category
    public Rule categoryInterface;
    public Rule categoryName;
    public Rule categoryImplementation;

    // Protocol Declaration
    public Rule protocolDeclarationList;
    public Rule protocolDeclaration;
    public Rule protocolInterfaceDeclaration;
    public Rule qualifiedProtocolInterfaceDeclaration;

    // Class Declaration
    public Rule classDeclarationList;
    public Rule classList;

    // Class Interface
    public Rule classInterface;
    public Rule interfaceDeclarationList;
    public Rule propertyAttributesDeclaration;
    public Rule propertyAttributes;
    public Rule propertyAttribute;

    // Class Implementation
    public Rule classImplementation;
    public Rule className;
    public Rule superClassReference;
    public Rule instanceVariables;
    public Rule implementationDefinitionList;
    public Rule functionDefinition;
    public Rule functionDeclaration;
    public Rule functionSignature;
    public Rule classMethodDefinition;
    public Rule instanceMethodDefinition;
    public Rule methodDefinition;
    public Rule propertyImplementation;
    public Rule propertySynthesizeItem;
    public Rule declarator;
    public Rule blockItem;
    public Rule labeledStatement;
    public Rule selectionStatement;
    public Rule iterationStatement;
    public Rule jumpStatement;
    public Rule elseStatement;
    public Rule forInIterationVariable;
    public Rule parameterDeclaration;
    public Rule methodSelector;
    public Rule initDeclaratorList;
    public Rule propertySynthesizeList;
    public Rule identifierList;
    public Rule keywordSelector;
    public Rule parameterTypeList;
    public Rule keywordDeclarator;
    public Rule superClassName;
    public Rule structDeclarationList;
    public Rule visibilitySpecification;
    public Rule structDeclaration;
    public Rule specifierQualifier;
    public Rule structDeclaratorList;
    public Rule typeSpecifier;
    public Rule typeQualifier;
    public Rule builtInType;
    public Rule structOrUnionSpecifier;
    public Rule enumSpecifier;
    public Rule protocolReferenceList;
    public Rule structOrUnion;
    public Rule enumeratorList;
    public Rule enumerator;
    public Rule structDeclarator;
    public Rule storageClassSpecifier;
    public Rule declspec;
    public Rule initDeclarator;
    public Rule initializer;
    public Rule directDeclarator;
    public Rule parameterList;
    public Rule declarationSpecifier;
    public Rule functionSpecifier;
    public Rule anonymousFunctionDeclarator;
    public Rule abstractDeclarator;
    public Rule pointer;
    public Rule typeQualifierList;
    public Rule protocolList;
    public Rule protocolName;
    public Rule typeDeclaration;
    public Rule typeDeclarator;
    public Rule typeDirectDeclarator;
    public Rule directAbstractDeclarator;
    public Rule protocolQualifier;
    public Rule typeNameWithUnknowType;
    public Rule specifierQualifierWithUnknownType;
    public Rule typeSpecifierWithUnknownType;
    public Rule typeDeclarationSpecifier;
    public Rule typeDeclaratorList;

    // Expressions
    public Rule expression;
    public Rule assignmentExpression;
    public Rule unaryExpression;
    public Rule conditionalExpression;
    public Rule postfixExpression;
    public Rule castExpression;
    public Rule primaryExpression;
    public Rule argumentExpressionList;
    public Rule messageExpression;
    public Rule selectorExpression;
    public Rule protocolExpression;
    public Rule encodeExpression;
    public Rule logicalOrExpression;
    public Rule logicalAndExpression;
    public Rule inclusiveOrExpression;
    public Rule exclusiveOrExpression;
    public Rule andExpression;
    public Rule equalityExpression;
    public Rule relationalExpression;
    public Rule shiftExpression;
    public Rule additiveExpression;
    public Rule multiplicativeExpression;
    public Rule constantExpression;
    public Rule blockExpression;


    // Operators
    public Rule unaryOperator;

    // ??
    public Rule initializerList;
    public Rule designation;
    public Rule designator;
    public Rule constant;
    public Rule receiver;
    public Rule typedefedIdentifier;
    public Rule messageSelector;
    public Rule keywordArgument;
    public Rule selectorName;
    public Rule keywordName;
    public Rule block;

    @Override
    public Rule getRootRule() {
        return program;
    }

}
