/*
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
 */
package org.apache.sling.scripting.javascript.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>EspReader</code> is a <code>FilterReader</code> which takes
 * JSP like input and produces plain ECMA script output. The filtering
 * modifications done on the input comprise the following :
 * <ul>
 * <li>Template text (HTML) is wrapped by out.write(). At most one line of
 * text is wrapped into a single write() call. Double quote characters in the
 * template text (e.g. for HTML tag attribute values) are escaped.
 * <li>ECMA code is written to the output as is.
 * <li>ECMA slash star (/*) comments are also written as is.
 * <li>ECMA slash slash (//) comments are written as is.
 * <li>JSP style template comments (&lt;%-- --&gt;) are also removed from the
 * stream. Lineendings (LFs and CRLFs) are written, though.
 * <li>HTML comments (&lt;!-- --&gt;) are not treated specially. Rather they are
 * handled as plain template text written to the output wrapped in
 * out.write(). The consequence of this behaviour is, that as in JSP ECMA
 * expressions may be included within the comments.
 * </ul>
 * <p>
 * The nice thing about this reader is, that the line numbers of the resulting
 * stream match the line numbers of the matching contents of the input stream.
 * Due to the insertion of write() calls, column numbers will not necessarily
 * match, though. This is especially true if you mix ECMA code tags (&lt;% %&gt;)
 * with template text on the same line.
 * <p>
 * For maximum performance it is advisable to not create the EspReader with a
 * plain FileReader or InputStreamReader but rather with a BufferedReader based
 * on one of the simpler Readers. The reasons for this is, that we call the base
 * reader character by character. This in turn is not too performing if the base
 * reader does not buffer its input.
 */
public class EspReader extends FilterReader {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(EspReader.class);

    /**
     * Default parser state. This is the state the parser starts running in. In
     * this state all text is treated as template text, which should be wrapped
     * by out.write() line by line.
     */
    private static final byte PARSE_STATE_ESP = 1;

    /**
     * ECMA script reading state. When in this state everything upto to the next
     * <code>%&gt;</code> is written to the output verbatim with three
     * exceptions : ECMA slash star comments are handed over to handled by the
     * {@link #PARSE_STATE_ECMA_COMMENT} state, quoted strings are handled in
     * the {@link #PARSE_STATE_QUOTE} state and ECMA slash slash comments are
     * handled in {@link #PARSE_STATE_ECMA_COMMENTL} state.
     */
    private static final byte PARSE_STATE_ECMA = 2;

    /**
     * ECMA script expression reading state. This state works exactly the same
     * as the {@link #PARSE_STATE_ECMA} state with one exception: The whole
     * code enclosed in the <code>&lt;%=</code> ... <code>%&gt;</code> tags
     * is itself wrapped with a <code>out.write()</code> statement
     * verbatim.
     */
    private static final byte PARSE_STATE_ECMA_EXPR = 3;

    /**
     * Compact ESP expression syntax similar to JSP Expression Language notation 
     */
    private static final byte PARSE_STATE_ECMA_EXPR_COMPACT = 4;

    /**
     * JSP comment reading state. When in this state everything upto the closing
     * <code>--&gt;</code> tag is removed from the stream.
     */
    private static final byte PARSE_STATE_JSP_COMMENT = 5;

    /**
     * ECMA quoted string reading state. When in this state everything is
     * written exactly as in the input stream upto the closing quote, which
     * matches the opening quote.
     */
    private static final byte PARSE_STATE_QUOTE = 6;

    /**
     * Verbatim copy state. When in this state as many as verbatimChars
     * characters are returned unchecked. As soon as this number of characters
     * is returned, the last state is popped from the stack. This state is
     * mainly used to (re-)inject static text into the output without further
     * processing.
     */
    private static final byte PARSE_STATE_VERBATIM = 7;

    /**
     * ECMA Comment reading state. When in this state, an ECMA slash star
     * comment is read (and completely returned).
     */
    private static final byte PARSE_STATE_ECMA_COMMENT = 8;

    /**
     * ECMA Comment reading state. When in this state, an ECMA slash slash
     * comment is read (and completely returned).
     */
    private static final byte PARSE_STATE_ECMA_COMMENTL = 9;
    
    /**
     * To work with lookahead and character insertion, we use a PushbackReader.
     */
    private PushbackReader input;

    /**
     * Current parse state. This field contains one of the
     * <code>PARSE_STATE</code> constants.
     */
    private byte state;

    /**
     * Stack of states. Whenever we enter a new state, the old state is pushed
     * onto the stack. When a state is left, the previous one is popped from the
     * stack.
     *
     * @see #pushState(byte)
     * @see #popState()
     * @see #state
     */
    private Stack<Byte> stateStack;

    /**
     * This value is set to true, if the parser is expected to insert a
     * out.write() call into the input stream when in state
     * {@link #PARSE_STATE_ESP}. When this field is true, it is not
     * necessairily the case, that we are at the start of a real text line.
     */
    private boolean lineStart;

    /**
     * If characters are put into the pushback Stream that should be given back
     * verbatim, this value is set to the number of such consecutive characters.
     */
    private int verbatimChars;

    /**
     * During String matching this is the character used for string quoting.
     */
    private char quoteChar;

    /**
     * Set to true if an escape character (\) has been encountered within a
     * quoted string.
     */
    private boolean escape;

    /**
     * Whether the definition of the out variable has already been written or not.
     * The initial value is <code>true</code> indicating it has still to be
     * defined.
     *
     * @see #startWrite(String)
     */
    private boolean outUndefined = true;
    
    /**
     * Javascript statement that sets the "out" variable that's used
     * to output data. Automatically inserted by the reader in code,
     * where needed.
     */
    public static final String DEFAULT_OUT_INIT_STATEMENT = "out=response.writer;"; 
    private String outInitStatement = DEFAULT_OUT_INIT_STATEMENT;

    /**
     * Create an EspReader on top of the given <code>baseReader</code>. The
     * constructor wraps the input reader with a <code>PushbackReader</code>,
     * so that input stream modifications may be handled transparently by our
     * {@link #doRead()} method.
     *
     * @param baseReader the wrapped reader
     */
    public EspReader(Reader baseReader) {
        super(baseReader);
        this.input = new PushbackReader(baseReader, 100);
        this.stateStack = new Stack<Byte>();
        this.lineStart = true;
        this.verbatimChars = -1;
        this.quoteChar = 0;
        this.escape = false;

        // Start in ESP (template text) state
        pushState(PARSE_STATE_ESP);
    }
    
    /**
     * Set the code fragment used to initialize the "out" variable
     *
     * @param statement the statement used for initialization
     */
    public void setOutInitStatement(String statement) {
        outInitStatement = statement;
    }

    /**
     * Check whether we may block at the next read() operation. We may be ready
     * if and only if our input reader is ready. But this does not guarantee
     * that we won't block, as due to filtering there may be more than one
     * character needed from the input to return one.
     *
     * @return <code>true</code> if a character is available on the
     *         <code>PushbackReader</code>.
     * @throws IOException if the reader is not open
     */
    public boolean ready() throws IOException {
        ensureOpen();
        return input.ready();
    }

    /**
     * Return the next filtered character. This need not be the next character
     * of the input stream. It may be a character from the input reader, after
     * having skipped filtered characters or it may be a character injected due
     * to translation of template text to ECMA code.
     *
     * @return the next character after filtering or -1 at the end of the input
     *         reader
     * @throws IOException if the reader is not open
     */
    public int read() throws IOException {
        ensureOpen();
        return doRead();
    }

    /**
     * Fill the given buffer with filtered or injected characters. This need not
     * be the next characters of the input stream. It may be characters from the
     * input reader, after having skipped filtered characters or it may be a
     * characters injected due to translation of template text to ECMA code.
     * This method is exactly the same as
     * <code>read(cbuf, 0, cbuf.length)</code>.
     *
     * @param cbuf The character buffer to fill with (filtered) characters
     * @return the number of characters filled in the buffer or -1 at the end of
     *         the input reader.
     * @throws IOException if the reader is not open
     */
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    /**
     * Fill the buffer from the offset with the number of characters given. This
     * need not be the next characters of the input stream. It may be characters
     * from the input reader, after having skipped filtered characters or it may
     * be a characters injected due to translation of template text to ECMA
     * code.
     *
     * @param cbuf The character buffer to fill with (filtered) characters
     * @param off Offset from where to start in the buffer
     * @param len The number of characters to fill into the buffer
     * @return the number of characters filled in the buffer or -1 at the end of
     *         the input reader.
     * @throws IOException if the reader is not open
     * @throws IndexOutOfBoundsException if len is negative, off is negative or
     *             higher than the buffer length or off+len is negative or
     *             beyond the buffer size.
     */
    public int read(char[] cbuf, int off, int len) throws java.io.IOException {
        ensureOpen();

        // Check lines (taken from InputStreamReader ;-)
        if ((off < 0) || (off > cbuf.length) || (len < 0)
            || ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int i;
        for (i = 0; i < len; i++, off++) {
            int c = doRead();
            if (c < 0) {
                break;
            }
            cbuf[off] = (char) c;
        }

        // return EOF (-1) if none have been read, else return the number read
        return (i == 0) ? -1 : i;
    }

    /**
     * Skip the number of filtered characters. The skip method is the same as
     * calling read() repeatedly for the given number of characters and throwing
     * away the result. If the end of input reader is reached before having
     * skipped the number of characters, the method returns the number
     * characters skipped so far.
     *
     * @param n the number of (filtered) characters to skip
     * @return the number of (filtered) characters actually skipped
     * @throws IllegalArgumentException if n is negative
     * @throws IOException if the reading the characters throws
     */
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }

        long i = -1;
        while (++i < n) {
            if (doRead() < 0) {
                break;
            }
        }
        return i;
    }

    /**
     * Close the EspReader.
     */
    public void close() throws java.io.IOException {
        if (input != null) {
            input.close();
            input = null;
        }

        // I dont' know what happens ??
        super.close();
    }

    /**
     * Mark the present position in the stream. The <code>mark</code> for
     * class <code>EspReader</code> always throws an throwable.
     *
     * @param readAheadLimit The number of characters to read ahead
     * @exception IOException Always, since mark is not supported
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported");
    }

    /**
     * Tell whether this stream supports the mark() operation, which it does
     * not.
     *
     * @return false Always, since mark is not supported
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reset the stream. The <code>reset</code> method of
     * <code>EspReader</code> always throws an throwable.
     *
     * @exception IOException Always, since reset is not supported
     */
    public void reset() throws IOException {
        throw new IOException("reset() not supported");
    }

    /**
     * Internal routine doing all the footwork of reading one character at a
     * time from the <code>PushbackReader</code> and acting according to the
     * current state.
     * <p>
     * This filter is implemented using a finite state machine using the states
     * defined above with the <code>PARSE_STATE</code> constants. Each state
     * may do a look ahead in certain situations to decide on further steps.
     * Characters looked ahead may or may not be inserted back into the input
     * stream depending on the concrete state.
     *
     * @return the next character from the input stream according to the current
     *         state or -1 to indicate end of file.
     * @throws IOException if the input <code>PushbackReader</code> throws it
     */
    private int doRead() throws IOException {

        // we return out of the loop, if we find a character passing the filter
        for (;;) {

            // Get a character from the input, which may well have been
            // injected using the unread() method
            int c = input.read();

            // catch EOF
            if (c < 0) {

                // if a template text line is still incomplete, inject
                // proper line ending and continue until this has been returned
                if (!lineStart && state == PARSE_STATE_ESP) {
                    doVerbatim("\");"); // line ending injection
                    lineStart = true; // mark the line having ended
                    continue; // let's start read the injection
                }

                return c; // return the marker, we're done
            }

            // Do the finite state machine
            switch (state) {

                // NOTE :
                // - continue means ignore current character, read next
                // - break means return current character

                // Template text state - text is wrapped in out.write()
                case PARSE_STATE_ESP:
                    if (c == '$') { // might start EL-like ECMA expr
                    	int c2 = input.read();
                    	if (c2 == '{') {
                            // ECMA expression ${ ... }
                            pushState(PARSE_STATE_ECMA_EXPR_COMPACT);
                            startWrite(null);
                            if (!lineStart) {
                                doVerbatim("\");");
                            }
                            continue;
                    	}
                    	 
                    	input.unread(c2);

                    } else  if (c == '<') { // might start ECMA code/expr, ESP comment or JSP comment
                        int c2 = input.read();
                        int c3 = input.read();

                        if (c2 == '%') {
                            // ECMA or JSP comment

                            if (c3 == '=') {

                                // ECMA expression <%= ... %>
                                pushState(PARSE_STATE_ECMA_EXPR);
                                startWrite(null);
                                if (!lineStart) {
                                    doVerbatim("\");");
                                }
                                continue;

                            } else if (c3 == '-') {

                                // (Possible) JSP Comment <%-- ... --%>
                                int c4 = input.read();
                                if (c4 == '-') {
                                    pushState(PARSE_STATE_JSP_COMMENT);
                                    continue;
                                }
                                input.unread(c4);

                            }

                            // We only get here if we are sure about ECMA

                            // ECMA code <% ... %>
                            input.unread(c3);
                            pushState(PARSE_STATE_ECMA);
                            if (!lineStart) {
                                doVerbatim("\");");
                            }
                            continue;

                        }

                        // Nothing special, push back read ahead
                        input.unread(c3);
                        input.unread(c2);

                        // End of template text line
                    } else if (c == '\r' || c == '\n') {
                        String lineEnd; // will be injected

                        // Check for real CRLF
                        if (c == '\r') {
                            int c2 = input.read();
                            if (c2 != '\n') {
                                input.unread(c2);
                                lineEnd = "\\r";
                            } else {
                                lineEnd = "\\r\\n";
                            }
                        } else {
                            lineEnd = "\\n";
                        }

                        // Only write line ending if not empty
                        if (!lineStart) {
                            doVerbatim("\");\n");
                            doVerbatim(lineEnd);
                            lineStart = true;

                        } else { // if (lineEnd.length() > 1) {
                            // no matter what line ending we have, make it LF
                            doVerbatim("\");\n");
                            doVerbatim(lineEnd);
                            startWrite("\"");
                        }

                        continue;

                        // template text is wrapped with double quotes, which
                        // when occurring in the text must be escaped.
                        // We also escape the escape character..
                    } else if (c == '"' || c == '\\') {

                        doVerbatim(String.valueOf((char) c));
                        c = '\\';

                    }

                    // If in template text at the beginning of a line
                    if (lineStart) {
                        lineStart = false;
                        startWrite("\"" + (char) c);
                        continue;
                    }

                    break;

                // Reading ECMA code or and ECMA expression
                case PARSE_STATE_ECMA_EXPR:
                case PARSE_STATE_ECMA:

                    if (c == '%') {

                        // might return to PARSE_STATE_ESP
                        int c2 = input.read();
                        if (c2 == '>') {

                            // An expression is wrapped in out.write()
                            if (popState() == PARSE_STATE_ECMA_EXPR) {
                                doVerbatim(");");
                            }

                            // next ESP needs out.write(
                            lineStart = true;

                            continue;

                        }

                        // false alert, push back
                        input.unread(c2);

                    } else if (c == '/') {

                        // might be ECMA Comment
                        int c2 = input.read();
                        if (c2 == '/') {
                            // single line comment
                            pushState(PARSE_STATE_ECMA_COMMENTL);
                        } else if (c2 == '*') {
                            // multiline comment
                            pushState(PARSE_STATE_ECMA_COMMENT);
                        }

                        // false alert, push back
                        input.unread(c2);

                    } else if (c == '\'' || c == '"') {

                        // an ECMA string
                        escape = false; // start unescaped
                        quoteChar = (char) c; // to recognize the end
                        pushState(PARSE_STATE_QUOTE);

                    }
                    break;

                // reading compact (EL-like) ECMA Expression
                case PARSE_STATE_ECMA_EXPR_COMPACT:
                    if (c == '}') { //might be the end of a compact expression
                        // An expression is wrapped in out.write()
                        popState();
                        doVerbatim(");");

                        // next ESP needs out.write(
                        lineStart = true;

                        continue;

                    }
                    break;

                // Reading a JSP comment, only returning line endings
                case PARSE_STATE_JSP_COMMENT:

                    // JSP comments end complexly with --%>
                    if (c == '-') {
                        int c2 = input.read();
                        if (c2 == '-') {
                            int c3 = input.read();
                            if (c3 == '%') {
                                int c4 = input.read();
                                if (c4 == '>') {

                                    // we really reached the end ...
                                    popState();
                                    continue;

                                }
                                input.unread(c4);
                            }
                            input.unread(c3);
                        }
                        input.unread(c2);

                        // well, not definitely correct but reasonably accurate
                        // ;-)
                    } else if (c == '\r' || c == '\n') {

                        // terminate an open template line
                        if (!lineStart) {
                            input.unread(c); // push back the character
                            doVerbatim("\");"); // insert ");
                            lineStart = true; // mark the line start
                            continue; // Force read of the "
                        }

                        break;
                    }

                    // continue reading another character in the comment
                    continue;

                    // Read an ECMA string upto the ending quote character
                case PARSE_STATE_QUOTE:

                    // if unescaped quote character
                    if (c == quoteChar && !escape) {
                        popState();
                    } else {
                        // mark escape - only if not already escaped (bug 7079)
                        escape = c == '\\' && !escape;
                    }

                    break;

                // Return characters unfiltered
                case PARSE_STATE_VERBATIM:

                    // Go back to previous state if all characters read
                    if (--verbatimChars < 0) {
                        popState();
                    }

                    break;

                // Return an ECMA multiline comment, ending with */
                case PARSE_STATE_ECMA_COMMENT:

                    // Might be the end of the comment
                    if (c == '*') {
                        int c2 = input.read();
                        if (c2 == '/') {
                            popState(); // back to previous
                            doVerbatim("/"); // return slash verbatim
                        } else {
                            input.unread(c2);
                        }
                    }

                    break;

                // Return an ECMA single line comment, ending with end of line
                case PARSE_STATE_ECMA_COMMENTL:

                    // CRLF recognition
                    if (c == '\r') {
                        int c2 = input.read();
                        if (c2 == '\n') {
                            popState();
                        }
                        input.unread(c2);

                        // LF only line end
                    } else if (c == '\n') {
                        popState();
                    }

                    break;
                    
                // What ???!!!
                default:

                    // we warn and go back to default state
                    log.warn("doRead(): unknown state " + state);
                    state = PARSE_STATE_ESP;

                    break;

            } // switch

            // Exiting the switch normally we return the current character
            return c;

        } // for(;;)

    }

    /**
     * Throw an IOException if the reader is not open
     *
     * @throws IOException if the reader is (already) closed
     */
    private void ensureOpen() throws IOException {
        if (input == null) {
            throw new IOException("Reader is closed");
        }
    }

    /**
     * Injects the call to write template text and checks whether the global
     * <em>out</em> variable has also to be defined such that the writer is
     * acquired on demand.
     *
     * @param startString Additional data to be injected as initial argument
     *      to the <em>out.write</em> call written. If <code>null</code> just
     *      the method call is injected.
     *
     * @throws IOException if the 'unreading' throws
     */
    private void startWrite(String startString) throws IOException {

        // inject the out.write( part and the initial string
        if (startString != null && startString.length() > 0) {
            doVerbatim(startString);
        }
        doVerbatim("out.write(");

        // if out is not set yet, we also acquire it now setting it
        // globally
        if (outUndefined) {
            doVerbatim(outInitStatement);
            outUndefined = false;
        }
    }

    /**
     * Injects a string into the input stream, sets the number of characters to
     * return verbatim and change state. The state change only happens if we are
     * not in verbatim state already. Else the current string is simply
     * prepended to the previous inhjection. This is simply a convenience method
     * ;-)
     *
     * @param verbatimString The string to inject into the input stream
     * @throws IOException if the 'unreading' throws
     */
    private void doVerbatim(String verbatimString) throws IOException {

        // Push 'back' into PushbackReader
        input.unread(verbatimString.toCharArray());

        // Set the number of characters to return verbatim
        verbatimChars += verbatimString.length();

        // Change state if not already in verbatim state
        if (state != PARSE_STATE_VERBATIM) {
            pushState(PARSE_STATE_VERBATIM);
        }
    }

    /**
     * Push the current state on stack and set to <code>newState</code>. This
     * new state is also returned.
     *
     * @param newState the new state to set
     * @return the new state set according to <code>newState</code>
     */
    private byte pushState(byte newState) {
        stateStack.push(state);
        return state = newState;
    }

    /**
     * Sets the current state to the state stored at the top of the stack. If
     * the stack is empty prior to this call, the default template text state is
     * set. The method returns the state prior to setting to the new state.
     *
     * @return the state prior to calling this method
     */
    private byte popState() {
        byte oldState = state;
        state = stateStack.isEmpty() ? PARSE_STATE_ESP : stateStack.pop();
        return oldState;
    }

}