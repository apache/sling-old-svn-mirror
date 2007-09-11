/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

import org.apache.sling.component.Component;
import org.apache.sling.component.Content;
import org.apache.sling.core.impl.output.Buffer;
import org.apache.sling.core.impl.output.BufferProvider;
import org.apache.sling.core.impl.output.BufferedPrintWriter;
import org.apache.sling.core.impl.output.BufferedServletOutputStream;


/**
 *
 * The <code>ContentData</code> class provides objects which are relevant for
 * the processing of a single Content object by its Component.
 *
 * @see RequestData
 */
public class ContentData implements BufferProvider {

    private Content content;

    private Component component;

    private Buffer buffer;
    private int requestedBufferSize;
    private BufferProvider parent;

    public ContentData(Content content, BufferProvider parent) {
        this.content = content;
        this.parent = parent;
    }

    /* package */ void dispose() {
        this.content = null;

        // flush buffer contents to output
        try {
            this.flushBuffer();
        } catch (IOException ioe) {
            // TODO: handle
        }
    }

    /**
     * @return the content
     */
    public Content getContent() {
        return this.content;
    }

    /**
     * @return the component
     */
    public Component getComponent() {
        return this.component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(Component component) {
        this.component = component;
    }

    //---------- BufferProvider interface -------------------------------

    public void setBufferSize(int buffersize) {
        if (this.buffer != null) {
            this.buffer.setBufferSize(buffersize);
        }

        this.requestedBufferSize = buffersize;
    }

    public int getBufferSize() {
        if (this.buffer != null) {
            return this.buffer.getBufferSize();
        }

        return this.requestedBufferSize;
    }

    public void flushBuffer() throws IOException {
        if (this.buffer != null) {
            this.buffer.flushBuffer();
        }
    }

    public void resetBuffer() {
        if (this.buffer != null) {
            this.buffer.resetBuffer();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (this.buffer instanceof OutputStream) {
            return (ServletOutputStream) this.buffer;
        } else if (this.buffer != null) {
            throw new IllegalStateException("Writer already retrieved");
        }

        ServletOutputStream parentStream = this.parent.getOutputStream();
        BufferedServletOutputStream stream = new BufferedServletOutputStream(parentStream, this.getBufferSize());
        this.buffer = stream;

        return stream;
    }

    public PrintWriter getWriter() throws IOException {
        if (this.buffer instanceof PrintWriter) {
            return (PrintWriter) this.buffer;
        } else if (this.buffer != null) {
            throw new IllegalStateException("OutputStream already retrieved");
        }

        PrintWriter parentWriter = this.parent.getWriter();
        BufferedPrintWriter writer = new BufferedPrintWriter(parentWriter, this.getBufferSize());
        this.buffer = writer;

        return writer;
    }
}
