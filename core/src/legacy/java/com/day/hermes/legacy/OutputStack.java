/*
 * $Id: OutputStack.java 22189 2006-09-07 11:47:26Z fmeschbe $
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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Stack;

/**
 * The <code>OutputStack</code> class provides a simple framework to manage
 * the output channels within the Delivery Module.
 * <p>
 * <strong>Problem</strong>
 * <p>
 * The <code>DeliveryHttpServletResponseImpl</code> class wraps the original
 * <code>PrintWriter</code> or <code>ServletOutputStream</code> received from
 * the servlet container with at most two additional <code>PrintWriters</code>/
 * <code>ServletOutputStream</code>, one for caching the result and one for link
 * checking the result stream (only for writers). Each request may have none,
 * either of both or both of these wrappers.
 * <p>
 * If a servlet (plain serlvet, JSP, or even ECMA template) includes another
 * request into the response, each call to <code>RequestDisaptcher.include</code>
 * (actually for forward, too, but this is less relevant) results in a new
 * {@link DeliveryHttpServletResponse} object being created. The
 * {@link DeliveryHttpServletResponse#getWriter getWriter} and
 * {@link DeliveryHttpServletResponse#getOutputStream getOutputStream} methods
 * of these additional response objects get the same servlet container response
 * object and wrap it with new cache handling and/or link checking wrappers.
 * <p>
 * The problems lies in the fine print: the cache handling stream of outer
 * servlets do not get the output from inner servlets, because this output does
 * not pass by the outer servlet's cache handler and link checker. Particularly
 * the former is problematic in that the cache data will be corrupt.
 * <p>
 * Another problem in this wrapping situation - regardless of whether the
 * request is forwarded/included or not - is that checking the error condition
 * on the <code>PrintWriter</code> fails for the wrappers because the original
 * servlet container's <code>PrintWriter</code> grocks all problems and neither
 * the caching nor the link checking writers know about any problems if not
 * actively asking the wrapped <code>PrintWriter</code> which currently the
 * caching wrapper does not do.
 * <p>
 * <strong>Solution</strong>
 * The solution to the problem is to maintain a stack of
 * <code>Writer</code>s or <code>OutputStream</code>s per HTTP request. Note
 * that when using request forwarding and inclusion, a HTTP request may result
 * in multiple <code>service</code> method calls and thus in mutliple
 * {@link DeliveryHttpServletResponse} objects being created. The stack is
 * stored as an attribute of the request to retrieve it for included or
 * forwarded requests.
 * <p>
 * To use this stack, a reference of to the stack is retrieved using the
 * {@link #getOutputStack} method. This method either returns the instance
 * stored as a request attribtue or creates a new instance to be returned also
 * storing this new instance as a request attribute. The name of the attribute
 * is <code>com.day.hermes.output_stack</code>. If an object is found at
 * that name, which is not an <code>OutputStack</code>, the
 * {@link #getOutputStack} method throws an <code>IOException</code>.
 * <p>
 * The thus retrieved output stack instance will then be used to create a writer,
 * push that writer or a writer wrapping the writer returned and finally popping
 * to close the writer (or likewise an output stream). That is the usage
 * pattern for getting a writer or output stream to be used is :
 * <pre>
        // get an instance of the writer stack
        OutputStack stack = OutputStack.getOutputStack(request, response);

        // get the initial writer to be wrapped and pushed
        Writer w = stack.createWriter();

        // ... optionally wrap this writer with further ones ...
        w = new CacheWriter(w);

        // push the writer and get a <code>PrintWriter</code> to use
        PritnWriter printWriter = stack.pushWriter(w);
 * </pre>
 * <p>
 * At the end, when the <code>PrintWriter</code> or
 * <code>ServletOutputStream</code> is no longer used and would generally be
 * closed, the following pattern is applied :
 * <pre>
        // get an instance of the writer stack
        OutputStack stack = OutputStack.getOutputStack(request, response);

        // pop the top writer closing it
        stack.popWriter();
 * </pre>
 * <p>
 * Note that there is at most one output stack per HTTP request and as soon as
 * the last writer has been popped off the stack the writer stack cannot be used
 * to create writers again.
 * <p>
 * This class provides support for both <code>Writer</code> and
 * <code>OutptuStream</code> instances but can only handle one type at a time.
 * The type handled is defined with the first call to a create method. If a
 * subsequent call to one of the create methods tries to retrieve another type
 * an <code>IllegalStateException</code> is thrown.
 *
 * @version $Revision: 1.4 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since fennec
 * @audience core
 */
