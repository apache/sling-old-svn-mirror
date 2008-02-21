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
package org.apache.sling.ujax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Generator for a HTML status response that displays the changes made in a
 * ujax post request.
 *
 * see <a href="UjaxHtmlResponse.html">UjaxHtmlResponse.html</a> for the format.
 */
public class UjaxHtmlResponse {

    /**
     * some human readable title like: 200 Created /foo/bar
     */
    public static final String PN_TITLE = "title";

    /**
     * status code. more or less http response status codes
     */
    public static final String PN_STATUS_CODE = "status.code";

    /**
     * some human readable status message
     */
    public static final String PN_STATUS_MESSAGE = "status.message";

    /**
     * externaly mapped location url of the modified path
     */
    public static final String PN_LOCATION = "location";

    /**
     * externaly mapped location url of the parent of the modified path
     */
    public static final String PN_PARENT_LOCATION = "parentLocation";

    /**
     * the path of the modified item. this is usually the addressed resource
     * or in case of a creation request (eg: /foo/*) the path of the newly
     * created node.
     */
    public static final String PN_PATH = "path";

    /**
     * the referrer of the request
     */
    public static final String PN_REFERER = "referer";

    /**
     * human readable changelog
     */
    public static final String PN_CHANGE_LOG = "changeLog";

    /**
     * name of the html template
     */
    private static final String TEMPLATE_NAME = "UjaxHtmlResponse.html";

    /**
     * Properties of the response
     */
    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * the post processor that contains the change log and all infos
     */
    private final UjaxPostProcessor ctx;

    /**
     * the servlet response
     */
    private final HttpServletResponse response;

    /**
     * Creates a new ujax html response
     * @param ctx the request processor
     * @param response the response
     */
    public UjaxHtmlResponse(UjaxPostProcessor ctx, HttpServletResponse response) {
        this.ctx = ctx;
        this.response = response;
        prepare();
    }

    /**
     * prepares the response properties
     */
    private void prepare() {
        String path = ctx.getCurrentPath();
        if (path == null) {
            path = ctx.getRootPath();
        }
        setProperty(PN_PATH, path);

        if (ctx.getError() != null) {
            setStatus(500, ctx.getError().toString());
            setTitle("Error while processing " + path);
        } else {
            if (ctx.isCreateRequest()) {
                setStatus(201, "Created");
                setTitle("Content created " + path);
            } else {
                setStatus(200, "OK");
                setTitle("Content modified " + path);
            }
        }
        setProperty(PN_LOCATION, ctx.getLocation());
        setProperty(PN_PARENT_LOCATION, ctx.getParentLocation());
        String referer = ctx.getRequest().getHeader("referer");
        if (referer == null) {
            referer = "";
        }
        setProperty(PN_REFERER, referer);
        // get changelog
        StringBuffer cl = new StringBuffer("<pre>");
        ctx.getChangeLog().dump(cl, "<br/>");
        cl.append("</pre>");
        setProperty(PN_CHANGE_LOG, cl.toString());
    }

    /**
     * sets the response status code properties
     * @param code the code
     * @param message the message
     */
    public void setStatus(int code, String message) {
        setProperty(PN_STATUS_CODE, String.valueOf(code));
        setProperty(PN_STATUS_MESSAGE, message);
    }

    /**
     * Sets the title of the respose message
     * @param title the title
     */
    public void setTitle(String title) {
        setProperty(PN_TITLE, title);
    }

    /**
     * Sets a generic response property with the given
     * @param name name of the property
     * @param value value of the property
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    /**
     * Writes the response to the given writer and replaces all ${var} patterns
     * by the value of the respective property. if the property is not defined
     * the pattern is not modified.
     *
     * @throws IOException if an i/o exception occurs
     */
    public void send() throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        Writer out = response.getWriter();
        InputStream template = getClass().getResourceAsStream(TEMPLATE_NAME);
        Reader in = new BufferedReader(new InputStreamReader(template));
        StringBuffer varBuffer = new StringBuffer();
        int state = 0;
        int read;
        while ((read = in.read()) >= 0) {
            char c = (char) read;
            switch (state) {
                // initial
                case 0:
                    if (c=='$') {
                        state = 1;
                    } else {
                        out.write(c);
                    }
                    break;
                // $ read
                case 1:
                    if (c=='{') {
                        state = 2;
                    } else {
                        state = 0;
                        out.write('$');
                        out.write(c);
                    }
                    break;
                // { read
                case 2:
                    if (c=='}') {
                        state = 0;
                        String prop = properties.get(varBuffer.toString());
                        if (prop == null) {
                            out.write("${");
                            out.write(varBuffer.toString());
                            out.write("}");
                        } else {
                            out.write(prop);
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