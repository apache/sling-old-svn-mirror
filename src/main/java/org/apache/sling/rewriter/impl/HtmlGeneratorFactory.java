/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.rewriter.impl;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * On the fly HTML parser which can be used as the
 * starting point for html pipelines.
 *
 * @scr.component metatype="no"
 * @scr.service
 * @scr.property name="pipeline.type" value="html-generator"
 */
public class HtmlGeneratorFactory implements GeneratorFactory {

    public static String NAMESPACE = "http://org.apache.sling/rewriter";

    public static String END_SLASH_ATTR = "endSlash";

    public static String QUOTES_ATTR = "quotes";

    public static final String INCLUDE_TAGS_PROPERTY = "includeTags";

    private static final Set<String> DEFAULT_INCLUSION_TAGS;
    static {
        DEFAULT_INCLUSION_TAGS = new HashSet<String>();
        DEFAULT_INCLUSION_TAGS.add("A");
        DEFAULT_INCLUSION_TAGS.add("/A");
        DEFAULT_INCLUSION_TAGS.add("IMG");
        DEFAULT_INCLUSION_TAGS.add("AREA");
        DEFAULT_INCLUSION_TAGS.add("FORM");
        DEFAULT_INCLUSION_TAGS.add("BASE");
        DEFAULT_INCLUSION_TAGS.add("LINK");
        DEFAULT_INCLUSION_TAGS.add("SCRIPT");
        DEFAULT_INCLUSION_TAGS.add("/BODY");
    }

    /**
     * @see org.apache.sling.rewriter.GeneratorFactory#createGenerator()
     */
    public Generator createGenerator() {
        return new HtmlGenerator();
    }

    public static final class HtmlGenerator extends Writer implements Generator {

        /** Internal character buffer */
        private final CharArrayWriter buffer = new CharArrayWriter(256);

        /** Tag tokenizer */
        private final TagTokenizer tokenizer = new TagTokenizer();

        /** Tag name buffer */
        private final CharArrayWriter tagNameBuffer = new CharArrayWriter(30);

        /** Tag name */
        private String tagName;

        /** Tag inclusion list */
        private Set<String> tagInclusionSet;

        /** Registered content handler */
        private ContentHandler contentHandler;

        /** Parse state constant */
        private final static int PS_OUTSIDE = 0;

        /** Parse state constant */
        private final static int PS_TAG = PS_OUTSIDE + 1;

        /** Parse state constant */
        private final static int PS_SCRIPT = PS_TAG + 1;

        /** Parse state constant */
        private final static int PS_COMMENT = PS_SCRIPT + 1;

        /** Parse state constant */
        private final static int PS_STRING = PS_COMMENT + 1;

        /** Tag type constant */
        private final static int TT_NONE = 0;

        /** Tag type constant */
        private final static int TT_MAYBE = 1;

        /** Tag type constant */
        private final static int TT_TAG = 2;

        /** Parse state */
        private int parseState;

        /** Parse substate */
        private int parseSubState;

        /** Previous parse state */
        private int prevParseState;

        /** Current tag type */
        private int tagType;

        /** Quote character */
        private char quoteChar;

        /** Did we already start parsing? */
        boolean started = false;

        private final org.xml.sax.helpers.AttributesImpl atts = new org.xml.sax.helpers.AttributesImpl();

        /**
         * Default constructor.
         */
        public HtmlGenerator() {
            this.tagInclusionSet = DEFAULT_INCLUSION_TAGS;
        }

        /**
         * @see org.apache.sling.rewriter.Generator#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessingComponentConfiguration)
         */
        public void init(ProcessingContext pipelineContext,
                         ProcessingComponentConfiguration config) {
            final String[] includedTags = OsgiUtil.toStringArray(config
                    .getConfiguration().get(INCLUDE_TAGS_PROPERTY));
            if (includedTags != null && includedTags.length > 0) {
                this.tagInclusionSet = new HashSet<String>();
                for (final String tag : includedTags) {
                    this.tagInclusionSet.add(tag);
                }
                // we always have to include body!
                this.tagInclusionSet.add("/BODY");
            }
        }

