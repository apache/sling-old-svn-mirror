/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.html.dom;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.impl.html.dom.template.*;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HtmlParserTest {

    /**
     * Pretty print a template nodes structure for debugging of failed tests
     *
     * @param indentation - indentation level (the method is used recursively)
     * @param node - template nodes to print
     */
    private static void print(int indentation, TemplateNode node) {
        if (node == null) {
            return;
        }

        List<TemplateNode> children = null;
        String name = "UNKNOWN";


        if (node.getClass() == Template.class) {
            Template template = (Template)node;
            children = template.getChildren();
            name = template.getName();
        } else if (node.getClass() == TemplateElementNode.class) {
            TemplateElementNode element = (TemplateElementNode)node;
            children = element.getChildren();
            name = "ELEMENT: " + element.getName();
        } else if (node.getClass() == TemplateTextNode.class) {
            name = "TEXT: " + ((TemplateTextNode)node).getText();
        } else if (node.getClass() == TemplateCommentNode.class) {
            name = "COMMENT: " + ((TemplateCommentNode)node).getText();
        }

        System.out.print(StringUtils.repeat("\t", indentation));
        System.out.println(name.replace("\n","\\n").replace("\r", "\\r"));
        if (children == null) {
            return;
        }
        for (TemplateNode child : children) {
            print(indentation + 1, child);
        }
    }

    /**
     * Assert helper to compare two template nodes structures
     *
     * @param reference - reference nodes structure
     * @param parsed - parsed nodes structure
     */
    private static void assertSameStructure(TemplateNode reference, TemplateNode parsed) {
        if (parsed == null || reference == null) {
            assertTrue("Expecting both null", parsed == reference);
            return;
        }
        assertEquals("Expecting same class", reference.getClass(), parsed.getClass());

        List<TemplateNode> parsedChildren = null, referenceChildren = null;

        if (parsed.getClass() == Template.class) {
            Template parsedTemplate = (Template)parsed;
            Template referenceTemplate = (Template)reference;
            assertEquals("Expecting same name",
                    referenceTemplate.getName(),
                    parsedTemplate.getName());
            parsedChildren = parsedTemplate.getChildren();
            referenceChildren = referenceTemplate.getChildren();
        } else if (parsed.getClass() == TemplateElementNode.class) {
            TemplateElementNode parsedElement = (TemplateElementNode)parsed;
            TemplateElementNode referenceElement = (TemplateElementNode)reference;
            assertEquals("Expecting same name",
                    referenceElement.getName(),
                    parsedElement.getName());
            parsedChildren = parsedElement.getChildren();
            referenceChildren = referenceElement.getChildren();
        } else if (parsed.getClass() == TemplateTextNode.class) {
            assertEquals("Expecting same content",
                    ((TemplateTextNode)reference).getText(),
                    ((TemplateTextNode)parsed).getText());
        } else if (parsed.getClass() == TemplateCommentNode.class) {
            assertEquals("Expecting same content",
                    ((TemplateCommentNode)reference).getText(),
                    ((TemplateCommentNode)parsed).getText());
        }

        if (parsedChildren == null || referenceChildren == null) {
            assertTrue("Expecting both children null", parsedChildren == referenceChildren);
            return;
        }

        assertEquals("Expecting same number of children",
                parsedChildren.size(),
                referenceChildren.size());

        for (int i = 0, n = parsedChildren.size(); i < n; i++) {
            assertSameStructure(parsedChildren.get(i), referenceChildren.get(i));
        }
    }

    /**
     * Create a basic template nodes structure containing one text nodes and one comment nodes
     *
     * @param textAndComment - String containing text (optional) and comment
     * @return
     */
    private Template createReference(String textAndComment) {
        int commentIx = textAndComment.indexOf("<!");
        if (commentIx < 0 || commentIx > textAndComment.length()) {
            throw new IndexOutOfBoundsException("String must contain text and comment");
        }
        Template reference = new Template();
        if (commentIx > 0) {
            reference.addChild(new TemplateTextNode(
                    textAndComment.substring(0, commentIx)));
        }
        reference.addChild(new TemplateCommentNode(
                textAndComment.substring(commentIx)));
        return reference;
    }

    @Test
    public void testParseCommentSpanningAcrossCharBuffer() throws Exception {
        String[] testStrings = new String[] {
                "<!--/* comment */-->",
                "1<!--/* comment */-->",
                "12<!--/* comment */-->",
                "123<!--/* comment */-->",
                "1234<!--/* comment */-->",
                "12345<!--/* comment */-->",
                "123456<!--/* comment */-->",
                "1234567<!--/* comment */-->",
                "12345678<!--/* comment */-->",
                "123456789<!--/* comment */-->",
                "1234567890<!--/* comment */-->",
                "12345678901<!--/* comment */-->"
        };
        Template reference = null, parsed = null;
        Whitebox.setInternalState(HtmlParser.class, "BUF_SIZE", 10);

        try {
            for (String test : testStrings) {
                StringReader reader = new StringReader(test);
                reference = createReference(test);

                TemplateParser.TemplateParserContext context = new TemplateParser.TemplateParserContext();
                HtmlParser.parse(reader, context);
                parsed = context.getTemplate();

                assertSameStructure(parsed, reference);
            }
        } catch (AssertionError e) {
            System.out.println("Reference: ");
            print(0, reference);
            System.out.println("Parsed: ");
            print(0, parsed);
            throw e;
        }
    }
}
