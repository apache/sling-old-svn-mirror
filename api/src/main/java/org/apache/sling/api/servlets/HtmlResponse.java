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
package org.apache.sling.api.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.ResponseUtil;

/**
 * Generator for a HTML status response that displays the changes made in a post
 * request. see <a href="HtmlResponse.html">HtmlResponse.html</a> for the
 * format.
 *
 * @deprecated use org.apache.sling.servlets.post.HtmlResponse instead.
 */
@Deprecated
public class HtmlResponse {

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
     * externally mapped location url of the modified path
     */
    public static final String PN_LOCATION = "location";

    /**
     * externally mapped location url of the parent of the modified path
     */
    public static final String PN_PARENT_LOCATION = "parentLocation";

    /**
     * the path of the modified item. this is usually the addressed resource or
     * in case of a creation request (eg: /foo/*) the path of the newly created
     * node.
     */
    public static final String PN_PATH = "path";

    /**
     * the referrer of the request
     */
    public static final String PN_REFERER = "referer";

    /**
     * Indicating whether request processing created new data. This property
     * is initialized to <code>false</code> and may be changed by calling
     * the {@link #setCreateRequest(boolean)} method.
     */
    public static final String PN_IS_CREATED = "isCreate";

    /**
     * human readable changelog
     */
    public static final String PN_CHANGE_LOG = "changeLog";

    /**
     * The Throwable caught while processing the request. This property is not
     * set unless the {@link #setError(Throwable)} method is called.
     */
    public static final String PN_ERROR = "error";

    /**
     * name of the html template
     */
    private static final String TEMPLATE_NAME = "HtmlResponse.html";

    /**
     * list of changes
     */
    private final StringBuilder changes = new StringBuilder();

