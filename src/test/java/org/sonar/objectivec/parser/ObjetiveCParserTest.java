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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;
import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCGrammar;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;

public final class ObjetiveCParserTest {

    @Test
    public void parserHandlesCallPredeclarationKeyword() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());

        parser.parse("\n@class TestClass;\n");
    }

    @Test
    public void parserHandlesImplementationKeyword() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());

        parser.parse("\n@implementation TestClass\n @end\n");
    }

    @Test
    public void parserHandlesInstanceVariableDeclarations() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());

        parser.parse("@implementation TestClass{}@end");
    }

    @Test
    public void parserHandlesMethodDeclarationWithMultipleParameters() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse("@implementation TestClass\n- (NSString *) test:(double) param other:(NSString *) param {\n}\n@end");
    }

    @Test
    public void parserHandlesImportPreprocessing() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse("#import \"OrganizationViewController.h\"\n#import \"UIViewController+I5Cache.h\"\n #import \"Flurry.h\"\n\n@implementation OrganizationViewController\n@end\n");
    }

    @Test
    public void parserHandlesInterfaceAndInheritance() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse("#import <UIKit/UIKit.h>\n@interface AdvertViewController : UIViewController\n@end\n");
    }

    @Test
    public void parserHandlesProtocolImplementationDeclaration() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse("#import <UIKit/UIKit.h>\n@interface AdvertViewController : UIViewController<UITableViewDataSource, UITableViewDelegate>\n@end\n");
    }

    @Test
    public void parserHandlesDeclaration() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().structDeclaration);
        parser.parse("UIWebView *content;");
    }

    @Test
    public void parserHandleMethodType() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().methodType);
        parser.parse("(UIWebView *)");
    }

    @Test
    public void parserHandlesMemberDeclaration() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse("#import <UIKit/UIKit.h>\n@interface AdvertViewController : UIViewController {\n@protected\nNSDictionary * agendaItem;\n}\n@property (nonatomic, strong) IBOutlet UIWebView *content;\n@end\n");
    }

    @Test
    public void parserHandlesFunctionDefinition() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().functionDefinition);
        parser.parse("int main(int argc, char *argv[])\n{\n}\n");

    }

    @Test
    public void parserHandlesStuctTypeDef() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        final AstNode node = parser.parse("typedef struct tagRS_BLOCKINFO {\nint ncDataCodeWord;\nint ncDataCodeWord;\n} RS_BLOCKINFO, *LPRS_BLOCKINFO;");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesConstTypeDef() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().declarationSpecifier);
        final AstNode node = parser.parse("const char* LPCTR");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesFunctionDeclaration() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.setRootRule(parser.getGrammar().functionDeclaration);
        final AstNode node = parser.parse("int EncodeData(int nLevel, int nVersion , LPCSTR lpsSource, unsigned sourcelen, unsigned char QR_m_data[MAX_BITDATA]);");
        assertThat(node.getNumberOfChildren(), equalTo(2));
    }

    @Test
    public void parserHandlesFile() {
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser
                .create(new ObjectiveCConfiguration());
        parser.parse(new File("/Users/dbregeon/Development/xcode/pandabear/PebbleQRPayment/PebbleQRPayment/QR_Encode.h"));
    }
}
