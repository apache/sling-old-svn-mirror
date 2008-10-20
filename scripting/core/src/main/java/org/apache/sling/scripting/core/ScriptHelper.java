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
import java.util.ArrayList;
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
import org.apache.sling.api.scripting.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.core.impl.helper.OnDemandReaderRequest;
import org.apache.sling.scripting.core.impl.helper.OnDemandWriterResponse;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Simple script helper providing access to the (wrapped) response, the
 * on-demand writer and a simple API for request inclusion. Instances of this
 * class are made available to the scripts as the global <code>sling</code>
 * variable.
 */
public class ScriptHelper implements SlingScriptHelper {

    private final SlingScript script;

    private final SlingHttpServletRequest request;

    private final SlingHttpServletResponse response;

    protected final BundleContext bundleContext;

    /**
     * The list of references - we don't need to synchronize this as we are
     * running in one single request.
     */
    protected final List<ServiceReference> references = new ArrayList<ServiceReference>();

    /** A map of found services. */
    protected final Map<String, Object> services = new HashMap<String, Object>();

    public ScriptHelper(BundleContext ctx, SlingScript script) {
        if (ctx == null ) {
            throw new IllegalArgumentException("Bundle context must not be null.");
        }
        this.bundleContext = ctx;
        this.request = null;
        this.response = null;
        this.script = script;
    }

    public ScriptHelper(BundleContext ctx, SlingScript script, SlingHttpServletRequest request,
            SlingHttpServletResponse response) {
        if (ctx == null ) {
            throw new IllegalArgumentException("Bundle context must not be null.");
        }
        this.bundleContext = ctx;
        this.script = script;
        this.request = new OnDemandReaderRequest(request);
        this.response = new OnDemandWriterResponse(response);
    }

    public SlingScript getScript() {
        return script;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public SlingHttpServletResponse getResponse() {
        return response;
    }

    /**
     * @trows SlingIOException Wrapping a <code>IOException</code> thrown
     *        while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    public void include(String path) {
        include(path, (RequestDispatcherOptions) null);
    }

    /** Include the output of another request, using specified options */
    public void include(String path, String options) {
        include(path, new RequestDispatcherOptions(options));
    }

    /** Include the output of another request, using specified options */
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
    
    /** Forward the request to another resource, using specified options */
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
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getService(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        ServiceType service = (ServiceType) this.services.get(type.getName());
        if (service == null) {
            final ServiceReference ref = this.bundleContext.getServiceReference(type.getName());
            if (ref != null) {
                this.references.add(ref);
                service = (ServiceType) this.bundleContext.getService(ref);
                this.services.put(type.getName(), service);
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
                final List<ServiceType> objects = new ArrayList<ServiceType>();
                for (int i = 0; i < refs.length; i++) {
                    this.references.add(refs[i]);
                    final ServiceType service = (ServiceType) this.bundleContext.getService(refs[i]);
                    if (service != null) {
                        objects.add(service);
                    }
                }
                if (objects.size() > 0) {
                    result = (ServiceType[]) objects.toArray();
                }
            }
            return result;
        } catch (InvalidSyntaxException ise) {
            throw new InvalidServiceFilterSyntaxException(filter,
                "Invalid filter syntax", ise);
        }
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#dispose()
     */
    public void dispose() {
        final Iterator<ServiceReference> i = this.references.iterator();
        while (i.hasNext()) {
            final ServiceReference ref = i.next();
            this.bundleContext.ungetService(ref);
        }
        this.references.clear();
        this.services.clear();
    }
}
