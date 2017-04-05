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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;

/**
 * HTML parser. Invokes a <code>DocumentHandler</code> whenever an event occurs.
 */
public final class HtmlParser {

    private static int BUF_SIZE = 2048;

    /** Internal character buffer */
    private final CharArrayWriter buffer = new CharArrayWriter(256);

    /** Tag tokenizer */
    private final TagTokenizer tokenizer = new TagTokenizer();

    /** Tag name buffer */
    private final CharArrayWriter tagNameBuffer = new CharArrayWriter(30);

    /** Tag name */
    private String tagName;

    /** Registered document handler */
    private final DocumentHandler documentHandler;

    private enum PARSE_STATE {
        OUTSIDE,
        TAG,
        SCRIPT,
        COMMENT,
        STRING,
        EXPRESSION
    }

    /** Tag type constant */
    private final static int TT_NONE = 0;

    /** Tag type constant */
    private final static int TT_MAYBE = 1;

    /** Tag type constant */
    private final static int TT_TAG = 2;

    /** Expression state constant */
    private final static int EXPR_NONE = 0;

    /** Expression state constant */
    private final static int EXPR_MAYBE = 1;

    /** Parse state */
    private PARSE_STATE parseState = PARSE_STATE.OUTSIDE;

    /** Parse substate */
    private int parseSubState;

    /** Previous parse state */
    private PARSE_STATE prevParseState;

    /** Current tag type */
    private int tagType;

    /** Expression type */
    private int exprType;

    /** Quote character */
    private char quoteChar;

    public static void parse(final Reader reader, final DocumentHandler documentHandler)
    throws IOException {
        final HtmlParser parser = new HtmlParser(documentHandler);
        parser.parse(reader);
    }