class OutputStack {

    /* default logger */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(OutputStack.class);

    /** The name of the request attribute storing instances of this class */
    private static final String OUTPUT_STACK_ATTRIBUTE =
            "com.day.hermes.output_stack";

    /**
     * The original servlet response. This must be the servlet response object
     * which is wrapped by the client of the <code>OutputStack</code> class.
     */
    private final ServletResponse response;

    /**
     * The output channel currently on top of the stack. This field is
     * <code>null</code> until after the {@link #pushWriter} or
     * {@link #pushOutputStream} method is called the first time and
     * will revert to <code>null</code> as soon as the {@link #popWriter} or
     * {@link #popOutputStream} method is called the same number of times as the
     * corresponding {@link #pushWriter}/{@link #pushOutputStream} has been
     * called.
     */
    private Object top;

    /**
     * The {@link BufferedOutput} is created on first call to
     * {@link #createWriter} or {@link #createOutputStream} initially writing
     * to the servlet container's response channel.
     * <p>
     * Whenever one of the push methods is called, the destination of this
     * buffered output is replaced with the output channel pushed. Whenever one
     * of the pop methods is called, the destination of this buffered output is
     * replaced with the output channel popped from the stack. If the pop method
     * has been called the same number of times as the push method, the
     * buffered output is automatically closed because it would be redirected
     * to <code>null</code> (see {@link BufferedPrintWriter#setDestination} for
     * details).
     *
     * @see #pushOutput
     * @see #pushOutputStream
     * @see #pushWriter
     * @see #popOutput
     * @see #popOutputStream
     * @see #popWriter
     */
    private BufferedOutput buffer;

    /**
     * The stack of output channels, which might be <code>null</code> if only
     * one (namely the servlet container's) output channel is used.
     */
    private Stack outputStack;

    /**
     * Creates a new <code>OutputStack</code> ultimately writing to the output
     * channel of the given response object.
     *
     * @param response The <code>ServletResponse</code> object representing the
     *      response to the current servlet request. This <em>MUST</em> be the
     *      servlet containers response object otherwise a stack overflow
     *      will occur when calling one of the create methods the first time.
     *
     * @see #createOutputStream
     * @see #createWriter
     */
    private OutputStack(ServletResponse response) {
        this.response = response;
    }

    /**
     * Returns the <code>OutputStack</code> instance for the request. If such a
     * stack has already been created for the request it is retrieved from the
     * request else a new <code>OutputStack</code> instance is created and
     * stored as a request attribute.
     *
     * @param request The <code>ServletRequest</code> object representing the
     *      current servlet request.
     * @param response The <code>ServletResponse</code> object representing the
     *      response to the current servlet request. This <em>MUST</em> be the
     *      servlet containers response object otherwise a stack overflow
     *      will occur.
     *
     * @return The <code>OutputStack</code> output for the servlet request.
     *
     * @throws IOException if the object stored under the name used for the
     *      <code>OutputStack</code> is not an <code>OutputStack</code> object.
     */
    static OutputStack getOutputStack(ServletRequest request,
            ServletResponse response) throws IOException {

        // ok, might theoretically be somehting else !!!
        Object stackObject = request.getAttribute(OUTPUT_STACK_ATTRIBUTE);
        if (stackObject == null) {
            // create a new OutputStack
            OutputStack stack = new OutputStack(response);
            request.setAttribute(OUTPUT_STACK_ATTRIBUTE, stack);
            return stack;
        } else if (!(stackObject instanceof OutputStack)) {
            throw new IOException(OUTPUT_STACK_ATTRIBUTE + " attribute not an OutputStack");
        } else {
            // return the OutputStack
            return (OutputStack) stackObject;
        }

    }

