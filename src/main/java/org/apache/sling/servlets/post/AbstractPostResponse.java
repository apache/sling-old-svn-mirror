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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AbstractPostResponse</code> class provides a basic implementation
 * of the {@link PostResponse} interface maintaining properties to be
 * prepared for sending the response in an internal map.
 */
public abstract class AbstractPostResponse implements PostResponse {

    /**
     * Name of the title property set by {@link #setTitle(String)}
     */
    public static final String PN_TITLE = "title";

    /**
     * Name of the status code property set by {@link #setStatus(int, String)}
     */
    public static final String PN_STATUS_CODE = "status.code";

    /**
     * Name of the status message property set by {@link #setStatus(int, String)}
     */
    public static final String PN_STATUS_MESSAGE = "status.message";

    /**
     * Name of the location property set by {@link #setLocation(String)}
     */
    public static final String PN_LOCATION = "location";

    /**
     * Name of the parent location property set by {@link #setParentLocation(String)}
     */
    public static final String PN_PARENT_LOCATION = "parentLocation";

    /**
     * Name of the path property set by {@link #setPath(String)}
     */
    public static final String PN_PATH = "path";

    /**
     * Name of the referer property set by {@link #setReferer(String)}
     */
    public static final String PN_REFERER = "referer";

    /**
     * Name of the create status property set by {@link #setCreateRequest(boolean)}
     */
    public static final String PN_IS_CREATED = "isCreate";

    /**
     * Name of the error property set by {@link #setError(Throwable)}
     */
    public static final String PN_ERROR = "error";

    /**
     * Properties of the response
     */
    private final Map<String, Object> properties = new HashMap<String, Object>();

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
        final Boolean isCreateRequest = getProperty(PN_IS_CREATED,
            Boolean.class);
        return (isCreateRequest != null)
                ? isCreateRequest.booleanValue()
                : false;
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
     * status code is determined by checking if there was an error. If there was
     * an error, the response is assumed to be unsuccessful and 500 is returned.
     * If there is no error, the response is assumed to be successful and 200 is
     * returned.
     */
    public int getStatusCode() {
        Integer status = getProperty(PN_STATUS_CODE, Integer.class);
        if (status == null) {
            if (getError() != null) {
                // if there was an error
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
     * Records a 'moved' change.
     * <p/>
     * Note: the moved change only records the basic move command. the implied
     * changes on the moved properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was moved
     * @param dstPath destination path of the node that was moved.
     */
    public void onMoved(String srcPath, String dstPath) {
        onChange("moved", srcPath, dstPath);
    }

    /**
     * Records a 'copied' change.
     * <p/>
     * Note: the copy change only records the basic copy command. the implied
     * changes on the copied properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was copied
     * @param dstPath destination path of the node that was copied.
     */
    public void onCopied(String srcPath, String dstPath) {
        onChange("copied", srcPath, dstPath);
    }


    /**
     * prepares the response properties
     */
    private void prepare(final HttpServletResponse response, final boolean setStatus) {
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

        if (setStatus) {
            Object status = getProperty(PN_STATUS_CODE);
            if (status instanceof Number) {
                int statusCode = ((Number) status).intValue();
                response.setStatus(statusCode);

                // special treatment of 201/CREATED and 3xx: Requires Location
                if (statusCode == HttpServletResponse.SC_CREATED || statusCode / 100 == 3) {
                    response.setHeader("Location", getLocation());
                }
            }
        }

    }

    /**
     * Sets a generic response property with the given
     *
     * @param name name of the property
     * @param value value of the property
     */
    protected void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Returns the generic response property with the given name and type or
     * <code>null</code> if no such property exists or the property is not of
     * the requested type.
     */
    @SuppressWarnings("unchecked")
    protected <Type> Type getProperty(String name, Class<Type> type) {
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
    protected Object getProperty(String name) {
        return properties.get(name);
    }

    protected abstract void doSend(HttpServletResponse response) throws IOException;

    /**
     * Writes the response to the given writer and replaces all ${var} patterns
     * by the value of the respective property. if the property is not defined
     * the pattern is not modified.
     *
     * @param response to send to
     * @param setStatus whether to set the status code on the response
     * @throws IOException if an i/o exception occurs
     */
    public final void send(HttpServletResponse response, boolean setStatus)
            throws IOException {
        prepare(response, setStatus);
        doSend(response);
    }

}