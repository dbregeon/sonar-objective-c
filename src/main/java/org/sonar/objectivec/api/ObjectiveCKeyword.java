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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum ObjectiveCKeyword implements TokenType {

    // method param/return value annotations
    IN("in"),
    OUT("out"),
    INOUT("inout"),
    BYCOPY("bycopy"),
    BYREF("byref"),
    ONEWAY("oneway"),

    // const is defined in c keywords
    // compiler directives

    AT_CLASS("@class"), // forward declaration
    AT_DEFS("@defs"), // returns layout of an obj-c class
    AT_PROTOCOL("@protocol"), // protocol declaration
    AT_REQUIRED("@required"),
    AT_OPTIONAL("@optional"),
    AT_INTERFACE("@interface"), // class or category declaration
    AT_PUBLIC("@public"),
    AT_PACKAGE("@package"),
    AT_PROTECTED("@protected"),
    AT_PRIVATE("@private"),
    AT_PROPERTY("@property"),
    AT_IMPLEMENTATION("@implementation"), // class or category implementation
    AT_SYNTHESIZE("@synthesize"),
    AT_DYNAMIC("@dynamic"),

    // remaining directives

    END("@end"), // ends protocol/class/category declaration/implementation
    AT_SYNCHRONIZED("@synchronized"), // mutex
    AT_AUTORELEASEPOOL("@autoreleasepool"),
    AT_SELECTOR("@selector"), // returns SEL type of a method
    AT_ENCODE("@encode"), // return char encoding of a type
    AT_COMPATIBILITY_ALIAS("@compatibility_alias"),

    // preprocessor directives

    HASH_IMPORT("#import"),
    HASH_INCLUDE("#include"),
    HASH_PRAGMA("#pragma"),
    HASH_DEFINE("#define"),
    HASH_UNDEF("#undef"),
    HASH_IF("#if"),
    HASH_IFDEF("#ifdef"),
    HASH_IFNDEF("#ifndef"),
    HASH_ELSE("#else"),
    HASH_ENDIF("#endif"),

    // keywords from c

    AUTO("auto"),
    BREAK("break"),
    CASE("case"),
    CHAR("char"),
    CONST("const"),
    CONTINUE("continue"),
    DEFAULT("default"),
    DO("do"),
    DOUBLE("double"),
    ELSE("else"),
    ENUM("enum"),
    EXTERN("extern"),
    FLOAT("float"),
    FOR("for"),
    GOTO("goto"),
    IF("if"),
    INT("int"),
    LONG("long"),
    REGISTER("register"),
    RETURN("return"),
    SHORT("short"),
    SIGNED("signed"),
    SIZEOF("sizeof"),
    STATIC("static"),
    STRUCT("struct"),
    SWITCH("switch"),
    TYPEDEF("typedef"),
    UNION("union"),
    UNSIGNED("unsigned"),
    VOID("void"),
    VOLATILE("volatile"),
    WHILE("while"),

    // exception handling

    THROW("@throw"),
    FINALLY("@finally"),
    CATCH("@catch"),
    TRY("@try"),

    INLINE("inline");

    private final String value;

    private ObjectiveCKeyword(final String value) {
        this.value = value;
    }

    public String getName() {
        return name();
    }

    public String getValue() {
        return value;
    }

    public boolean hasToBeSkippedFromAst(final AstNode node) {
        return false;
    }

    public static String[] keywordValues() {
        final ObjectiveCKeyword[] keywordsEnum = ObjectiveCKeyword.values();
        final String[] keywords = new String[keywordsEnum.length];
        for (int i = 0; i < keywords.length; i++) {
            keywords[i] = keywordsEnum[i].getValue();
        }
        return keywords;
    }

}