    /**
     * Properties of the response
     */
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Creates a new html response with default settings, which is
     * <code>null</code> for almost all properties except the
     * {@link #isCreateRequest()} which defaults to <code>false</code>.
     */
    public HtmlResponse() {
        setCreateRequest(false);
    }

    // ---------- Settings for the response ------------------------------------

    /**
     * Returns the referer as from the 'referer' request header.
     */
    public String getReferer() {
        return getProperty(PN_REFERER, String.class);
    }

    /**
     * Sets the referer property
     */
    public void setReferer(String referer) {
        setProperty(PN_REFERER, referer);
    }

    /**
     * Returns the absolute path of the item upon which the request operated.
     * <p>
     * If the {@link #setPath(String)} method has not been called yet, this
     * method returns <code>null</code>.
     */
    public String getPath() {
        return getProperty(PN_PATH, String.class);
    }

    /**
     * Sets the absolute path of the item upon which the request operated.
     */
    public void setPath(String path) {
        setProperty(PN_PATH, path);
    }

    /**
     * Returns <code>true</code> if this was a create request.
     * <p>
     * Before calling the {@link #setCreateRequest(boolean)} method, this method
     * always returns <code>false</code>.
     */
    public boolean isCreateRequest() {
        return getProperty(PN_IS_CREATED, Boolean.class);
    }

    /**
     * Sets whether the request was a create request or not.
     */
    public void setCreateRequest(boolean isCreateRequest) {
        setProperty(PN_IS_CREATED, isCreateRequest);
    }

    /**
     * Returns the location of the modification. this is the externalized form
     * of the current path.
     *
     * @return the location of the modification.
     */
    public String getLocation() {
        return getProperty(PN_LOCATION, String.class);
    }

    public void setLocation(String location) {
        setProperty(PN_LOCATION, location);
    }

    /**
     * Returns the parent location of the modification. this is the externalized
     * form of the parent node of the current path.
     *
     * @return the location of the modification.
     */
    public String getParentLocation() {
        return getProperty(PN_PARENT_LOCATION, String.class);
    }

    public void setParentLocation(String parentLocation) {
        setProperty(PN_PARENT_LOCATION, parentLocation);
    }

    /**
     * Sets the title of the response message
     *
     * @param title the title
     */
    public void setTitle(String title) {
        setProperty(PN_TITLE, title);
    }

    /**
     * sets the response status code properties
     *
     * @param code the code
     * @param message the message
     */
    public void setStatus(int code, String message) {
        setProperty(PN_STATUS_CODE, code);
        setProperty(PN_STATUS_MESSAGE, message);
    }

    /**
     * Returns the status code of this instance. If the status code has never
     * been set by calling the {@link #setStatus(int, String)} method, the
     * status code is determined by checking if there was an error.  If there
     * was an error, the response is assumed to be unsuccessful and 500 is returned.
     * If there is no error, the response is assumed to be successful and 200 is returned.
     */
    public int getStatusCode() {
        Integer status = getProperty(PN_STATUS_CODE, Integer.class);
        if (status == null) {
        	if (getError() != null) {
        		//if there was an error
        		status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        	} else {
        		status = HttpServletResponse.SC_OK;
        	}
        }
        return status;
    }

    public String getStatusMessage() {
        return getProperty(PN_STATUS_MESSAGE, String.class);
    }

    /**
     * Returns any recorded error or <code>null</code>
     *
     * @return an error or <code>null</code>
     */
    public Throwable getError() {
        return getProperty(PN_ERROR, Throwable.class);
    }

    public void setError(Throwable error) {
        setProperty(PN_ERROR, error);
    }

    /**
     * Returns <code>true</code> if no {@link #getError() error} is set and if
     * the {@link #getStatusCode() status code} is one of the 2xx codes.
     */
    public boolean isSuccessful() {
        return getError() == null && (getStatusCode() / 100) == 2;
    }

    // ---------- ChangeLog ----------------------------------------------------

    /**
     * Records a 'modified' change
     *
     * @param path path of the item that was modified
     */
    public void onModified(String path) {
        onChange("modified", path);
    }

    /**
     * Records a 'created' change
     *
     * @param path path of the item that was created
     */
    public void onCreated(String path) {
        onChange("created", path);
    }

    /**
     * Records a 'deleted' change
     *
     * @param path path of the item that was deleted
     */
    public void onDeleted(String path) {
        if (path != null) {
            onChange("deleted", path);
        }
    }

    /**
     * Records a 'moved' change. <p/> Note: the moved change only records the
     * basic move command. the implied changes on the moved properties and sub
     * nodes are not recorded.
     *
     * @param srcPath source path of the node that was moved
     * @param dstPath destination path of the node that was moved.
     */
    public void onMoved(String srcPath, String dstPath) {
        onChange("moved", srcPath, dstPath);
    }

    /**
     * Records a 'copied' change. <p/> Note: the copy change only records the
     * basic copy command. the implied changes on the copied properties and sub
     * nodes are not recorded.
     *
     * @param srcPath source path of the node that was copied
     * @param dstPath destination path of the node that was copied.
     */
    public void onCopied(String srcPath, String dstPath) {
        onChange("copied", srcPath, dstPath);
    }

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
     * prepares the response properties
     */
    private void prepare() {
        String path = getPath();
        if (getProperty(PN_STATUS_CODE) == null) {
            if (getError() != null) {
                setStatus(500, getError().toString());
                setTitle("Error while processing " + path);
            } else {
                if (isCreateRequest()) {
                    setStatus(201, "Created");
                    setTitle("Content created " + path);
                } else {
                    setStatus(200, "OK");
                    setTitle("Content modified " + path);
                }
            }
        }

        String referer = getReferer();
        if (referer == null) {
            referer = "";
        }
        setReferer(referer);

        // get changelog
        changes.insert(0, "<pre>");
        changes.append("</pre>");
        setProperty(PN_CHANGE_LOG, changes.toString());
    }

    /**
     * Sets a generic response property with the given
     *
     * @param name name of the property
     * @param value value of the property
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Returns the generic response property with the given name and type or
     * <code>null</code> if no such property exists or the property is not of
     * the requested type.
     */
    @SuppressWarnings("unchecked")
    public <Type> Type getProperty(String name, Class<Type> type) {
        Object value = getProperty(name);
        if (type.isInstance(value)) {
            return (Type) value;
        }

        return null;
    }

    /**
     * Returns the generic response property with the given name and type or
     * <code>null</code> if no such property exists.
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Writes the response to the given writer and replaces all ${var} patterns
     * by the value of the respective property. if the property is not defined
     * the pattern is not modified.
     *
     * @param response to send to
     * @param setStatus whether to set the status code on the response
     * @throws IOException if an i/o exception occurs
     */
    public void send(HttpServletResponse response, boolean setStatus)
            throws IOException {
        prepare();

        if (setStatus) {
            Object status = getProperty(PN_STATUS_CODE);
            if (status instanceof Number) {
                int statusCode = ((Number) status).intValue();
                response.setStatus(statusCode);

                // special treatment of 201/CREATED: Requires Location
                if (statusCode == HttpServletResponse.SC_CREATED) {
                    response.setHeader("Location", getLocation());
                }
            }
        }

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
                        Object prop = properties.get(varBuffer.toString());
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