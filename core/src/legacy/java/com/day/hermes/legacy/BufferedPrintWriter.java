/*
 * $Id: BufferedPrintWriter.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.legacy;

import com.day.hermes.logging.FmtLogger;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * The <code>BufferedPrintWriter</code> implements buffering for the
 * <code>PrintWriter</code> returned by the
 * {@link DeliveryHttpServletResponse#getWriter} method.
 * <p>
 * We need this additional buffering class for the
 * {@link DeliveryHttpServletResponseImpl} class because we wrap the original
 * <code>PrintWriter</code> retrieved from the servlet container with optional
 * caching and link checking writers.
 * <p>
 * This class overwrites all the base class's write methods, which are used as
 * the base for writing. All other methods of the base class use the write
 * methods after having formatted the parameter.
 * <p>
 * The buffer is flushed to the wrapped writer in the following cases :
 * <ol>
 * <li>The buffer is full if more characters have to be added. That is the
 *      buffer is not flushed when the buffer gets full by adding characters but
 *      when the buffer would get 'overfull' with adding characters.
 * <li>When the {@link #flush} method is called.
 * <li>When the writer is closed
 * </ol>
 * <p>
 * This class is not multithread safe as it is intended to be used on single
 * requests which are assigned to single threads.
 * <p>
 * This extension to the <code>PrintWriter</code> class does not support
 * automatic flush of the output buffer. That is, the <code>println</code>
 * method never ever flushes output.
 *
 * @version $Revision: 1.5 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since echidna
 * @audience core
 */
class BufferedPrintWriter extends PrintWriter implements OutputStack.BufferedOutput {

    /** default logger */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(BufferedPrintWriter.class);

    /** The line separator */
    private static final String lineSeparator =
            System.getProperty("line.separator");

    /** Reference to the servlet container's writer */
    protected final PrintWriter servletWriter;

    /**
     * The current buffer size which may be modified and retrieved.
     * @see #setBufferSize
     * @see #getBufferSize
     */
    private int bufferSize;

    /** The character buffer */
    private char[] buffer;

    /** The current offset of writing characters into the buffer */
    private int offset;

    /**
     * @see #checkError
     * @see #setError
     */
    protected boolean trouble;

    /** flag to indicate that the writer has been closed */
    protected boolean closed;

    /**
     * Creates an instance wrapping the <code>PrintWriter</code> and providing
     * an initial buffer size.
     * <p>
     * The <code>servletWriter</code> is to initially write data to. This
     * destination may be replaced with the {@link #setDestination}. It is
     * intended that any <code>Writer</code> set in the {@link #setDestination}
     * method be ultimately writing to this same <code>servletWriter</code>.
     * <p>
     * The other use of the <code>servletWriter</code> is to check for any
     * errors occurred by forwarding the call to the {@link #checkError} to this
     * writer.
     *
     * @param servletWriter The <code>PrintWriter</code> to which data is
     *      initially written.
     * @param bufferSize The initial size of the buffer. If negative or zero
     *      no buffering is initiallly done.
     */
    BufferedPrintWriter(PrintWriter servletWriter, int bufferSize) {
        super(servletWriter, false);

        // set the buffer
        this.offset = 0;
        setBufferSize(bufferSize);

        // keep the servlet Writer
        this.servletWriter = servletWriter;
    }

    /**
     * Creates a <code>BufferedWriter</code> for the given response instance.
     * This instance uses the <code>PrintWriter</code> retrieved from the
     * response object to write the data flushing out of the buffer.
     * <p>
     * The <code>PrintWriter</code> from the response is to initially write
     * data to. This destination may be replaced with the
     * {@link #setDestination}. It is intended that any <code>Writer</code> set
     * in the {@link #setDestination} method be ultimately writing to this
     * same servlet container <code>PrintWriter</code>.
     * <p>
     * The other use of the servlet container's <code>PrintWriter</code> is to
     * check for any errors occurred by forwarding the call to the
     * {@link #checkError} to this writer.
     *
     * @param response The response object for which to return a
     *      <code>BufferedPrintWriter</code>. This <em>MUST</em> be the servlet
     *      containers response object otherwise a stack overflow will occur.
     *
     * @throws IOException If a problem occurrs getting the writer from
     *      the response
     * @throws IllegalStateException If an <code>OutputStream</code> has already
     *      been obtained from the response and the <code>PrintWriter</code> is
     *      not available any more.
     * @throws StackOverflowError if the <code>response</code> parameter is not
     *      the servlet container's response object.
     */
    BufferedPrintWriter(ServletResponse response) throws IOException {
        this(response.getWriter(), response.getBufferSize());
    }

    /**
     * Returns the <code>Writer</code> to which the buffer currently is written.
     * @return the <code>Writer</code> to which the buffer currently is written.
     * @see #setDestination
     */
    public Object getDestination() {
        return out;
    }

