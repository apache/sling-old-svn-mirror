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
package org.apache.sling.scripting.jsp.util;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

/**
 * The <code>JspSlingHttpServletResponseWrapper</code> class may be used by
 * tag library implementors to create a <code>RenderResponse</code> object
 * which wraps the writer of the response of a given page context.
 * <p>
 * Instances of this class only support writers. Trying to get an
 * <code>OutputStream</code> always results in an
 * <code>IllegalStateException</code>. This is the same behaviour as
 * implemented by response wrappers of Apache Jasper.
 */
public class JspSlingHttpServletResponseWrapper extends
        SlingHttpServletResponseWrapper {

    // The original JspWriter of the wrapped response
    private JspWriter jspWriter;

    // The PrintWriter returned by the getWriter method. Wraps jspWriter
    private PrintWriter printWriter;

    /**
     * Creates an instance of this response wrapper for the given
     * <code>pageContext</code>. The original JspWriter is retrieved from the
     * page context calling the <code>PageContext.getOut()</code> method. The
     * delegatee <code>RenderResponse</code> is retrieved from the page
     * context by calling the {@link TagUtil#getResponse(PageContext)} method.
     *
     * @param pageContext The <code>PageContext</code> to use to get the
     *            original output stream and the delegatee response.
     * @see TagUtil#getResponse(PageContext)
     */
    public JspSlingHttpServletResponseWrapper(PageContext pageContext) {
        super(TagUtil.getResponse(pageContext));

        this.jspWriter = pageContext.getOut();
        this.printWriter = new PrintWriter(this.jspWriter);
    }

    /**
     * Returns the writer for this response wrapper.
     */
    @Override
    public PrintWriter getWriter() {
        return this.printWriter;
    }

    /**
     * Throws an <code>IllegalStateException</code> as this wrapper only
     * supports writers.
     */
    @Override
    public ServletOutputStream getOutputStream() {
        throw new IllegalStateException();
    }

    /**
     * Resets the buffer of the JspWriter underlying the writer of this
     * instance.
     */
    @Override
    public void resetBuffer() {
        try {
            this.jspWriter.clearBuffer();
        } catch (IOException ignore) {
            // don't care
        }
    }
}
