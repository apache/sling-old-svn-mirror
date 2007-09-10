/*
 * $Id: BufferedServletOutputStream.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The <code>BufferedServletOutputStream</code> is a wrapper for
 * <code>OutputStream</code> objects, which do not have their own
 * buffering support. The main use of this class is to wrap the output stream
 * retrieved from the cache handler, which does not have buffering but buffering
 * needs to be supported.
 * <p>
 * This class is not multithread safe as it is intended to be used on single
 * requests which are assigned to single threads.
 *
 * @version $Revision: 1.5 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since echidna
 * @audience core
 */
class BufferedServletOutputStream extends ServletOutputStream implements OutputStack.BufferedOutput {

    /* default logger */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(BufferedServletOutputStream.class);

    /** The original servlet container's ServletOutputStream */
    protected final OutputStream servletOutputStream;

    /** The wrapped <code>ServletOutputStream</code> */
    protected OutputStream delegatee;

    /** The size of the buffer in bytes */
    private int bufferSize;

    /** The byte buffer itself */
    private byte[] buffer;

    /** The current offset of writing bytes into the buffer */
    private int offset;

    /**
     * Creates an instance wrapping the <code>OutputStream</code> and
     * providing an initial buffer bufferSize.
     *
     * @param delegatee The <code>OutputStream</code> to wrap with buffering
     * @param bufferSize The initial buffer bufferSize in bytes
     */
    BufferedServletOutputStream(OutputStream delegatee, int bufferSize) {
        this.servletOutputStream = delegatee;
        this.delegatee = delegatee;
        this.offset = 0;
        setBufferSize(bufferSize);
    }

    //---------- buffer handling -----------------------------------------------

    /**
     * Sets the buffer size. If the buffer contains data, this method throws
     * an IllegalStateException.
     *
     * @param bufferSize
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
            this.buffer = new byte[bufferSize];
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
     * CLears the buffer.
     */
    public void resetBuffer() {
        this.offset = 0;
    }

    /**
     * Flushes the output buffer to the underlying <code>ServletOutputStream</code>.
     *
     * @throws IOException If the stream is already closed or if an I/O error
     *      occurrs flushing the buffer.
     */
    public void flushBuffer() throws IOException {
        assertOpen();

        // write the buffer
        if (buffer != null) {
            if (offset > 0) {
                log.debug("flush: Flushing {0} bytes", String.valueOf(offset));
                delegatee.write(buffer, 0, offset);
            } else {
                log.debug("flush: Empty buffer");
            }
        } else {
            log.debug("write: No buffer to flush due to disabled buffering");
        }

        offset = 0;
    }

    /**
     * Returns the <code>OutputStream</code> to which the buffer currently is
     * written.
     * @return the <code>OutputStream</code> to which the buffer currently is
     *      written.
     * @see #setDestination
     */
    public Object getDestination() {
        return delegatee;
    }

    /**
     * Redirects this object to the indicated output stream after flushing the
     * current contents of the buffer to the former destination.
     * <p>
     * If the new destination is <code>null</code>, this output stream is
     * closed as if the {@link #close} method would be called. Subsequent calls
     * to this method have no effect anymore and this instance cannot be used
     * anymore.
     * <p>
     * If calling this method after closing this instance, that is after calling
     * the {@link #close} method or after calling this method with
     * <code>null</code> <code>OutputStream</code> has no effect other than
     * setting the error flag.
     *
     * @param output The new <code>OutputStream</code> to which the buffer
     *      contents will be flushed.
     *
     * @see #getDestination
     */
    void setDestination(OutputStream output) throws IOException {
        assertOpen();

        if (output == null) {
            log.info("setDestination: Redirecting to null, closing down");
            close();

            // set the destination to the servlet output to prevent possible NPE
            delegatee = servletOutputStream;
        } else {
            log.debug("setDestination: Flushing buffer to {1} and redirectin" +
                    " to {0}", delegatee, output);

            // flush the current contents of the buffer to the current destination
            flushBuffer();

            // replace the destination writer
            this.delegatee = output;
        }
    }

