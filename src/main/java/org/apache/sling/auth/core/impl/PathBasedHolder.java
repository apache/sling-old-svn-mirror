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
package org.apache.sling.auth.core.impl;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * The <code>PathBasedHolder</code> provides the basic abstraction for managing
 * authentication handler and authentication requirements in the
 * {@link SlingAuthenticator} with the following base functionality:
 * <ul>
 * <li>Provide location of control through its path fields</li>
 * <li>Support orderability of instances by being <code>Comparable</code> and
 * ordering according to the {@link #fullPath} and the
 * <code>ServiceReference</code> of the provider service</li>
 * <li>Support {@link #equals(Object)} and {@link #hashCode()} compatible with
 * the <code>Comparable</code> implementation.</li>
 * </ul>
 */
public abstract class PathBasedHolder implements Comparable<PathBasedHolder> {

    /**
     * The full registration path of this instance. This is the actual URL with
     * which this instance has been created.
     */
    protected final String fullPath;

    /**
     * The Scheme part of the URL of the {@link #fullPath}. If no scheme is
     * contained, this field is set to an empty string.
     */
    final String protocol;

    /**
     * The host part of the URL of the {@link #fullPath}. If no host is
     * contained, this field is set to an empty string.
     */
    final String host;

    /**
     * The path part of the URL of the {@link #fullPath}. If that URL contains
     * neither a scheme nor a host, this field is actually set to the same as
     * {@link #fullPath}.
     */
    final String path;

    /**
     * The <code>ServiceReference</code> to the service, which causes this
     * instance to be created. This may be <code>null</code> if the entry has
     * been created by the {@link SlingAuthenticator} itself.
     */
    private final ServiceReference<?> serviceReference;

    /**
     * Sets up this instance with the given configuration URL provided by the
     * given <code>serviceReference</code>.
     * <p>
     * The <code>serviceReference</code> may be <code>null</code> which means
     * the configuration is created by the {@link SlingAuthenticator} itself.
     * Instances whose service reference is <code>null</code> are always ordered
     * behind instances with non-<code>null</code> service references (provided
     * their path is equal.
     *
     * @param url The configuration URL to setup this instance with
     * @param serviceReference The reference to the service providing the
     *            configuration for this instance.
     */
    protected PathBasedHolder(final String url,
            final ServiceReference<?> serviceReference) {

        String path = url;
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
        this.fullPath = url;
        this.path = path;
        this.host = host;
        this.protocol = protocol;
        this.serviceReference = serviceReference;
    }

    /**
     * Returns a descriptive string of the provider of this instance. The string
     * is derived from the service reference with which this instance has been
     * created. If the instance has been created without a service reference it
     * is ordered the service description of the {@link SlingAuthenticator} is
     * returned.
     */
    final String getProvider() {
        // assume the commons/auth SlingAuthenticator provides the entry
        if (serviceReference == null) {
            return SlingAuthenticator.DESCRIPTION;
        }

        final String descr = PropertiesUtil.toString(
            serviceReference.getProperty(Constants.SERVICE_DESCRIPTION), null);
        if (descr != null) {
            return descr;
        }

        return "Service "
            + PropertiesUtil.toString(
                serviceReference.getProperty(Constants.SERVICE_ID), "unknown");
    }

    /**
     * Compares this instance to the <code>other</code> PathBasedHolder
     * instance. Comparison takes into account the {@link #path} first. If they
     * are not equal the result is returned: If the <code>other</code> path is
     * lexicographically sorted behind this {@link #path} a value larger than
     * zero is returned; otherwise a value smaller than zero is returned.
     * <p>
     * If the paths are the same, a positive number is returned if the
     * <code>other</code> service reference is ordered after this service
     * reference. If the service reference is the same, zero is returned.
     * <p>
     * As a special case, zero is returned if <code>other</code> is the same
     * object as this.
     * <p>
     * If this service reference is <code>null</code>, <code>-1</code> is always
     * returned; if the <code>other</code> service reference is
     * <code>null</code>, <code>+1</code> is returned.
     */
    @Override
    public final int compareTo(PathBasedHolder other) {

        // compare the path first, and return if not equal
        final int pathResult = other.path.compareTo(path);
        if (pathResult != 0) {
            return pathResult;
        }

        // now compare the service references giving priority to
        // to the higher priority service
        if (serviceReference == null) {
            return -1;
        } else if (other.serviceReference == null) {
            return 1;
        }

        return other.serviceReference.compareTo(serviceReference);
    }

    /**
     * Returns the hash code of the full path.
     */
    @Override
    public int hashCode() {
        return fullPath.hashCode();
    }

    /**
     * Returns <code>true</code> if the other object is the same as this or if
     * it is an instance of the same class with the same full path and the same
     * provider (<code>ServiceReference</code>).
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        }

        if (obj.getClass() == getClass()) {
            PathBasedHolder other = (PathBasedHolder) obj;
            return fullPath.equals(other.fullPath)
                && ((serviceReference == null && other.serviceReference == null) || (serviceReference != null && serviceReference.equals(other.serviceReference)));
        }

        return false;
    }
}