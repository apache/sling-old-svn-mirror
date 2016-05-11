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
package org.apache.sling.scripting.core;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.core.impl.helper.OnDemandReaderRequest;
import org.apache.sling.scripting.core.impl.helper.OnDemandWriterResponse;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

/**
 * Simple script helper providing access to the (wrapped) response, the
 * on-demand writer and a simple API for request inclusion. Instances of this
 * class are made available to the scripts as the global <code>sling</code>
 * variable.
 *
 * Client code using this object should take care to call {@link #cleanup()}
 * when the object is not used anymore!
 */
public class ScriptHelper implements SlingScriptHelper {

    /** The corresponding script. */
    private final SlingScript script;

    /** The current request or <code>null</code>. */
    private final SlingHttpServletRequest request;

    /** The current response or <code>null</code>. */
    private final SlingHttpServletResponse response;

    /** The bundle context. */
    protected final BundleContext bundleContext;

    /**
     * The list of references - we don't need to synchronize this as we are
     * running in one single request.
     */
    protected List<ServiceReference> references;

    /** A map of found services. */
    protected Map<String, Object> services;

    public ScriptHelper(final BundleContext ctx, final SlingScript script) {
        if (ctx == null ) {
            throw new IllegalArgumentException("Bundle context must not be null.");
        }
        this.request = null;
        this.response = null;
        this.script = script;
        this.bundleContext = ctx;
    }

    public ScriptHelper(final BundleContext ctx,
            final SlingScript script,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) {
        if (ctx == null ) {
            throw new IllegalArgumentException("Bundle context must not be null.");
        }
        this.script = script;
        this.request = new OnDemandReaderRequest(request);
        this.response = new OnDemandWriterResponse(response);
        this.bundleContext = ctx;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getScript()
     */
    public SlingScript getScript() {
        return script;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getRequest()
     */
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getResponse()
     */
    public SlingHttpServletResponse getResponse() {
        return response;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#include(java.lang.String)
     */
    public void include(String path) {
        include(path, (RequestDispatcherOptions) null);
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#include(java.lang.String, java.lang.String)
     */
    public void include(String path, String options) {
        include(path, new RequestDispatcherOptions(options));
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#include(java.lang.String, org.apache.sling.api.request.RequestDispatcherOptions)
     */
    public void include(String path, RequestDispatcherOptions options) {
        final RequestDispatcher dispatcher = getRequest().getRequestDispatcher(
            path, options);

        if (dispatcher != null) {
            try {
                dispatcher.include(getRequest(), getResponse());
            } catch (IOException ioe) {
                throw new SlingIOException(ioe);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        }
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(java.lang.String)
     */
    public void forward(String path) {
        forward(path, (RequestDispatcherOptions) null);
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(java.lang.String, java.lang.String)
     */
    public void forward(String path, String options) {
        forward(path, new RequestDispatcherOptions(options));
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(java.lang.String, org.apache.sling.api.request.RequestDispatcherOptions)
     */
    public void forward(String path, RequestDispatcherOptions options) {
        final RequestDispatcher dispatcher = getRequest().getRequestDispatcher(
            path, options);

        if (dispatcher != null) {
            try {
                dispatcher.forward(getRequest(), getResponse());
            } catch (IOException ioe) {
                throw new SlingIOException(ioe);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        }
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#dispose()
     */
    @Deprecated
    public void dispose() {
        LoggerFactory.getLogger(this.getClass()).error("ScriptHelper#dispose has been called. This method is deprecated and should never be called by clients!");
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getService(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        ServiceType service = (this.services == null ? null : (ServiceType) this.services.get(type.getName()));
        if (service == null) {
            final ServiceReference ref = this.bundleContext.getServiceReference(type.getName());
            if (ref != null) {
                service = (ServiceType) this.bundleContext.getService(ref);
                if ( service != null ) {
                    if ( this.services == null ) {
                        this.services = new HashMap<String, Object>();
                    }
                    if ( this.references == null ) {
                        this.references = new ArrayList<ServiceReference>();
                    }
                    this.references.add(ref);
                    this.services.put(type.getName(), service);
                }
            }
        }
        return service;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getServices(java.lang.Class, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(
            Class<ServiceType> serviceType, String filter)
    throws InvalidServiceFilterSyntaxException {
        try {
            final ServiceReference[] refs = this.bundleContext.getServiceReferences(
                serviceType.getName(), filter);
            ServiceType[] result = null;
            if (refs != null) {
                // sort by service ranking (lowest first) (see ServiceReference#compareTo(Object))
                List<ServiceReference> references = Arrays.asList(refs);
                Collections.sort(references);
                // get the highest ranking first
                Collections.reverse(references);
                
                final List<ServiceType> objects = new ArrayList<ServiceType>();
                for (ServiceReference reference : references) {
                    final ServiceType service = (ServiceType) this.bundleContext.getService(reference);
                    if (service != null) {
                        if ( this.references == null ) {
                            this.references = new ArrayList<ServiceReference>();
                        }
                        this.references.add(reference);
                        objects.add(service);
                    }
                }
                if (objects.size() > 0) {
                    ServiceType[] srv = (ServiceType[]) Array.newInstance(serviceType, objects.size());
                    result = objects.toArray(srv);
                }
            }
            return result;
        } catch (InvalidSyntaxException ise) {
            throw new InvalidServiceFilterSyntaxException(filter,
                "Invalid filter syntax", ise);
        }
    }

    /**
     * Clean up this instance.
     */
    public void cleanup() {
        if ( this.references != null ) {
            final Iterator<ServiceReference> i = this.references.iterator();
            while (i.hasNext()) {
                final ServiceReference ref = i.next();
                this.bundleContext.ungetService(ref);
            }
            this.references.clear();
        }
        if ( this.services != null ) {
            this.services.clear();
        }
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(org.apache.sling.api.resource.Resource)
     */
    public void forward(Resource resource) {
        forward(resource, (RequestDispatcherOptions) null);
    }


    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    public void forward(Resource resource, String options) {
        forward(resource, new RequestDispatcherOptions(options));
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(org.apache.sling.api.resource.Resource, org.apache.sling.api.request.RequestDispatcherOptions)
     */
    public void forward(Resource resource, RequestDispatcherOptions options) {
        final RequestDispatcher dispatcher = getRequest().getRequestDispatcher(
            resource, options);

        if (dispatcher != null) {
            try {
                dispatcher.forward(getRequest(), getResponse());
            } catch (IOException ioe) {
                throw new SlingIOException(ioe);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        }
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#forward(org.apache.sling.api.resource.Resource)
     */
    public void include(Resource resource) {
        include(resource, (RequestDispatcherOptions) null);
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#include(org.apache.sling.api.resource.Resource, java.lang.String)
     */
    public void include(Resource resource, String options) {
        include(resource, new RequestDispatcherOptions(options));
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#include(org.apache.sling.api.resource.Resource, org.apache.sling.api.request.RequestDispatcherOptions)
     */
    public void include(Resource resource, RequestDispatcherOptions options) {
        final RequestDispatcher dispatcher = getRequest().getRequestDispatcher(
            resource, options);

        if (dispatcher != null) {
            try {
                dispatcher.include(getRequest(), getResponse());
            } catch (IOException ioe) {
                throw new SlingIOException(ioe);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        }
    }
}