    //---------- Writer support ------------------------------------------------

    /**
     * Creates a new <code>Writer</code> onto which caching and link checking
     * writers may be plugged.
     * <p>
     * If this method is called the first the time, the servlet container's
     * writer is returned as if retrieved using the container's response
     * object's <code>getWriter</code> method. For each further call to this
     * method (but after a call to {@link #pushWriter}) a <code>Writer</code>
     * is returned, which wraps the current top of the stack <code>Writer</code>
     * but does not forward the close operation to that <code>Writer</code>.
     * <p>
     * The <code>Writer</code> returned should neither be closed nor should it
     * be directly used. Rather it is intended that this writer is wrapped
     * with yet other writers if so desired and ultimately push the resulting
     * <code>Writer</code> (which may be the <code>Writer</code> returned by
     * this method) onto the stack using {@link #pushWriter} and using the
     * <code>PrintWriter</code> so received.
     * <p>
     * To close the <code>Writer</code> returned and pushed simply pop the stack
     * using {@link #popWriter}.
     *
     * @return The <code>PrintWriter</code> to be used for the response data.
     *
     * @throws IOException if getting the writer from the original servlet
     *      response fails.
     * @throws IllegalStateException if this <code>OutputStack</code> does not
     *      provide <code>Writer</code>s but <code>OutputStream</code>s.
     * @throws StackOverflowError if this <code>OutputStack</code> has not
     *      been created with the servlet container's resposne object.
     */
    public Writer createWriter() throws IOException {
        if (top == null) {
            // create the buffer for the real first call
            if (buffer == null) {
                // create a new buffer for writers
                buffer = new BufferedPrintWriter(response);
            } else if (!(buffer.getDestination() instanceof Writer)) {
                // check destination of the current buffered output
                throw new IllegalStateException("OutputStream already obtained");
            }

            // if repeated call without ever pushing, just return the buffers
            // destination
            return (Writer) buffer.getDestination();

        } else if (top instanceof Writer) {
            // wrap the top with a BasePrintWriter to prevent sifting the
            // close operation all the way down
            return new BaseWriter((Writer) top);

        } else {
            throw new IllegalStateException("OutputStream already obtained");
        }
    }

    /**
     * Registers the <code>Writer</code> as the new writer on the top of the
     * stack and returns a <code>PrintWriter</code> writing to the registered
     * writer.
     * <p>
     * Neither the registered <code>Writer</code> nor the
     * <code>PrintWriter</code> returned should directly be closed. Instead
     * {@link #popWriter} should be called to remove the registered writer
     * from the stack and close it.
     * <p>
     * It is assumed but not forced that this writer bases on a chain of
     * <code>Writers</code> ultimately based on a <code>Writer</code> retrieved
     * with {@link #createWriter}.
     *
     * @param toRegister The <code>Writer</code> to register on the top of the
     *      stack and wrap with a <code>PrintWriter</code>.
     *
     * @return The buffering <code>PrintWriter</code> writing to the given
     *      <code>Writer</code> just registered.
     */
    public PrintWriter pushWriter(Writer toRegister) {
        // return the buffer to be used
        try {
            return (PrintWriter) pushOutput(toRegister);
        } catch (IOException ioe) {
            // this is unlikely for the PrintWriter case, log anyhow
            log.mark("pushWriter: Unexpected error occurred:", ioe);
            log.mark("pushWriter: Please file an incident report. Thanks for your support");

            // return a simple dummy wrapper to be able to continue working
            return new PrintWriter(toRegister);
        }
    }

    /**
     * Closes the <code>Writer</code> on the top of the stack and sets the next
     * <code>Writer</code> on the stack as the "new" top <code>Writer</code>.
     * If all writers ever registered with the {@link #pushWriter} method have
     * been closed further calls to this method have no effect.
     * <p>
     * To close all writers on the stack, you can use the return value of this
     * method to decide on whether more should be closed, for example :
     * <blockquote>while (outputStack.popWriter());</blockquote>
     *
     * @return <code>true</code> if there was a <code>Writer</code> to close.
     */
    public boolean popWriter() {
        return popOutput();
    }

