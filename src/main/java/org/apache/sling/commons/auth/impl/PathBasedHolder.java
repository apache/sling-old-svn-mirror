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
package org.apache.sling.commons.auth.impl;

/**
 * The <code>AuthenticationHandlerHolder</code> class represents an
 * authentication handler service in the internal data structure of the
 * {@link SlingAuthenticator}.
 *
 * @since 2.1
 */
public abstract class PathBasedHolder implements Comparable<PathBasedHolder> {

    // full path of the service registration
    protected final String fullPath;

    // file path part of the service registration full path
    final String path;

    // host element of the service registration full path
    final String host;

    // protocol element of the service registration full path
    final String protocol;

    protected PathBasedHolder(final String fullPath) {

        String path = fullPath;
        String host = "";
        String protocol = "";

        // check for protocol prefix in the full path
        if (path.startsWith("http://") || path.startsWith("https://")) {
            int idxProtocolEnd = path.indexOf("://");
            protocol = path.substring(0, idxProtocolEnd);
            path = path.substring(idxProtocolEnd + 1);
        }

        // check for host prefix in the full path
        if (path.startsWith("//")) {
            int idxHostEnd = path.indexOf("/", 2);
            idxHostEnd = idxHostEnd == -1 ? path.length() : idxHostEnd;

            if (path.length() > 2) {
                host = path.substring(2, idxHostEnd);
                if (idxHostEnd < path.length()) {
                    path = path.substring(idxHostEnd);
                } else {
                    path = "/";
                }
            } else {
                path = "/";
            }
        }

        // assign the fields
        this.fullPath = fullPath;
        this.path = path;
        this.host = host;
        this.protocol = protocol;
    }

    public int compareTo(PathBasedHolder other) {
        return other.path.compareTo(path);
    }

    @Override
    public int hashCode() {
        return fullPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        }

        if (obj.getClass() == getClass()) {
            PathBasedHolder other = (PathBasedHolder) obj;
            return fullPath.equals(other.fullPath);
        }

        return false;
    }
}