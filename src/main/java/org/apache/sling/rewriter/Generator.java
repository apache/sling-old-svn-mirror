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
import java.io.PrintWriter;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The <code>Generator</code> interface defines the start of a rewriter pipeline.
 * A generator is not a component managed by the container (OSGi). A
 * {@link GeneratorFactory} is a service managed by the container which creates
 * generator instances on demand.
 */
public interface Generator {

    /**
     * Initialize this component.
     * @param context The invocation context.
     * @param config The configuration for this component.
     */
    void init(ProcessingContext context, ProcessingComponentConfiguration config)
    throws IOException;


    /**
     * Set the content handler the generator should stream to.
     * @param handler A transformer or serializer.
     */
    void setContentHandler(ContentHandler handler);

    /**
     * Get the writer to write the output to.
     * @return A print writer.
     */
    PrintWriter getWriter();

    /**
     * Notify the generator that parsing is finished.
     */
    void finished() throws IOException, SAXException;

    /**
     * Dispose the generator.
     * This method is always invoked by the rewriter in order to
     * allow the generator to release any resources etc. After
     * this method has been called the instance is considered
     * unusable.
     */
    void dispose();
}