    /**
     * Redirects the <code>BufferedPrintWriter</code> to the indicated writer
     * after flushing the current contents of the buffer to the former
     * destination.
     * <p>
     * If the new destination is <code>null</code>, this writer is closed as
     * if the {@link #close} method would be called. Subsequent calls to this
     * method have no effect anymore and this instance cannot be used anymore.
     * <p>
     * If calling this method after closing this instance, that is after calling
     * the {@link #close} method or after calling this method with
     * <code>null</code> <code>Writer</code> has no effect other than setting
     * the error flag.
     *
     * @param writer The new <code>Writer</code> to which the buffer contents
     *      will be flushed.
     *
     * @see #getDestination
     */
    void setDestination(Writer writer) {
        if (isClosed()) {
            log.info("setDestination: PrintWriter already closed. No Redirection");
            setError();
            return;
        }

        if (writer == null) {
            log.info("setDestination: Redirecting to null, closing down");
            close();
        } else {
            log.debug("setDestination: Flushing buffer to {1} and redirectin" +
                    " to {0}", out, writer);

            // flush the current contents of the buffer to the current destination
            flushBuffer();

            // replace the destination writer
            this.out = writer;
        }
    }

    /**
     * Redirects this <code>BufferedPrintWriter</code> to the given destination.
     * If the destination is <code>null</code> or is an instance of the
     * <code>Writer</code> class, the same happens as if the
     * {@link #setDestination(Writer)} would be called. Otherwise this method
     * has no effect at all.
     *
     * @param destination The destination to which the output of the
     *      <code>BufferedPrintWriter</code> should directed.
     *
     * @return <code>false</code> if the destination is neither <code>null</code>
     *      nor a <code>Writer</code> instance.
     *
     * @see #setDestination(Writer)
     */
    public boolean setDestination(Object destination) {
        if (destination == null || destination instanceof Writer) {
            setDestination((Writer) destination);
            return true;
        } else {
            return false;
        }
    }

    //---------- buffer handling -----------------------------------------------

    /**
     * Sets the buffer size. If the buffer contains data, this method throws
     * an IllegalStateException.
     *
     * @param bufferSize The new size of the buffer. If this value is less than
     *      or equal to zero, buffering is disabled alltogether.
     *
     * @throws IllegalStateException if the buffer contains data.
     */
    public void setBufferSize(int bufferSize) {

        // check buffer requirements
        if (offset != 0) {
            throw new IllegalStateException("Buffer not empty");
        }

        // only resize buffer if positive value
        if (bufferSize > 0) {
            log.debug("setBufferSize: Creating Buffer of {0} characters",
                    String.valueOf(bufferSize));
            this.bufferSize = bufferSize;
            this.buffer = new char[bufferSize];
        } else {
            log.debug("setBufferSize: Disabling Buffering");
            this.bufferSize = -1;
            this.buffer = null;
        }
    }

    /**
     * Returns the size of the buffer
     * @return the size of the buffer
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Clears the buffer.
     */
    public void resetBuffer() {
        // Simply reset the write offset to the beginning of the buffer
        log.debug("resetBuffer");
        this.offset = 0;
    }

    /**
     * Writes the contents of the buffer to the wrapped writer.
     */
    public void flushBuffer() {
        if (isClosed()) {
            log.info("flush: PrintWriter already closed. No Flushing");
            setError();
            return;
        }

        // write the buffer
        if (buffer != null) {
            if (offset > 0) {
                log.debug("flush: Flushing {0} characters", String.valueOf(offset));
                super.write(buffer, 0, offset);
            } else {
                log.debug("flush: Empty buffer");
            }
        } else {
            log.debug("write: No buffer to flush due to disabled buffering");
        }

        // reset buffer offset to start writing at the beginning
        offset = 0;
    }

    //---------- PrintWriter overwrites ----------------------------------------

    /**
     * Flush the stream if it's not closed and check its error state.
     * Errors are cumulative; once the stream encounters an error, this
     * routine will return true on all successive calls.
     * <p>
     * This implementation checks with the <code>PrintWriter</code> used to
     * create this <code>BufferedPrintWriter</code>.
     *
     * @return <code>true</code> if the print stream has encountered an error,
     *      either on the underlying output stream or during a format conversion.
     */
    public boolean checkError() {
        /**
         * Three steps of checking :
         * - first check whether this is already marked as trouble some
         * - next flush all buffers and check whether an error occurred
         * - next check with the servlet writer whether this one had troubles
         *  ( the servlet writer has to be checked, because this one eats
         *    all io exceptions trying to write to the client )
         */
        return trouble || super.checkError() || servletWriter.checkError();
    }

    /** Indicate that an error has occurred. */
    protected void setError() {
        this.trouble = true;
        super.setError();
    }

    /**
     * Writes the contents of the buffer to the wrapped writer and flushes the
     * latter.
     */
    public void flush() {
        // flush the buffer to the destination
        flushBuffer();

        // flush the destination if not closed already
        if (!isClosed()) {
            super.flush();
        }
    }

    /**
     * Closes the output after flushing the buffer contents to the wrapped
     * writer.
     */
    public void close() {
        if (!isClosed()) {

            // only write buffer, leave to wrapped to flush upon close
            flushBuffer();

            // close the writer and release wrapped writer
            super.close();

            // remove the buffer space alltogether
            setBufferSize(0);

            // set the destination to the servlet writer to prevent possible NPE
            closed = true;
            out = servletWriter;
        }
    }

