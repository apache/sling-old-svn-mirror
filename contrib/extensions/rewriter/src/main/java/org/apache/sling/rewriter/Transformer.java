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
package org.apache.sling.rewriter;

import java.io.IOException;

import org.xml.sax.ContentHandler;

/**
 * The <code>Transformer</code> interface defines the middle of a rewriter pipeline.
 */
public interface Transformer extends ContentHandler {

    /**
     * Initialize this component.
     * @param context The invocation context.
     * @param config The configuration for this component.
     */
    void init(ProcessingContext context, ProcessingComponentConfiguration config)
    throws IOException;

    /**
     * Set the content handler the transformer should stream to.
     * @param handler Another transformer or a serializer.
     */
    void setContentHandler(ContentHandler handler);

    /**
     * Dispose the transformer.
     * This method is always invoked by the rewriter in order to
     * allow the transformer to release any resources etc. After
     * this method has been called the instance is considered
     * unusable.
     */
    void dispose();
}