    //---------- OutputStream support ------------------------------------------

    /**
     * Creates a new <code>OutputStream</code> onto which caching output streams
     * may be plugged.
     * <p>
     * If this method is called the first the time, the servlet container's
     * output stream is returned as if retrieved using the container's response
     * object's <code>getOutputStream</code> method. For each further call to
     * this method (but after a call to {@link #pushOutputStream}) an
     * <code>OutputStream</code> is returned, which wraps the current top of
     * the stack <code>OutputStream</code> but does not forward the close
     * operation to that <code>OutputStream</code>.
     * <p>
     * The <code>OuptutStream</code> returned should neither be closed nor
     * should it be directly used. Rather it is intended that this output stream
     * is wrapped with yet other output streams if so desired and ultimately
     * push the resulting <code>OutputStream</code> (which may be the
     * <code>OutputStream</code> returned by this method) onto the stack using
     * {@link #pushOutputStream} and using the <code>ServletOutputStream</code>
     * so received.
     * <p>
     * To close the <code>OutputStream</code> returned and pushed simply pop
     * the stack using {@link #popOutputStream}.
     *
     * @return The <code>OutputStream</code> to be used for the response data.
     *
     * @throws IOException if getting the writer from the original servlet
     *      response fails.
     * @throws IllegalStateException if this <code>OutputStack</code> does not
     *      provide <code>OutputStream</code>s but <code>Writer</code>s.
     * @throws StackOverflowError if this <code>OutputStack</code> has not
     *      been created with the servlet container's resposne object.
     */
    public OutputStream createOutputStream() throws IOException {
        if (top == null) {
            // create the buffer for the real first call
            if (buffer == null) {
                buffer = new BufferedServletOutputStream(response.getOutputStream(),
                        response.getBufferSize());
            } else if (!(buffer.getDestination() instanceof OutputStream)) {
                // check destination of the current buffered output
                throw new IllegalStateException("Writer already obtained");
            }

            // if repeated call without ever pushing, just return the buffers
            // destination
            return (OutputStream) buffer.getDestination();

        } else if (top instanceof OutputStream) {
            // wrap the top with a BasePrintWriter to prevent sifting the
            // close operation all the way down
            return new BaseOutputStream((OutputStream) top);

        } else {
            throw new IllegalStateException("Writer already obtained");
        }

    }

    /**
     * Registers the <code>OutputStream</code> as the new writer on the top of
     * the stack and returns a <code>ServletOutputStream</code> writing to the
     * registered output stream.
     * <p>
     * Neither the registered <code>OutputStream</code> nor the
     * <code>ServletOutputStream</code> returned should directly be closed.
     * Instead {@link #popOutputStream} should be called to remove the
     * registered writer from the stack and close it.
     * <p>
     * It is assumed but not forced that this writer bases on a chain of
     * <code>OutputStream</code>s ultimately based on a
     * <code>OutputStream</code> retrieved with {@link #createOutputStream}.
     *
     * @param toRegister The <code>OuptutStream</code> to register on the top
     *      of the stack and wrap with a <code>ServletOutputStream</code>.
     *
     * @return The buffering <code>ServletOutputStream</code> writing to the
     *      given <code>OutputStream</code> just registered.
     */
    public ServletOutputStream pushOutputStream(OutputStream toRegister)
            throws IOException {
        // return the buffer to be used
        return (ServletOutputStream) pushOutput(toRegister);
    }

    /**
     * Closes the <code>OutputStream</code> on the top of the stack and sets
     * the next <code>OutputStream</code> on the stack as the "new" top
     * <code>OutputStream</code>. If all writers ever registered with the
     * {@link #pushOutputStream} method have been closed further calls
     * to this method have no effect.
     * <p>
     * To close all output streams on the stack, you can use the return value
     * of this method to decide on whether more should be closed, for example :
     * <blockquote>while (outputStack.popOutputStream());</blockquote>
     *
     * @return <code>true</code> if there was a <code>Writer</code> to close.
     */
    public boolean popOutputStream() throws IOException {
        return popOutput();
    }

    //---------- stack implementation ------------------------------------------