    /**
     * Redirects the output of this instance if the destination is an
     * <code>OutputStream</code> or <code>null</code>. Otherwise nothing
     * happens.
     *
     * @param destination The destination to redirect the output to.
     *
     * @return <code>false</code> if the destination is neither <code>null</code>
     *      nor a <code>Writer</code> instance.
     *
     * @throws IOException If switching the output destination results in an
     *      error.
     */
    public boolean setDestination(Object destination) throws IOException {
        if (destination == null || destination instanceof OutputStream) {
            setDestination((OutputStream) destination);
            return true;
        } else {
            return false;
        }
    }

    //---------- ServletOutputStream overwrites --------------------------------

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of <code>flush</code> is
     * that calling it is an indication that, if any bytes previously
     * written have been buffered by the implementation of the output
     * stream, such bytes should immediately be written to their
     * intended destination.
     * <p>
     * The <code>flush</code> method of <code>OutputStream</code> does nothing.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void flush() throws IOException {
        flushBuffer();
        delegatee.flush();
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of <code>close</code>
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     * <p>
     * The <code>close</code> method of <code>OutputStream</code> does nothing.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        if (delegatee != null) {
            flushBuffer();
            delegatee.close();
            delegatee = null;

            // remove the buffer
            setBufferSize(0);
        }
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for <code>write</code> is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument <code>b</code>. The 24
     * high-order bits of <code>b</code> are ignored.
     * <p>
     * Subclasses of <code>OutputStream</code> must provide an
     * implementation for this method.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> may be thrown if the
     *             output stream has been closed.
     */
    public void write(int b) throws IOException {

        // assert stream is not closed
        assertOpen();

        if (buffer == null) {
            log.info("write: Direct writing due to disabled buffering");
            delegatee.write(b);
        } else {
            if (offset >= bufferSize) {
                log.debug("write: Buffer full, flushing first");
                flushBuffer();
            }
            buffer[offset++] = (byte) b;
        }
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     * The general contract for <code>write(b, off, len)</code> is that
     * some of the bytes in the array <code>b</code> are written to the
     * output stream in order; element <code>b[off]</code> is the first
     * byte written and <code>b[off+len-1]</code> is the last byte written
     * by this operation.
     * <p>
     * The <code>write</code> method of <code>OutputStream</code> calls
     * the write method of one argument on each of the bytes to be
     * written out. Subclasses are encouraged to override this method and
     * provide a more efficient implementation.
     * <p>
     * If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs. In particular,
     *             an <code>IOException</code> is thrown if the output
     *             stream is closed.
     */
    public void write(byte b[], int off, int len) throws IOException {
        // assert stream is not closed
        assertOpen();

        if (buffer == null) {
            log.info("write: Direct writing due to disabled buffering");
            delegatee.write(b, off, len);
        } else {

            // copy all buffer parts bigger than the current space
            while (offset + len - 1 >= bufferSize) {

                // write the first portion to the buffer to flush
                int space = bufferSize - offset;
                System.arraycopy(b, off, buffer, offset, space);
                off += space;
                len -= space;

                log.debug("write: {0} bytes written, flush buffer",
                        String.valueOf(space));

                offset = bufferSize;
                flushBuffer();
            }

            // copy rest of the data
            if (len > 0) {
                log.debug("write: Writing {0} bytes to the buffer",
                        String.valueOf(len));
                System.arraycopy(b, off, buffer, offset, len);
                offset += len;
            }
        }
    }

    //---------- internal ------------------------------------------------------

    /**
     * Throws an <code>IOException</code> if the stream is closed.
     *
     * @throws IOException if the stream is already closed.
     */
    private void assertOpen() throws IOException {
        if (delegatee == null) {
            throw new IOException("Stream already closed");
        }
    }
}
