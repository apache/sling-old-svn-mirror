/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

/**
 *
 */
public class WrappedResourceProvider  implements ResourceProvider {

    private ResourceProvider resourceProvider;
    private Comparable<?> serviceReference;

    /**
     *
     */
    public WrappedResourceProvider(ResourceProvider resourceProvider, Comparable<?> serviceReference) {
        this.resourceProvider = resourceProvider;
        this.serviceReference = serviceReference;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(ResourceResolver arg0, String arg1) {
        return resourceProvider.getResource(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    public Resource getResource(ResourceResolver arg0, HttpServletRequest arg1, String arg2) {
        return resourceProvider.getResource(arg0, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
     */
    public Iterator<Resource> listChildren(Resource arg0) {
        return resourceProvider.listChildren(arg0);
    }

    /**
     *
     */
    public Comparable<?> getComparable() {
        return serviceReference;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return resourceProvider.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof WrappedResourceProvider ) {
            return resourceProvider.equals(((WrappedResourceProvider) obj).resourceProvider);
        } else if ( obj instanceof ResourceProvider) {
            return resourceProvider.equals(obj);
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return resourceProvider.toString();
    }
}