    /**
     * Registers the output channel as the new top of the stack wrapping with
     * the buffering output channel.
     *
     * @param toRegister The output channel to be registered, which must be
     *      compatible with the buffering output channel in use by this
     *      <code>OutputStack</code> instance.
     *
     * @return The buffering output channel
     *
     * @throws IOException If wrapping with the output channel fails.
     */
    public Object pushOutput(Object toRegister) throws IOException {

        // try to redirect the destination first
        if (!buffer.setDestination(toRegister)) {
            throw new IOException("Output " + toRegister + " not compatible " +
                    "with current buffering channel");
        }

        // after successfully setting the new destination it is safe to
        // organize the stack now for the new top position.

        // place the old top writer to the stack (this may be null on first call)
        if (outputStack == null) {
            outputStack = new Stack();
        }
        outputStack.push(top);

        // set new writer as the top and redirect the buffer to that one
        top = toRegister;

        // return the buffer to be used
        return buffer;
    }

    /**
     * Close the output channel at the top of stack and popping it off the stack.
     *
     * @return <code>true</code> if there was an output channel at the top,
     *      which could be popped. Otherwise <code>false</code> is returned if
     *      the stack is empty.
     */
    public boolean popOutput() {
        // ignore if pushWriter has never been called or all writers have been popped.
        if (top != null) {

            // make sure the buffer is pumped out to the top writer
            // before closing that one
            try {
                buffer.flushBuffer();
            } catch (IOException ioe) {
                log.warn("popOutput: Problem flusing the output: {0}",
                        ioe.getMessage());
                log.debug("dump:", ioe);
            }

            // close the top output channel
            try {
                if (top instanceof OutputStream) {
                    ((OutputStream) top).close();
                } else if (top instanceof Writer) {
                    ((Writer) top).close();
                }
            } catch (IOException ioe) {
                log.warn("popOutput: Problem closing the output: {0}",
                    ioe.getMessage());
                log.debug("dump:", ioe);
            }

            // replace top with top element from stack (may be <code>null</code>)
            if (outputStack != null && !outputStack.isEmpty()) {
                // pop the next writer to the top
                top = outputStack.pop();
            } else {
                // this was the last writer
                top = null;
            }

            // redirect the buffer to the new top, if <code>null</code> closes
            try {
                buffer.setDestination(top);
            } catch (IOException ignore) {
                // because buffer is empty anyway
            }

            return true;
        } else {
            return false;
        }
    }

    //---------- Buffer handling -----------------------------------------------

    /**
     * Returns the size of the buffer of the currently active buffering output
     * channel or 0 if no buffering channel has yet been created, which is the
     * case when no push method has yet been called on this object.
     *
     * @return The size of the buffering output channel or 0 if no buffering
     *      output channel has yet been defined or a negative value if buffering
     *      is not enabled on the buffering output channel.
     */
    public int getBufferSize() {
        return (buffer != null) ? buffer.getBufferSize()  : 0;
    }

    /**
     * Sets the size of the buffering output channel. This method has no effect
     * if no buffering output channel has yet been set, that is if no push
     * method has yet been called.
     *
     * @param bufferSize The new size of the buffer or 0 (or a negative value)
     *      if buffering is to be disabled on the buffering output channel.
     */
    public void setBufferSize(int bufferSize) {
        if (buffer != null) {
            buffer.setBufferSize(bufferSize);
        }
    }

    /**
     * Resets the buffer of the burrering output channel. This method has no
     * effect if no buffering output channel has yet been set, that is if no
     * push method has yet been called.
     *
     * @throws IllegalStateException if the implementation is not willing to
     *      clear the buffer.
     */
    public void resetBuffer() {
        if (buffer != null) {
            buffer.resetBuffer();
        }
    }

    /**
     * Flushes the buffer if any buffering is used and the buffer is available.
     *
     * @throws IOException if flushing the buffer to the client results in an
     *      error.
     */
    public void flushBuffer() throws IOException {
        if (buffer != null) {
            buffer.flushBuffer();
        }
    }

    //---------- inner interface -----------------------------------------------

