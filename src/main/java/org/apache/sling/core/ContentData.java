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
package org.apache.sling.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

import org.apache.sling.component.Component;
import org.apache.sling.component.Content;
import org.apache.sling.core.output.Buffer;
import org.apache.sling.core.output.BufferProvider;
import org.apache.sling.core.output.BufferedPrintWriter;
import org.apache.sling.core.output.BufferedServletOutputStream;


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
        content = null;
        
        // flush buffer contents to output
        try {
            flushBuffer();
        } catch (IOException ioe) {
            // TODO: handle
        }
    }
    
    /**
     * @return the content
     */
    public Content getContent() {
        return content;
    }

    /**
     * @return the component
     */
    public Component getComponent() {
        return component;
    }
    
    /**
     * @param component the component to set
     */
    public void setComponent(Component component) {
        this.component = component;
    }
    
    //---------- BufferProvider interface -------------------------------

    public void setBufferSize(int buffersize) {
        if (buffer != null) {
            buffer.setBufferSize(buffersize);
        }
        
        requestedBufferSize = buffersize;
    }
    
    public int getBufferSize() {
        if (buffer != null) {
            return buffer.getBufferSize();
        }
        
        return requestedBufferSize;
    }
    
    public void flushBuffer() throws IOException {
        if (buffer != null) {
            buffer.flushBuffer();
        }
    }
    
    public void resetBuffer() {
        if (buffer != null) {
            buffer.resetBuffer();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (buffer instanceof OutputStream) {
            return (ServletOutputStream) buffer;
        } else if (buffer != null) {
            throw new IllegalStateException("Writer already retrieved");
        }
        
        ServletOutputStream parentStream = parent.getOutputStream();
        BufferedServletOutputStream stream = new BufferedServletOutputStream(parentStream, getBufferSize());
        buffer = stream;
        
        return stream;
    }
    
    public PrintWriter getWriter() throws IOException {
        if (buffer instanceof PrintWriter) {
            return (PrintWriter) buffer;
        } else if (buffer != null) {
            throw new IllegalStateException("OutputStream already retrieved");
        }
        
        PrintWriter parentWriter = parent.getWriter();
        BufferedPrintWriter writer = new BufferedPrintWriter(parentWriter, getBufferSize());
        buffer = writer;
        
        return writer;
    }
}
