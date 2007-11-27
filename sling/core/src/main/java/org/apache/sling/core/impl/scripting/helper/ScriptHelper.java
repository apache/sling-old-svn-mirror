/*
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
 */
package org.apache.sling.core.impl.scripting.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

/** Simple script helper providing access to the (wrapped) response, the
 * on-demand writer and a simple API for request inclusion. Instances of this
 * class are made available to the scripts as the global <code>sling</code>
 * variable.
 */
public class ScriptHelper implements SlingScriptHelper {

    private final SlingScript script;

    private final SlingHttpServletRequest request;

    private final SlingHttpServletResponse response;

    public ScriptHelper(SlingScript script, SlingHttpServletRequest request, SlingHttpServletResponse response) {
        this.script = script;
        this.request = request;
        this.response = new OnDemandWriterResponse(response);
    }

    public SlingScript getScript() {
        return script;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public SlingHttpServletResponse getResponse() {
        return response;
    }

    public void include(String path) throws ServletException, IOException {
        include(path, null);
    }

    public void include(String path, RequestDispatcherOptions options)
            throws ServletException, IOException {
        // TODO: Implement for options !!
        RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path);
        if (dispatcher != null) {
            dispatcher.include(getRequest(), getResponse());
        }
    }


    /** Simple Response wrapper returning an on-demand writer when asked for
     * a writer.
     */
    private static class OnDemandWriterResponse extends SlingHttpServletResponseWrapper {

        private PrintWriter writer;

        OnDemandWriterResponse(SlingHttpServletResponse delegatee) {
            super(delegatee);
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OnDemandWriter(getResponse()));
            }

            return writer;
        }
    }

    /** A writer acquiring the actual writer to delegate to on demand when the
     * first data is to be written. */
    private static class OnDemandWriter extends Writer {

        private final ServletResponse response;
        private Writer delegatee;

        OnDemandWriter(ServletResponse response) {
            this.response = response;
        }

        private Writer getWriter() throws IOException {
            if (delegatee == null) {
                delegatee = response.getWriter();
            }

            return delegatee;
        }

        @Override
        public void write(int c) throws IOException {
            synchronized (lock) {
                getWriter().write(c);
            }
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            synchronized (lock) {
                getWriter().write(cbuf);
            }
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            synchronized (lock) {
                getWriter().write(cbuf, off, len);
            }
        }

        @Override
        public void write(String str) throws IOException {
            synchronized (lock) {
                getWriter().write(str);
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            synchronized (lock) {
                getWriter().write(str, off, len);
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (lock) {
                Writer writer = delegatee;
                if (writer != null) {
                    writer.flush();
                }
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                // flush and close the delegatee if existing, otherwise ignore
                Writer writer = delegatee;
                if (writer != null) {
                    writer.flush();
                    writer.close();

                    // drop the delegatee now
                    delegatee = null;
                }
            }
        }
    }
}