    /**
     * The <code>BufferedOutput</code> interface defines an API, which will be
     * implemented by buffering output channel implementations to facilitate
     * stack management regardless of whether the channel is stream or writer
     * based.
     *
     * @version $Revision: 1.4 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
     * @author fmeschbe
     * @since fennec
     * @audience core
     */
    static interface BufferedOutput {

        /**
         * Switches the destination of this to the new destination. If the
         * type of destination is not compatible with the implementation the
         * implementation must ignore the call to this method and not do
         * anything but return <code>false</code>.
         * <p>
         * Using the value <code>null</code> as the parameter is not prohibited
         * by this interface. Implementations should specify how the react to
         * getting <code>null</code> as the new destination.
         *
         * @param destination The new destination, which must be compatible with
         *      the expected type of implementation.
         *
         * @return <code>true</code> if setting the destination of this instance
         *      succeeds. If the destination is not compatible with this
         *      buffering output <code>false</code> is returned.
         *
         * @throws IOException may be thrown if switching the destination fails.
         */
        public boolean setDestination(Object destination) throws IOException;

        /**
         * Returns the currently set destination of this
         * <code>BufferedOutput</code>. This may be <code>null</code> in case
         * the <code>BufferedOutput</code> object has already been closed.
         *
         * @return the destination
         */
        public Object getDestination();

        /**
         * Sets the new size of the buffer.
         *
         * @param buffersize The new size of the buffer. The interpretation of
         *      negative or zero values is up to the implementation.
         *
         * @throws IllegalStateException may be thrown if the implementation may
         *      not currently change the size of the buffer.
         */
        public void setBufferSize(int buffersize);

        /**
         * Returns the current size of the buffer. If the implementation is
         * not currently buffering or does not support buffering at a negative
         * number must be returned.
         *
         * @return The current size of the buffer or a negative number if
         *      buffering is disabled or not supported by the implementation.
         */
        public int getBufferSize();

        /**
         * Flushes the current contents of the buffer to the output destination
         * without forcing the destination to flush its contents.
         *
         * @throws IOException May be thrown if an error occurrs flushing the
         *      contents of the buffer.
         */
        public void flushBuffer() throws IOException;

        /**
         * Removes the contents of the buffer.
         *
         * @throws IllegalStateException may be thrown if the implementation
         *      is not willing to clear the buffer.
         */
        public void resetBuffer();
    }

    //---------- inner class ---------------------------------------------------

    /**
     * The <code>BaseWriter</code> class is a <code>FilterWriter</code> whose
     * {@link #close} method only flushes the output but does not really call
     * <code>closes</code> on the next output writer.
     * <p>
     * If an instance of this class is closed through the {@link #close} method,
     * further calls to any of the <code>write</code> methods or to the
     * {@link #flush} method will have no effect.
     *
     * @version $Revision: 1.4 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
     * @author fmeschbe
     * @since fennec
     * @audience core
     */
    private static class BaseWriter extends Writer {

        protected Writer out;

        /**
         * Creates a new <code>BaseWriter</code> instance writing to the given
         * <code>Writer</code>.
         *
         * @param out The <code>Writer</code> to which to write.
         *
         * @throws java.lang.NullPointerException if <code>out</code> is
         *      <code>null</code>.
         */
        BaseWriter(Writer out) {
            this.out = out;
        }

        /**
         * Write a single character.  The character to be written is contained in
         * the 16 low-order bits of the given integer value; the 16 high-order bits
         * are ignored.
         *
         * <p> Subclasses that intend to support efficient single-character output
         * should override this method.
         *
         * @param c  int specifying a character to be written.
         * @exception  IOException  If an I/O error occurs
         */
        public void write(int c) throws IOException {
            if (out != null) {
                out.write(c);
            }
        }

        /**
         * Write an array of characters.
         *
         * @param  cbuf  Array of characters to be written
         *
         * @exception  IOException  If an I/O error occurs
         */
        public void write(char cbuf[]) throws IOException {
            if (out != null) {
                out.write(cbuf);
            }
        }

