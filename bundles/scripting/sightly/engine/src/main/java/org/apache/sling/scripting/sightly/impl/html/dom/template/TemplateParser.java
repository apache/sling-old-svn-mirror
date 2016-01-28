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
package org.apache.sling.scripting.sightly.impl.html.dom.template;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.scripting.sightly.impl.html.dom.AttributeList;
import org.apache.sling.scripting.sightly.impl.html.dom.DocumentHandler;
import org.apache.sling.scripting.sightly.impl.html.dom.HtmlParser;

/**
 * The template parser parses an HTML document and returns a reusable tree
 * representation.
 */
public class TemplateParser {

    /**
     * Parse an html document
     * @param reader to be parsed
     * @throws IOException in case of any parsing error
     *
     * @return a Template
     */
    public Template parse(final Reader reader) throws IOException {
        final TemplateParserContext context = new TemplateParserContext();

        HtmlParser.parse(reader, context);

        return context.getTemplate();
    }

    public static final class TemplateParserContext implements DocumentHandler {

        /** Used for text/character events */
        private StringBuilder textBuilder = new StringBuilder();

        /** Element stack - root is the Template */
        private final Deque<TemplateElementNode> elementStack = new ArrayDeque<TemplateElementNode>();

        /** The template. */
        private Template template;

        public Template getTemplate() {
            return this.template;
        }

        public void onStart() throws IOException {
            this.template = new Template();
            this.elementStack.push(this.template);
        }

        public void onEnd() throws IOException {
            this.checkText();
            this.elementStack.clear();
        }

        private void checkText() {
            if (textBuilder.length() > 0) {
                elementStack.peek().addChild(new TemplateTextNode(textBuilder.toString()));
                this.textBuilder = new StringBuilder();
            }
        }

        public void onStartElement(String name, AttributeList attList, boolean endSlash) {
            this.checkText();
            final List<TemplateAttribute> attrs = new ArrayList<TemplateAttribute>();
            final Iterator<String> iter = attList.attributeNames();
            while ( iter.hasNext() ) {
                final String aName = iter.next();
                final TemplateAttribute attr = new TemplateAttribute(aName, attList.getValue(aName), attList.getQuoteChar(aName));
                attrs.add(attr);
            }
            final TemplateElementNode element = new TemplateElementNode(name, endSlash, attrs);
            element.setHasStartElement();
            elementStack.peek().addChild(element);
            if ( !endSlash ) {
                elementStack.push(element);
            }
        }

        public void onEndElement(String name) {
            this.checkText();
            if (contains(name)) {
                TemplateElementNode element = this.elementStack.pop();
                while ( !name.equals(element.getName()) ) {
                    element = this.elementStack.pop();
                }
                element.setHasEndElement();
            } else {
                final TemplateElementNode element
                        = new TemplateElementNode(name, false, new ArrayList<TemplateAttribute>());
                elementStack.peek().addChild(element);
                element.setHasEndElement();
            }
        }

        public void onCharacters(final char[] ch, final int off, final int len) {
            textBuilder.append(ch, off, len);
        }

        public void onComment(final String text) throws IOException {
            this.checkText();
            elementStack.peek().addChild(new TemplateCommentNode(text));
        }

        private boolean contains(String name) {
            for (TemplateElementNode elem : this.elementStack) {
                if (name.equals(elem.getName())) {
                    return true;
                }
            }
            return false;
        }
    }
}