    /**
     * Writes a single character. If the buffer is already full, it is flushed
     * to the wrapped writer before writing the character to the buffer.
     *
     * @param c The character to write to the buffer.
     */
    public void write(int c) {
        if (isClosed()) {
            log.info("write: PrintWriter already closed. No Writing");
            setError();
            return;
        }

        if (buffer == null) {

            // checks for open stream itself
            log.info("write: Direct writing due to disabled buffering");
            super.write(c);

        } else {
            if (offset >= bufferSize) {
                log.debug("write: Buffer full, flushing first");
                flushBuffer();
            }

            buffer[offset++] = (char) c;
        }
    }

    /**
     * Writes a portion of an array of characters.
     * <p>
     * If the number of characters is more than the buffer capacity, the buffer
     * is flushed. Due to buffering it is not guaranteed that the complete
     * number of characters is either buffered or written. It is acceptable for
     * part of the characters to be written and the rest to be buffered.
     *
     * @param buf The characters to write
     * @param off The starting offset into the character array
     * @param len The number of characters to write starting with the offset.
     *
     * @throws NullPointerException if buf is <code>null</code>
     * @throws IndexOutOfBoundsException if either or both <code>off</code>
     *      and <code>off+len</code> are outside of the bounds of the character
     *      array <code>buf</code>.
     */
    public void write(char buf[], int off, int len) {
        if (isClosed()) {
            log.info("write: PrintWriter already closed. No Writing");
            setError();
            return;
        }

        if (buffer == null) {
            // checks for open stream itself
            log.info("write: Direct writing due to disabled buffering");
            super.write(buf, off, len);
        } else {
            // copy all buffer parts bigger than the current space
            while (offset + len - 1 >= bufferSize) {

                // write the first portion to the buffer to flush
                int space = bufferSize - offset;
                System.arraycopy(buf, off, buffer, offset, space);
                off += space;
                len -= space;

                log.debug("write: {0} characters written, flush buffer",
                        String.valueOf(space));

                // flush buffer and reset offset to 0
                offset = bufferSize;
                flushBuffer();
            }

            // copy rest of the data
            if (len > 0) {
                log.debug("write: Writing {0} characters to the buffer",
                        String.valueOf(len));
                System.arraycopy(buf, off, buffer, offset, len);
                offset += len;
            }
        }
    }

    /**
     * Writes a portion of a string.
     * <p>
     * If the number of characters is more than the buffer capacity, the buffer
     * is flushed. Due to buffering it is not guaranteed that the complete
     * number of characters is either buffered or written. It is acceptable for
     * part of the characters to be written and the rest to be buffered.
     *
     * @param s The string to write
     * @param off The starting offset into the string
     * @param len The number of characters to write starting with the offset.
     *
     * @throws NullPointerException if s is <code>null</code>
     * @throws IndexOutOfBoundsException if either or both <code>off</code>
     *      and <code>off+len</code> are outside of the bounds of the string
     *      <code>s</code>.
     */
    public void write(String s, int off, int len) {

        if (isClosed()) {
            log.info("write: PrintWriter already closed. No Writing");
            setError();
            return;
        }

        /**
         * For performance reasons the code from the write(char[], int, int)
         * method is copied where the System.arraycopy calls are replaced by
         * String.getChars calls. This prevents excessive copying and array
         * allocation which would be needed using the String.toCharArray method.
         */

        if (buffer == null) {
            // checks for open stream itself
            log.info("write: Direct writing due to disabled buffering");
            super.write(s, off, len);
        } else {
            // copy all buffer parts bigger than the current space
            while (offset + len -1 >= bufferSize) {

                // write the first portion to the buffer to flush
                int space = bufferSize - offset;
                s.getChars(off, off+space, buffer, offset);
                off += space;
                len -= space;

                log.debug("write: {0} characters written, flush buffer",
                        String.valueOf(space));

                // flush buffer and reset offset to 0
                offset = bufferSize;
                flushBuffer();
            }

            // copy rest of the data
            if (len > 0) {
                log.debug("write: Writing {0} characters to the buffer",
                        String.valueOf(len));
                s.getChars(off, off+len, buffer, offset);
                offset += len;
            }
        }
    }

    /**
     * Terminate the current line by writing the line separator string without
     * ever flushing the buffer.  The line separator string is defined by the
     * system property <code>line.separator</code>, and is not necessarily a
     * single newline character (<code>'\n'</code>).
     * <p>
     * This duplicates the functionality of the <em>private</em> of the
     * <code>PrintWriter.newLine</code> method.
     */
    public void println() {
        // write the line separator
        write(lineSeparator);
    }

    //---------- internal ------------------------------------------------------

    /**
     * Throws an <code>IOException</code> if the stream is closed. Unforntunately
     * the base class checker method is private, so we have to duplicate code
     * here.
     *
     * @return <code>true</code> if the writer is not closed yet
     */
    private boolean isClosed(){
        return (closed || out == null);
    }
}
