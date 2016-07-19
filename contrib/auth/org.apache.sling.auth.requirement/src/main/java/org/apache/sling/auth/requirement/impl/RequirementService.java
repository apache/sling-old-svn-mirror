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
package org.apache.sling.auth.requirement.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executor;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserverMBean;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.sling.auth.requirement.LoginPathProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Osgi Component implementing the {@code LoginPathProvider} interface.
 */
@Component(immediate = true, name = "org.apache.sling.auth.requirement.impl.RequirementService")
@Service(LoginPathProvider.class)
@Property(name = org.osgi.framework.Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
public class RequirementService implements LoginPathProvider {

    @SuppressWarnings("UnusedDeclaration")
    @Reference
    private RequirementHandler requirementHandler;

    @SuppressWarnings("UnusedDeclaration")
    @Reference
    private Executor executor;

    private BackgroundObserver backgroundObserver;

    private ServiceRegistration observerRegistration;
    private ServiceRegistration mbeanRegistration;

    //----------------------------------------------------------------< SCR >---
    @SuppressWarnings("UnusedDeclaration")
    @Activate
    protected void activate(BundleContext bundleContext) throws Exception {
        Observer observer = new RequirementObserver(requirementHandler);
        backgroundObserver = new BackgroundObserver(observer, executor);

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(org.osgi.framework.Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put(org.osgi.framework.Constants.SERVICE_DESCRIPTION, "Apache Sling Authentication Requirement Listener");

        observerRegistration = bundleContext.registerService(Observer.class.getName(), backgroundObserver, properties);

        final Dictionary<String, Object> mbeanProps = new Hashtable<String, Object>(properties);
        String objectName = String.format("org.apache.sling:type=%s,name=RequirementObserver", BackgroundObserverMBean.TYPE);
        mbeanProps.put("jmx.objectname", new ObjectName(objectName));

        mbeanRegistration = bundleContext.registerService(BackgroundObserverMBean.class.getName(), backgroundObserver.getMBean(), mbeanProps);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deactivate
    protected void deactivate() throws Exception {
        if (observerRegistration != null) {
            observerRegistration.unregister();
        }
        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
        }
        if (backgroundObserver != null) {
            backgroundObserver.close();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void bindRequirementHandler(@Nonnull RequirementHandler requirementHandler) {
        this.requirementHandler = requirementHandler;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void unbindRequirementHandler(@Nonnull RequirementHandler requirementHandler) {
        if (requirementHandler == this.requirementHandler) {
            this.requirementHandler = null;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void bindExecutor(@Nonnull Executor executor) {
        this.executor = executor;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void unbindExecutor(@Nonnull Executor executor) {
        if (executor == this.executor) {
            this.executor = null;
        }
    }

    //--------------------------------------------------< LoginPathProvider >---
    @CheckForNull
    @Override
    public String getLoginPath(@Nonnull HttpServletRequest request) {
        String path = getPathFromRequest(request);
        if (path != null && requirementHandler != null) {
            return requirementHandler.getLoginPath(path);
        } else {
            return null;
        }
    }


    //--------------------------------------------------------------------------
    @CheckForNull
    private static String getPathFromRequest(@Nonnull HttpServletRequest request) {
        String path = request.getParameter("resource");
        if (path == null) {
            path = request.getServletPath();
        }
        return path;
    }
}
