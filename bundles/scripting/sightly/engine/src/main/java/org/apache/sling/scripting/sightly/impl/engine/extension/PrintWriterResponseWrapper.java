/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

/**
 * Wrapper response to redirect the output into a specified print writer
 */
public class PrintWriterResponseWrapper extends SlingHttpServletResponseWrapper {

    private final PrintWriter writer;

    /**
     * Create a wrapper for the supplied wrappedRequest
     *
     * @param writer - the base writer
     * @param wrappedResponse - the wrapped response
     */
    public PrintWriterResponseWrapper(PrintWriter writer, SlingHttpServletResponse wrappedResponse) {
        super(wrappedResponse);
        this.writer = writer;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

}
