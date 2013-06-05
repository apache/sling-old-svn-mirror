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
package org.apache.sling.servlets.post;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.ResponseUtil;

/**
 * The <code>HtmlResponse</code> is an {@link AbstractPostResponse} preparing
 * the response in HTML (actually XHTML) such that it can be interpreted
 * as a plain response in a browser or as XML response in an Ajax request.
 */
public class HtmlResponse extends AbstractPostResponse {

    /**
     * Name of the property into which the change log is gathered to be
     * sent back by the {@link #doSend(HttpServletResponse)} method. This
     * property is only sent before replacing all variables in the HTML
     * response remplate.
     */
    private static final String PN_CHANGE_LOG = "changeLog";

    /**
     * name of the html template
     */
    private static final String TEMPLATE_NAME = "HtmlResponse.html";

    /**
     * list of changes
     */
    private final StringBuilder changes = new StringBuilder();

    /**
     * Records a generic change of the given <code>type</code>.
     * <p>
     * The change is added to the internal list of changes with the syntax of a
     * method call, where the <code>type</code> is the method name and the
     * <code>arguments</code> are the string arguments to the method enclosed in
     * double quotes. For example, the the call
     *
     * <pre>
     * onChange(&quot;sameple&quot;, &quot;arg1&quot;, &quot;arg2&quot;);
     * </pre>
     *
     * is aded as
     *
     * <pre>
     * sample(&quot;arg1&quot;, &quot;arg2&quot;)
     * </pre>
     *
     * to the internal list of changes.
     *
     * @param type The type of the modification
     * @param arguments The arguments to the modifications
     */
    public void onChange(String type, String... arguments) {
        changes.append(type);
        String delim = "(";
        for (String a : arguments) {
            changes.append(delim);
            changes.append('\"');
            changes.append(a);
            changes.append('\"');
            delim = ", ";
        }
        changes.append(");<br/>");
    }

    // ---------- Response Generation ------------------------------------------

    /**
     * Writes the response to the given writer and replaces all ${var} patterns
     * by the value of the respective property. if the property is not defined
     * the pattern is not modified.
     *
     * @param response to send to
     * @param setStatus whether to set the status code on the response
     * @throws IOException if an i/o exception occurs
     */
    @Override
    protected void doSend(HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        // get changelog
        changes.insert(0, "<pre>");
        changes.append("</pre>");
        setProperty(PN_CHANGE_LOG, changes.toString());

        Writer out = response.getWriter();
        InputStream template = getClass().getResourceAsStream(TEMPLATE_NAME);
        Reader in = new BufferedReader(new InputStreamReader(template));
        StringBuilder varBuffer = new StringBuilder();
        int state = 0;
        int read;
        while ((read = in.read()) >= 0) {
            char c = (char) read;
            switch (state) {
                // initial
                case 0:
                    if (c == '$') {
                        state = 1;
                    } else {
                        out.write(c);
                    }
                    break;
                // $ read
                case 1:
                    if (c == '{') {
                        state = 2;
                    } else {
                        state = 0;
                        out.write('$');
                        out.write(c);
                    }
                    break;
                // { read
                case 2:
                    if (c == '}') {
                        state = 0;
                        Object prop = getProperty(varBuffer.toString());
                        if (prop != null) {
                            out.write(ResponseUtil.escapeXml(prop.toString()));
                        }
                        varBuffer.setLength(0);
                    } else {
                        varBuffer.append(c);
                    }
            }
        }
        in.close();
        out.flush();
    }

}