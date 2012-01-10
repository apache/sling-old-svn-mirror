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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.ProcessorManager;
import org.xml.sax.ContentHandler;

/**
 * This response is used to pass the output through the rewriter pipeline.
 */
@Adaptable(adaptableClass=SlingHttpServletResponse.class, adapters={
    @Adapter(value=ContentHandler.class,
            condition="When the response is being processed through the Sling Rewriter filter.")
})
class RewriterResponse
    extends SlingHttpServletResponseWrapper {

    /** The current request. */
    private final SlingHttpServletRequest request;

    /** The processor. */
    private Processor processor;

    /** wrapped rewriter/servlet writer */
    private PrintWriter writer;

    /** response content type */
    private String contentType;

    /** The processor manager. */
    private final ProcessorManager processorManager;

    /**
     * Initializes a new instance.
     * @param request The sling request.
     * @param delegatee The SlingHttpServletResponse wrapped by this instance.
     */
    public RewriterResponse(SlingHttpServletRequest request,
                            SlingHttpServletResponse delegatee,
                            ProcessorManager processorManager) {
        super(delegatee);
        this.processorManager = processorManager;
        this.request = request;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    public void setContentType(String type) {
        this.contentType = type;
        super.setContentType(type);
    }

    /**
     * Wraps the underlying writer by a rewriter pipeline.
     *
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if ( this.processor != null && this.writer == null ) {
            return new PrintWriter(new Writer() {

                @Override
                public void close() throws IOException {
                    // nothing to do
                }

                @Override
                public void flush() throws IOException {
                    // nothing to do
                }

                @Override
                public void write(char[] cbuf, int off, int len)
                throws IOException {
                    // nothing to do
                }
             });
        }
        if (writer == null) {
            this.processor = this.getProcessor();
            if ( this.processor != null ) {
                this.writer = this.processor.getWriter();
            }
            if ( this.writer == null ) {
                this.writer = super.getWriter();
            }
        }
        return writer;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else {
            super.flushBuffer();
        }
    }

    /**
     * Inform this response that the request processing is finished.
     * @throws IOException
     */
    public void finished(final boolean errorOccured) throws IOException {
        if ( this.processor != null ) {
            this.processor.finished(errorOccured);
            this.processor = null;
        }
    }

    /**
     * If we have a pipeline configuration for the current request,
     * we can adapt this response to a content handler.
     * @see org.apache.sling.api.adapter.Adaptable#adaptTo(java.lang.Class)
     */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if ( type == ContentHandler.class ) {
            this.processor = this.getProcessor();
            if ( this.processor != null ) {
                @SuppressWarnings("unchecked")
                final AdapterType object = (AdapterType)this.processor.getContentHandler();
                return object;
            }
        }
        return super.adaptTo(type);
    }

    /**
     * Search the first matching processor
     */
    private Processor getProcessor() {
        final ProcessingContext processorContext = new ServletProcessingContext(this.request, this, this.getSlingResponse(), this.contentType);
        Processor found = null;
        final List<ProcessorConfiguration> processorConfigs = this.processorManager.getProcessorConfigurations();
        final Iterator<ProcessorConfiguration> i = processorConfigs.iterator();
        while ( found == null && i.hasNext() ) {
            final ProcessorConfiguration config = i.next();
            if ( config.match(processorContext) ) {
                try {
                    found = this.processorManager.getProcessor(config, processorContext);
                    this.request.getRequestProgressTracker().log("Found processor for post processing {0}", config);
                } catch (final SlingException se) {
                    // if an exception occurs during setup of the pipeline and we are currently
                    // already processing an error, we ignore this!
                    if ( processorContext.getRequest().getAttribute("javax.servlet.error.status_code") != null ) {
                        this.request.getRequestProgressTracker().log("Ignoring found processor for post processing {0}" +
                                " as an error occured ({1}) during setup while processing another error.", config, se);
                    } else {
                        throw se;
                    }
                }
            }
        }
        return found;
    }
}
