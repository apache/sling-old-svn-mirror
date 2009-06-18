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
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * The context for a processor invocation.
 */
public interface ProcessingContext {

    /**
     * The current request.
     */
    SlingHttpServletRequest getRequest();

    /**
     * The current response.
     */
    SlingHttpServletResponse getResponse();

    /**
     * The content type of the response.
     */
    String getContentType();

    /**
     * The writer.
     */
    PrintWriter getWriter() throws IOException;

    /**
     * The output stream.
     */
    OutputStream getOutputStream() throws IOException;
}