        /**
         * @see org.apache.sling.rewriter.Generator#getWriter()
         */
        public PrintWriter getWriter() {
            return new PrintWriter(this);
        }

        public Set<String> getTagInclusionSet() {
            return tagInclusionSet;
        }

        public void setTagInclusionSet(Set<String> tagInclusionSet) {
            this.tagInclusionSet = tagInclusionSet;
        }

        /**
         * @see org.apache.sling.rewriter.Generator#setContentHandler(org.xml.sax.ContentHandler)
         */
        public void setContentHandler(ContentHandler handler) {
            this.contentHandler = handler;
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException {
            this.update(cbuf, 0, len);
        }

        @Override
        public void write(int b) throws IOException {
            final char[] buf = new char[] { (char) b };
            this.update(buf, 0, buf.length);
        }

        @Override
        public void close() throws IOException {
            // nothing to do
        }

        @Override
        public void flush() throws IOException {
            flushBuffer();

            // send 0-length characters that eventually let the serializer flush the
            // underlying writer
            try {
                this.contentHandler.characters(new char[0], 0, 0);
            } catch (SAXException e) {
                throw handle(e);
            }
        }

        /**
         * Feed characters to the parser.
         *
         * @param buf
         *            character buffer
         * @param off
         *            offset where characters start
         * @param len
         *            length of affected buffer
         */
        public void update(char[] buf, int off, int len) throws IOException {
            if (!this.started) {
                try {
                    this.contentHandler.startDocument();
                } catch (SAXException se) {
                    this.handle(se);
                }
                this.started = true;
            }
            int start = off;
            int end = off + len;

            for (int curr = start; curr < end; curr++) {
                char c = buf[curr];

                switch (parseState) {
                case PS_OUTSIDE:
                    if (c == '<') {
                        if (curr > start) {
                            try {
                                this.contentHandler.characters(buf, start, curr - start);
                            } catch (SAXException e) {
                                throw handle(e);
                            }
                        }
                        start = curr;
                        parseState = PS_TAG;
                        parseSubState = 0;
                        tagType = TT_MAYBE;
                        resetTagName();
                    }
                    break;
                case PS_TAG:
                    switch (parseSubState) {
                    case -1:
                        if (c == '"' || c == '\'') {
                            quoteChar = c;
                            prevParseState = parseState;
                            parseState = PS_STRING;
                            parseSubState = -1;
                        } else if (c == '>') {
                            parseState = PS_OUTSIDE;
                        }
                        break;
                    case 0:
                        if (c == '!') {
                            parseState = PS_COMMENT;
                            parseSubState = 0;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else if (c == '"' || c == '\'') {
                            quoteChar = c;
                            prevParseState = parseState;
                            parseState = PS_STRING;
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else if (c == '>') {
                            parseState = PS_OUTSIDE;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else if (!Character.isWhitespace(c)) {
                            tagNameBuffer.write(c);
                            parseSubState = 1;
                        } else {
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        }
                        break;
                    case 1:
                        if (c == '"' || c == '\'') {
                            if (tagIncluded(getTagName())) {
                                tagType = TT_TAG;
                            } else {
                                tagType = TT_NONE;
                                flushBuffer();
                            }
                            parseSubState = 2;
                            quoteChar = c;
                            prevParseState = parseState;
                            parseState = PS_STRING;
                        } else if (c == '>') {
                            if (tagIncluded(getTagName())) {
                                processTag(buf, start, curr - start + 1);
                                start = curr + 1;
                                tagType = TT_NONE;
                                parseState = getTagName()
                                        .equalsIgnoreCase("SCRIPT") ? PS_SCRIPT
                                        : PS_OUTSIDE;
                                parseSubState = 0;
                            } else {
                                tagType = TT_NONE;
                                flushBuffer();
                                parseState = PS_OUTSIDE;
                            }
                        } else if (Character.isWhitespace(c)) {
                            if (tagIncluded(getTagName())) {
                                tagType = TT_TAG;
                            } else {
                                tagType = TT_NONE;
                                flushBuffer();
                            }
                            parseSubState = 2;
                        } else {
                            tagNameBuffer.write(c);
                        }
                        break;
                    case 2:
                        if (c == '"' || c == '\'') {
                            quoteChar = c;
                            prevParseState = parseState;
                            parseState = PS_STRING;
                        } else if (c == '>') {
                            if (tagType == TT_TAG) {
                                processTag(buf, start, curr - start + 1);
                                start = curr + 1;
                            } else {
                                flushBuffer();
                            }
                            tagType = TT_NONE;
                            parseState = getTagName().equalsIgnoreCase("SCRIPT") ? PS_SCRIPT
                                    : PS_OUTSIDE;
                            parseSubState = 0;
                        }
                        break;
                    }
                    break;
                case PS_COMMENT:
                    switch (parseSubState) {
                    case 0:
                        if (c == '-') {
                            parseSubState++;
                        } else if (c == '"' || c == '\'') {
                            quoteChar = c;
                            prevParseState = PS_TAG;
                            parseState = PS_STRING;
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else if (c == '>') {
                            parseState = PS_OUTSIDE;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else {
                            parseState = PS_TAG;
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        }
                        break;
                    case 1:
                        if (c == '-') {
                            parseSubState++;
                        } else if (c == '"' || c == '\'') {
                            quoteChar = c;
                            prevParseState = PS_TAG;
                            parseState = PS_STRING;
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else if (c == '>') {
                            parseState = PS_OUTSIDE;
                            tagType = TT_NONE;
                            flushBuffer();
                        } else {
                            parseState = PS_TAG;
                            parseSubState = -1;
                            tagType = TT_NONE;
                            flushBuffer();
                        }
                        break;
                    case 2:
                        if (c == '-') {
                            parseSubState++;
                        }
                        break;
                    case 3:
                        if (c == '-') {
                            parseSubState++;
                        } else {
                            parseSubState = 2;
                        }
                        break;
                    case 4:
                        if (c == '>') {
                            parseState = PS_OUTSIDE;
                        } else {
                            parseSubState = 2;
                        }
                        break;
                    }
                    break;

                case PS_SCRIPT:
                    switch (parseSubState) {
                    case 0:
                        if (c == '<') {
                            if (curr > start) {
                                try {
                                    this.contentHandler.characters(buf, start, curr - start);
                                } catch (SAXException e) {
                                    throw handle(e);
                                }
                            }
                            start = curr;
                            tagType = TT_MAYBE;
                            parseSubState++;
                        }
                        break;
                    case 1:
                        if (c == '/') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 2:
                        if (c == 'S' || c == 's') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 3:
                        if (c == 'C' || c == 'c') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 4:
                        if (c == 'R' || c == 'r') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 5:
                        if (c == 'I' || c == 'i') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 6:
                        if (c == 'P' || c == 'p') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 7:
                        if (c == 'T' || c == 't') {
                            parseSubState++;
                        } else {
                            tagType = TT_NONE;
                            flushBuffer();
                            parseSubState = 0;
                        }
                        break;
                    case 8:
                        if (c == '>') {
                            if (tagIncluded("SCRIPT")) {
                                processTag(buf, start, curr - start + 1);
                                start = curr + 1;
                            } else {
                                flushBuffer();
                            }
                            tagType = TT_NONE;
                            parseState = PS_OUTSIDE;
                        }
                        break;
                    }
                    break;

                case PS_STRING:
                    if (c == quoteChar) {
                        parseState = prevParseState;
                    }
                    break;
                }
            }
            if (start < end) {
                if (tagType == TT_NONE) {
                    try {
                        this.contentHandler.characters(buf, start, end - start);
                    } catch (SAXException e) {
                        throw handle(e);
                    }
                } else {
                    buffer.write(buf, start, end - start);
                }
            }
        }

        /**
         * Return a flag indicating whether the parser has still some undigested
         * characters left.
         *
         * @return <code>true</code> if the parser still contains characters
         *         <code>false</code> otherwise
         */
        public boolean isEmpty() {
            return buffer.size() == 0;
        }

        /**
         * Finish the parsing process. This forces the parser to flush the
         * characters still held in its internal buffer, regardless of the parsing
         * state.
         */
        public void finished() throws IOException {
            flushBuffer();
            if ( this.started ) {
                try {
                    this.contentHandler.endDocument();
                } catch (SAXException e) {
                    throw handle(e);
                }

            }
        }

        /**
         * Clears the internal tagname buffer and cache
         */
        protected void resetTagName() {
            tagName = null;
            tagNameBuffer.reset();
        }

        /**
         * Returns the tagname scanned and resets the internal tagname buffer
         *
         * @return tagname
         */
        protected String getTagName() {
            if (tagName == null) {
                tagName = tagNameBuffer.toString();
            }
            return tagName;
        }

        /**
         * Flush internal buffer. This forces the parser to flush the characters
         * still held in its internal buffer, regardless of the parsing state.
         */
        protected void flushBuffer() throws IOException {
            if (buffer.size() > 0) {
                char[] ch = buffer.toCharArray();
                try {
                    this.contentHandler.characters(ch, 0, ch.length);
                } catch (SAXException e) {
                    throw handle(e);
                }
                buffer.reset();
            }
        }

        /**
         * Returns a flag indicating whether the specified tag should be included in
         * the parsing process.
         *
         * @param tagName
         *            tag name
         * @return <code>true</code> if the tag should be processed, else
         *         <code>false</code>
         */
        protected boolean tagIncluded(String tagName) {
            return tagInclusionSet == null
                    || tagInclusionSet.contains(tagName.toUpperCase());
        }

        /**
         * Decompose a tag and feed it to the document handler.
         *
         * @param ch
         *            character data
         * @param off
         *            offset where character data starts
         * @param len
         *            length of character data
         */
        protected void processTag(char[] ch, int off, int len) throws IOException {
            buffer.write(ch, off, len);

            char[] snippet = buffer.toCharArray();

            tokenizer.tokenize(snippet, 0, snippet.length);
            if (!tokenizer.endTag()) {
                final AttributeList attributes = tokenizer.attributes();
                final String tagName = tokenizer.tagName();
                this.atts.clear();

                final char[] quotes = new char[attributes.attributeCount()];
                int index = 0;
                final Iterator<String> names = attributes.attributeNames();
                while (names.hasNext()) {
                    final String name = names.next();
                    final String value = attributes.getValue(name);
                    if (value != null) {
                        this.atts.addAttribute("", name, name, "CDATA", value);
                    } else {
                        this.atts.addAttribute("", name, name, "CDATA", "");
                    }
                    quotes[index] = attributes.getQuoteChar(name);
                    index++;
                }
                if ( index > 0 ) {
                    this.atts.addAttribute(NAMESPACE, QUOTES_ATTR, QUOTES_ATTR, "CDATA", new String(quotes));
                }
                try {
                    if (tokenizer.endSlash()) {
                        // just tell the contentHandler via attribute that an end slash is needed
                        this.atts.addAttribute("", END_SLASH_ATTR, END_SLASH_ATTR, "CDATA", "");
                    }
                    this.contentHandler.startElement("", tagName, tagName, this.atts);
                } catch (SAXException e) {
                    throw handle(e);
                }
            } else {
                try {
                    final String tagName = tokenizer.tagName();
                    this.contentHandler.endElement("", tagName, tagName);
                } catch (SAXException e) {
                    throw handle(e);
                }
            }

            buffer.reset();
        }

        protected final IOException handle(SAXException se) {
            if ( se.getCause() != null && se.getCause() instanceof IOException) {
                return (IOException)se.getCause();
            }
            final IOException ioe = new IOException("Unable to parse document");
            ioe.initCause(se);
            return ioe;
        }

        /**
         * @see org.apache.sling.rewriter.Generator#dispose()
         */
        public void dispose() {
            // nothing to do
        }
    }
}
