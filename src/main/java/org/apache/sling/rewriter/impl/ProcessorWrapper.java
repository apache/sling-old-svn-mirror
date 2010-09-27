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

import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.xml.sax.ContentHandler;

/**
 * This is a wrapper for a processor.
 */
public class ProcessorWrapper implements Processor {

    private final Processor delegatee;

    public ProcessorWrapper(final ProcessorConfiguration config,
                            final FactoryCache factoryCache)
    throws IOException {
        this.delegatee = factoryCache.getProcessor(config.getType());
        if ( this.delegatee == null ) {
            throw new IOException("Unable to get processor component for type " + config.getType());
        }
    }

    /**
     * @see org.apache.sling.rewriter.Processor#finished(boolean)
     */
    public void finished(final boolean errorOccured) throws IOException {
        delegatee.finished(errorOccured);
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getWriter()
     */
    public PrintWriter getWriter() {
        return delegatee.getWriter();
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return delegatee.getContentHandler();
    }

    /**
     * @see org.apache.sling.rewriter.Processor#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessorConfiguration)
     */
    public void init(ProcessingContext context, ProcessorConfiguration config)
    throws IOException {
        delegatee.init(context, config);
    }

    @Override
    public String toString() {
        return delegatee.toString();
    }
}
