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
package org.apache.sling.servlets.post;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * The <code>PostResponse</code> interface defines the API of a response
 * container which can (and should) be used by {@link PostOperation} services to
 * prepare responses to be sent back to the client.
 * <p>
 * This bundle provides a preconfigured {@link HtmlResponse} and a
 * {@link JSONResponse} implementation of this interface. Clients may extend the
 * {@link AbstractPostResponse} class to provide their own response
 * implementations.
 */
public interface PostResponse {

    /**
     * Sets the referer property
     */
    public void setReferer(String referer);

    /**
     * Returns the referer previously set by {@link #setReferer(String)}
     */
    public String getReferer();

    /**
     * Sets the absolute path of the item upon which the request operated.
     */
    public void setPath(String path);

    /**
     * Returns the absolute path of the item upon which the request operated.
     * <p>
     * If the {@link #setPath(String)} method has not been called yet, this
     * method returns <code>null</code>.
     */
    public String getPath();

    /**
     * Sets whether the request was a create request or not.
     */
    public void setCreateRequest(boolean isCreateRequest);

    /**
     * Returns <code>true</code> if this was a create request.
     * <p>
     * Before calling the {@link #setCreateRequest(boolean)} method, this method
     * always returns <code>false</code>.
     */
    public boolean isCreateRequest();

    /**
     * Sets the location of this modification. This is the externalized form of
     * the {@link #getPath() current path}.
     *
     * @param location
     */
    public void setLocation(String location);

    /**
     * Returns the location of the modification.
     * <p>
     * If the {@link #setLocation(String)} method has not been called yet, this
     * method returns <code>null</code>.
     */
    public String getLocation();

    /**
     * Sets the parent location of the modification. This is the externalized
     * form of the parent node of the {@link #getPath() current path}.
     */
    public void setParentLocation(String parentLocation);

    /**
     * Returns the parent location of the modification.
     * <p>
     * If the {@link #setParentLocation(String)} method has not been called yet,
     * this method returns <code>null</code>.
     */
    public String getParentLocation();

    /**
     * Sets the title of the response message
     *
     * @param title the title
     */
    public void setTitle(String title);

    /**
     * Sets the response status code properties
     *
     * @param code the code
     * @param message the message
     */
    public void setStatus(int code, String message);

    /**
     * Returns the status code of this instance. If the status code has never
     * been set by calling the {@link #setStatus(int, String)} method, the
     * status code is determined by checking if there was an error. If there was
     * an error, the response is assumed to be unsuccessful and 500 is returned.
     * If there is no error, the response is assumed to be successful and 200 is
     * returned.
     */
    public int getStatusCode();

    /**
     * Returns the status message or <code>null</code> if no has been set with
     * the {@link #setStatus(int, String)} method.
     */
    public String getStatusMessage();

    /**
     * Sets the recorded error causing the operation to fail.
     */
    public void setError(Throwable error);

    /**
     * Returns any recorded error or <code>null</code>
     *
     * @return an error or <code>null</code>
     */
    public Throwable getError();

    /**
     * Returns <code>true</code> if no {@link #getError() error} is set and if
     * the {@link #getStatusCode() status code} is one of the 2xx codes.
     */
    public boolean isSuccessful();

    // ---------- ChangeLog ----------------------------------------------------

    /**
     * Records a 'created' change
     *
     * @param path path of the item that was created
     */
    public void onCreated(String path);

    /**
     * Records a 'modified' change
     *
     * @param path path of the item that was modified
     */
    public void onModified(String path);

    /**
     * Records a 'deleted' change
     *
     * @param path path of the item that was deleted
     */
    public void onDeleted(String path);

    /**
     * Records a 'moved' change.
     * <p>
     * Note: the moved change only records the basic move command. the implied
     * changes on the moved properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was moved
     * @param dstPath destination path of the node that was moved.
     */
    public void onMoved(String srcPath, String dstPath);

    /**
     * Records a 'copied' change.
     * <p>
     * Note: the copy change only records the basic copy command. the implied
     * changes on the copied properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was copied
     * @param dstPath destination path of the node that was copied.
     */
    public void onCopied(String srcPath, String dstPath);

    /**
     * Records a generic change of the given <code>type</code> with arguments.
     *
     * @param type The type of the modification
     * @param arguments The arguments to the modifications
     */
    void onChange(String type, String... arguments);

    /**
     * Writes the response back over the provided HTTP channel. The actual
     * format of the response is implementation dependent.
     *
     * @param response to send to
     * @param setStatus whether to set the status code on the response
     * @throws IOException if an i/o exception occurs
     */
    void send(HttpServletResponse response, boolean setStatus)
            throws IOException;

}