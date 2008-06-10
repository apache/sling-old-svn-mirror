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
package org.apache.sling.jcr.webdav.impl.helper;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;

class SlingResourceLocator implements DavResourceLocator {

    private final String prefix;

    private final String workspaceName;

    private final String resourcePath;

    private final SlingLocatorFactory factory;

    private final String href;

    SlingResourceLocator(String prefix, String workspaceName,
            String resourcePath, SlingLocatorFactory factory) {

        this.prefix = prefix;
        this.workspaceName = workspaceName;
        this.resourcePath = resourcePath;
        this.factory = factory;

        StringBuffer buf = new StringBuffer(prefix);
        buf.append(Text.escapePath(resourcePath));
        int length = buf.length();
        if (length > 0 && buf.charAt(length - 1) != '/') {
            buf.append("/");
        }
        href = buf.toString();
    }

    /**
     * Return the prefix used to build the href String. This includes the
     * initial hrefPrefix as well a the path prefix.
     *
     * @return prefix String used to build the href.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the resource path which always starts with the workspace
     * path, if a workspace resource exists. For the top most resource
     * (request handle '/'), <code>null</code> is returned.
     *
     * @return resource path or <code>null</code>
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getResourcePath()
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Return the workspace path or <code>null</code> if this locator
     * object represents the '/' request handle.
     *
     * @return workspace path or <code>null</code>
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getWorkspacePath()
     */
    public String getWorkspacePath() {
        return "/" + workspaceName;
    }

    /**
     * Return the workspace name or <code>null</code> if this locator
     * object represents the '/' request handle, which does not contain a
     * workspace path.
     *
     * @return workspace name or <code>null</code>
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Returns true if the specified locator object refers to a resource
     * within the same workspace.
     *
     * @param locator
     * @return true if the workspace name obtained from the given locator
     *         refers to the same workspace as the workspace name of this
     *         locator.
     * @see DavResourceLocator#isSameWorkspace(org.apache.jackrabbit.webdav.DavResourceLocator)
     */
    public boolean isSameWorkspace(DavResourceLocator locator) {
        return (locator == null)
                ? false
                : isSameWorkspace(locator.getWorkspaceName());
    }

    /**
     * Returns true if the specified string equals to this workspace name or
     * if both names are null.
     *
     * @param workspaceName
     * @return true if the workspace name is equal to this workspace name.
     * @see DavResourceLocator#isSameWorkspace(String)
     */
    public boolean isSameWorkspace(String workspaceName) {
        String thisWspName = getWorkspaceName();
        return (thisWspName == null)
                ? workspaceName == null
                : thisWspName.equals(workspaceName);
    }

    /**
     * Returns an 'href' consisting of prefix and resource path (which
     * starts with the workspace path). It assures a trailing '/' in case
     * the href is used for collection. Note, that the resource path is
     * {@link Text#escapePath(String) escaped}.
     *
     * @param isCollection
     * @return href String representing the text of the href element
     * @see org.apache.jackrabbit.webdav.DavConstants#XML_HREF
     * @see DavResourceLocator#getHref(boolean)
     */
    public String getHref(boolean isCollection) {
        return (isCollection) ? href : href.substring(0, href.length() - 1);
    }

    /**
     * Returns true if the 'workspacePath' field is <code>null</code>.
     *
     * @return true if the 'workspacePath' field is <code>null</code>.
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#isRootLocation()
     */
    public boolean isRootLocation() {
        return getWorkspacePath() == null;
    }

    /**
     * Return the factory that created this locator.
     *
     * @return factory
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getFactory()
     */
    public DavLocatorFactory getFactory() {
        return factory;
    }

    /**
     * Uses {@link #getResourcePath()}
     * to build the repository path.
     *
     * @see DavResourceLocator#getRepositoryPath()
     */
    public String getRepositoryPath() {
        return getResourcePath();
//        factory.getRepositoryPath(getResourcePath(),
//            getWorkspacePath());
    }

    /**
     * Computes the hash code from the href, that is built from the prefix,
     * the workspace name and the resource path all of them representing
     * final instance fields.
     *
     * @return the hash code
     */
    public int hashCode() {
        return href.hashCode();
    }

    /**
     * Returns true, if the given object is a
     * <code>SlingResourceLocator</code> with the same hash code.
     *
     * @param obj the object to compare to
     * @return <code>true</code> if the 2 objects are equal;
     *         <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SlingResourceLocator) {
            SlingResourceLocator other = (SlingResourceLocator) obj;
            return hashCode() == other.hashCode();
        }
        return false;
    }
}