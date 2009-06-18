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
package org.apache.sling.engine.impl.output;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>BufferedServletOutputStream</code> is a wrapper for
 * <code>OutputStream</code> objects, which do not have their own
 * buffering support. The main use of this class is to wrap the output stream
 * retrieved from the cache handler, which does not have buffering but buffering
 * needs to be supported.
 * <p>
 * This class is not multithread safe as it is intended to be used on single
 * requests which are assigned to single threads.
 */
public class BufferedServletOutputStream extends ServletOutputStream implements Buffer {

    /* default logger */
    /** default log */
    private static final Logger log = LoggerFactory.getLogger(BufferedServletOutputStream.class);

    /** The wrapped <code>ServletOutputStream</code> */
    protected OutputStream delegatee;

    /** The size of the buffer in bytes */
    private int bufferSize;

    /** The byte buffer itself */
    private byte[] buffer;

    /** The current offset of writing bytes into the buffer */
    private int offset;

    /** flag to indicate that the stream has been closed */
    protected boolean closed;

    /**
     * Creates an instance wrapping the <code>OutputStream</code> and
     * providing an initial buffer bufferSize.
     *
     * @param delegatee The <code>OutputStream</code> to wrap with buffering
     * @param bufferSize The initial buffer bufferSize in bytes
     */
    public BufferedServletOutputStream(OutputStream delegatee, int bufferSize) {
        this.delegatee = delegatee;
        this.offset = 0;
        this.setBufferSize(bufferSize);
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
        if (this.offset != 0) {
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
        return this.bufferSize;
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
        this.assertOpen();

        // write the buffer
        if (this.buffer != null) {
            if (this.offset > 0) {
                log.debug("flush: Flushing {0} bytes", String.valueOf(this.offset));
                this.delegatee.write(this.buffer, 0, this.offset);
            } else {
                log.debug("flush: Empty buffer");
            }
        } else {
            log.debug("write: No buffer to flush due to disabled buffering");
        }

        this.offset = 0;
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
        this.flushBuffer();
        this.delegatee.flush();
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
        if (!this.closed) {
        	this.flushBuffer();
            this.delegatee.close();

            this.setBufferSize(0);
            this.closed = true;
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
        this.assertOpen();

        if (this.buffer == null) {
            log.debug("write: Direct writing due to disabled buffering");
            this.delegatee.write(b);
        } else {
            if (this.offset >= this.bufferSize) {
                log.debug("write: Buffer full, flushing first");
                this.flushBuffer();
            }
            this.buffer[this.offset++] = (byte) b;
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
        this.assertOpen();

        if (this.buffer == null) {
            log.debug("write: Direct writing due to disabled buffering");
            this.delegatee.write(b, off, len);
        } else {

            // copy all buffer parts bigger than the current space
            while (this.offset + len - 1 >= this.bufferSize) {

                // write the first portion to the buffer to flush
                int space = this.bufferSize - this.offset;
                System.arraycopy(b, off, this.buffer, this.offset, space);
                off += space;
                len -= space;

                log.debug("write: {0} bytes written, flush buffer",
                        String.valueOf(space));

                this.offset = this.bufferSize;
                this.flushBuffer();
            }

            // copy rest of the data
            if (len > 0) {
                log.debug("write: Writing {0} bytes to the buffer",
                        String.valueOf(len));
                System.arraycopy(b, off, this.buffer, this.offset, len);
                this.offset += len;
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
        if (this.closed) {
            throw new IOException("Stream already closed");
        }
    }
}