        /**
         * Write a portion of an array of characters.
         *
         * @param  cbuf  Array of characters
         * @param  off   Offset from which to start writing characters
         * @param  len   Number of characters to write
         *
         * @exception  IOException  If an I/O error occurs
         */
        public void write(char cbuf[], int off, int len) throws IOException {
            if (out != null) {
                out.write(cbuf, off, len);
            }
        }

        /**
         * Write a string.
         *
         * @param  str  String to be written
         *
         * @exception  IOException  If an I/O error occurs
         */
        public void write(String str) throws IOException {
            if (out != null) {
                out.write(str);
            }
        }

        /**
         * Write a portion of a string.
         *
         * @param  str  A String
         * @param  off  Offset from which to start writing characters
         * @param  len  Number of characters to write
         *
         * @exception  IOException  If an I/O error occurs
         */
        public void write(String str, int off, int len) throws IOException {
            if (out != null) {
                out.write(str, off, len);
            }
        }

        /**
         * Flush the stream.  If the stream has saved any characters from the
         * various write() methods in a buffer, write them immediately to their
         * intended destination.  Then, if that destination is another character or
         * byte stream, flush it.  Thus one flush() invocation will flush all the
         * buffers in a chain of Writers and OutputStreams.
         *
         * @exception  IOException  If an I/O error occurs
         */
        public void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }

        /**
         * Flushes output to the client and marks this instance closed without
         * closing the next writer in the chain.
         */
        public void close() throws IOException {
            synchronized (lock) {
                if (out != null) {
                    try {
                        flush();
                    } finally {
                        out = null;
                    }
                }
            }
        }
    }

    /**
     * The <code>BaseOutputStream</code> class is a
     * <code>FilterOutputStream</code> whose {@link #close} method only flushes
     * the output but does not really call <code>closes</code> on the next
     * <code>OutputStream</code>.
     * <p>
     * If an instance of this class is closed through the {@link #close} method,
     * further calls to any of the <code>write</code> methods or to the
     * {@link #flush} method will have no effect.
     *
     * @version $Revision: 1.4 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
     * @author fmeschbe
     * @since fennec
     * @audience core
     */
    private static class BaseOutputStream extends OutputStream {

        protected OutputStream out;

        /**
         * Creates a new <code>BaseOutputStream</code> instance writing to the
         * given <code>OutputStream</code>.
         *
         * @param out The <code>OutputStream</code> to which to write.
         *
         * @throws java.lang.NullPointerException if <code>out</code> is
         *      <code>null</code>.
         */
        BaseOutputStream(OutputStream out) {
            this.out = out;
        }

        /**
         * Writes the specified byte to this output stream. If the stream has
         * already been closed, this method does nothing.
         *
         * @param b the <code>byte</code> to write.
         *
         * @throws IOException if an I/O error occurs.
         */
        public void write(int b) throws IOException {
            if (out != null) {
                out.write(b);
            }
        }

        /**
         * Writes <code>b.length</code> bytes from the specified byte array
         * to this output stream. If the stream has already been closed, this
         * method does nothing.
         *
         * @param b the data.
         * @throws IOException if an I/O error occurs.
         * @see #write(byte[], int, int)
         */
        public void write(byte b[]) throws IOException {
            if (out != null) {
                out.write(b);
            }
        }

        /**
         * Writes <code>len</code> bytes from the specified byte array
         * starting at offset <code>off</code> to this output stream. If the
         * stream has already been closed, this method does nothing.
         *
         * @param b the data.
         * @param off the start offset in the data.
         * @param len the number of bytes to write.
         * @throws IOException if an I/O error occurs.
         */
        public void write(byte b[], int off, int len) throws IOException {
            if (out != null) {
                out.write(b, off, len);
            }
        }

        /**
         * Flushes this output stream and forces any buffered output bytes
         * to be written out. If the stream has already been closed, this
         * method does nothing.
         *
         * @throws IOException if an I/O error occurs.
         */
        public void flush() throws IOException {
            if (out != null) {
                out.flush();
            }
        }

        /**
         * Flushes output to the client and marks this instance closed without
         * closing the next output stream in the chain.
         */
        public void close() throws IOException {
            if (out != null) {
                try {
                    flush();
                } finally {
                    out = null;
                }
            }
        }
    }
}
