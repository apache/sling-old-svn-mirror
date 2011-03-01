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
package org.apache.sling.installer.api;

/**
 * The update result is returned by an {@link UpdateHandler} if
 * a resource could be persisted by the handler.
 *
 * @since 3.1
 */
public class UpdateResult {

    private final String url;
    private String digest;
    private Integer priority;
    private boolean resourceIsMoved = false;

    /**
     * Create an update result
     *
     * @param url Unique url for the resource. This should include the scheme!
     */
    public UpdateResult(final String url) {
        if ( url == null ) {
            throw new IllegalArgumentException("url must not be null.");
        }

        this.url = url;
    }

    /**
     * Return this data's url. It is opaque for the {@link OsgiInstaller}
     * but should uniquely identify the resource within the namespace of
     * the used installation mechanism.
     * The url includes the scheme followed by a colon followed by the unique id.
     */
    public String getURL() {
        return this.url;
    }

    public String getScheme() {
        final int pos = this.url.indexOf(':');
        return this.url.substring(0, pos);
    }

    public String getResourceId() {
        final int pos = this.url.indexOf(':');
        return this.url.substring(pos + 1);
    }

    /**
     * Return this resource's digest. Not necessarily an actual md5 or other digest of the
     * data, can be any string that changes if the data changes.
     * @return The digest or null
     */
    public String getDigest() {
        return this.digest;
    }

    /**
     * Return the priority of this resource. Priorities are used to decide which
     * resource to install when several are registered for the same OSGi entity
     * (bundle, config, etc.)
     */
    public int getPriority() {
        return this.priority != null ? this.priority : InstallableResource.DEFAULT_PRIORITY;
    }

    public void setPriority(final Integer prio) {
        this.priority = prio;
    }

    public void setDigest(final String digest) {
        this.digest = digest;
    }

    public void setResourceIsMoved(final boolean flag) {
        this.resourceIsMoved = flag;
    }

    public boolean getResourceIsMoved() {
        return this.resourceIsMoved;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", priority=" + this.getPriority() + ", url=" + url;
    }
}