    /**
     * Default constructor.
     */
    private HtmlParser(final DocumentHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    private void parse(final Reader reader)
    throws IOException {
        try {
            this.documentHandler.onStart();
            final char[] readBuffer = new char[BUF_SIZE];
            int readLen = 0;
            while ( (readLen = reader.read(readBuffer)) > 0 ) {
                this.update(readBuffer, readLen);
            }
            this.flushBuffer();
            this.documentHandler.onEnd();
        } finally {
            try {
                reader.close();
            } catch ( final IOException ignore) {
                // ignore
            }
        }
    }

    /**
     * Feed characters to the parser.
     *
     * @param buf character buffer
     * @param len length of affected buffer
     */
    private void update(final char[] buf, int len) throws IOException {
        int start = 0;
        final int end = len;

        for (int curr = start; curr < end; curr++) {
            final char c = buf[curr];

            switch (parseState) {
            case OUTSIDE:
                if (c == '<') {
                    if (curr > start) {
                        documentHandler.onCharacters(buf, start, curr - start);
                    }
                    start = curr;
                    parseState = PARSE_STATE.TAG;
                    parseSubState = 0;
                    tagType = TT_MAYBE;
                    resetTagName();
                } else if (c == '$') {
                    exprType = EXPR_MAYBE;
                    parseState = PARSE_STATE.EXPRESSION;
                }
                break;
            case TAG:
                switch (parseSubState) {
                case -1:
                    if (c == '"' || c == '\'') {
                        quoteChar = c;
                        prevParseState = parseState;
                        parseState = PARSE_STATE.STRING;
                        parseSubState = -1;
                    } else if (c == '>') {
                        parseState = PARSE_STATE.OUTSIDE;
                    }
                    break;
                case 0:
                    if (c == '!') {
                        parseState = PARSE_STATE.COMMENT;
                        parseSubState = 0;
                        tagType = TT_NONE;
                        // keep the accumulated buffer
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        prevParseState = parseState;
                        parseState = PARSE_STATE.STRING;
                        parseSubState = -1;
                        tagType = TT_NONE;
                        flushBuffer();
                    } else if (c == '>') {
                        parseState = PARSE_STATE.OUTSIDE;
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
                        tagType = TT_TAG;
                        parseSubState = 2;
                        quoteChar = c;
                        prevParseState = parseState;
                        parseState = PARSE_STATE.STRING;
                    } else if (c == '>') {
                        parseState = processTag(buf, start, curr - start + 1) ? PARSE_STATE.SCRIPT : PARSE_STATE.OUTSIDE;
                        start = curr + 1;
                        tagType = TT_NONE;
                        parseSubState = 0;
                    } else if (Character.isWhitespace(c)) {
                        tagType = TT_TAG;
                        parseSubState = 2;
                    } else {
                        tagNameBuffer.write(c);
                    }
                    break;
                case 2:
                    if (c == '"' || c == '\'') {
                        quoteChar = c;
                        prevParseState = parseState;
                        parseState = PARSE_STATE.STRING;
                    } else if (c == '>') {
                        if (tagType == TT_TAG) {
                            parseState = processTag(buf, start, curr - start + 1) ? PARSE_STATE.SCRIPT : PARSE_STATE.OUTSIDE;
                            start = curr + 1;
                        } else {
                            flushBuffer();
                            parseState = "SCRIPT".equalsIgnoreCase(getTagName()) ? PARSE_STATE.SCRIPT : PARSE_STATE.OUTSIDE;
                        }
                        tagType = TT_NONE;
                        parseSubState = 0;
                    }
                    break;
                default:
                    break;
                }
                break;
            case COMMENT:
                switch (parseSubState) {
                case 0:
                    if (c == '-') {
                        parseSubState++;
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        prevParseState = PARSE_STATE.TAG;
                        parseState = PARSE_STATE.STRING;
                        parseSubState = -1;
                        tagType = TT_NONE;
                        flushBuffer();
                    } else if (c == '>') {
                        parseState = PARSE_STATE.OUTSIDE;
                        tagType = TT_NONE;
                        flushBuffer();
                    } else {
                        parseState = PARSE_STATE.TAG;
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
                        prevParseState = PARSE_STATE.TAG;
                        parseState = PARSE_STATE.STRING;
                        parseSubState = -1;
                        tagType = TT_NONE;
                        flushBuffer();
                    } else if (c == '>') {
                        parseState = PARSE_STATE.OUTSIDE;
                        tagType = TT_NONE;
                        flushBuffer();
                    } else {
                        parseState = PARSE_STATE.TAG;
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
                        parseState = PARSE_STATE.OUTSIDE;
                        processComment(buf, start, curr - start + 1);
                        start = curr + 1;
                    } else {
                        parseSubState = 2;
                    }
                    break;
                default:
                    break;
                }
                break;

            case SCRIPT:
                switch (parseSubState) {
                case 0:
                    if (c == '<') {
                        if (curr > start) {
                            documentHandler.onCharacters(buf, start, curr - start);
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
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 2:
                    if (c == 'S' || c == 's') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 3:
                    if (c == 'C' || c == 'c') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 4:
                    if (c == 'R' || c == 'r') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 5:
                    if (c == 'I' || c == 'i') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 6:
                    if (c == 'P' || c == 'p') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 7:
                    if (c == 'T' || c == 't') {
                        parseSubState++;
                    } else {
                        tagType = TT_NONE;
                        parseSubState = 0;
                        flushBuffer();
                    }
                    break;
                case 8:
                    if (c == '>') {
                        processTag(buf, start, curr - start + 1);
                        start = curr + 1;
                        tagType = TT_NONE;
                        parseState = PARSE_STATE.OUTSIDE;
                    }
                    break;
                default:
                    break;
                }
                break;

            case STRING:
                if (c == quoteChar) {
                    parseState = prevParseState;
                }
                break;

            case EXPRESSION:
                if (exprType == EXPR_MAYBE && c != '{') {
                    // not a valid expression
                    if (c == '<') {
                        //reset to process tag correctly
                        curr--;
                    }
                    parseState = PARSE_STATE.OUTSIDE;
                } else if (c == '}') {
                    parseState = PARSE_STATE.OUTSIDE;
                }
                exprType = EXPR_NONE;
                break;
            default:
                break;
            }
        }
        if (start < end) {
            if (tagType == TT_NONE && parseState != PARSE_STATE.COMMENT) {
                documentHandler.onCharacters(buf, start, end - start);
            } else {
                buffer.write(buf, start, end - start);
            }
        }
    }

    /**
     * Clears the internal tagname buffer and cache
     */
    private void resetTagName() {
        tagName = null;
        tagNameBuffer.reset();
    }

    /**
     * Returns the tagname scanned and resets the internal tagname buffer
     *
     * @return tagname
     */
    private String getTagName() {
        if (tagName == null) {
            tagName = tagNameBuffer.toString();
        }
        return tagName;
    }

    /**
     * Flush internal buffer. This forces the parser to flush the characters
     * still held in its internal buffer, if the parsing state allows.
     */
    private void flushBuffer() throws IOException {
        if (buffer.size() > 0) {
            final char[] chars = buffer.toCharArray();
            documentHandler.onCharacters(chars, 0, chars.length);
            buffer.reset();
        }
    }

    /**
     * Process a comment from current and accumulated character data
     *
     * @param ch character data work buffer
     * @param off start offset for current data
     * @param len length of current data
     * @throws IOException
     */
    private void processComment(char[] ch, int off, int len) throws IOException {
        buffer.write(ch, off, len);
        documentHandler.onComment(buffer.toString());
        buffer.reset();
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
    private boolean processTag(char[] ch, int off, int len) throws IOException {
        buffer.write(ch, off, len);

        final char[] snippet = buffer.toCharArray();

        tokenizer.tokenize(snippet, 0, snippet.length);
        if (!tokenizer.endTag()) {
            documentHandler.onStartElement(tokenizer.tagName(), tokenizer
                    .attributes(), tokenizer
                    .endSlash());
        } else {
            documentHandler.onEndElement(tokenizer.tagName());
        }

        buffer.reset();
        return "SCRIPT".equalsIgnoreCase(tokenizer.tagName()) && !tokenizer.endSlash();
    }
}
