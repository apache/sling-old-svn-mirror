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
package org.apache.sling.atom.taglib.internal;

import javax.servlet.ServletContext;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.media.MediaExtensionFactory;
import org.apache.abdera.ext.opensearch.model.OpenSearchExtensionFactory;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.parser.stax.FOMFactory;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class AbderaActivator implements BundleActivator {

    private static final String SERVLET_CONTEXT_SERVICE = "javax.servlet.ServletContext";

    private ServiceTracker servletContext;

    private Abdera abdera;

    public void start(BundleContext context) {
        servletContext = new ServletContextTracker(context);
        servletContext.open();
    }

    public void stop(BundleContext context) {
        if (servletContext != null) {
            servletContext.close();
            servletContext = null;
        }

        abdera = null;
    }

    private class ServletContextTracker extends ServiceTracker {

        private ServletContextTracker(BundleContext context) {
            super(context, SERVLET_CONTEXT_SERVICE, null);
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object service = super.addingService(reference);
            if (service instanceof ServletContext) {
                ServletContext ctx = (ServletContext) service;
                Object attr = ctx.getAttribute(AbstractAbderaHandler.ABDERA_ATTRIBUTE);
                if (attr instanceof Abdera) {
                    // nothing to do, strange that it already is here, though...
                } else if (attr != null) {
                    // some other attribute of the same name ??
                    // might log this and do nothing !
                } else {
                    // create Abdera
                    final Abdera abdera = getAbdera();
                    ctx.setAttribute(AbstractAbderaHandler.ABDERA_ATTRIBUTE,
                        abdera);
                }
            }
            return service;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof ServletContext) {
                ServletContext ctx = (ServletContext) service;
                Object attr = ctx.getAttribute(AbstractAbderaHandler.ABDERA_ATTRIBUTE);
                if (attr == abdera) {
                    ctx.removeAttribute(AbstractAbderaHandler.ABDERA_ATTRIBUTE);
                }
            }

            super.removedService(reference, service);
        }
    }

    private Abdera getAbdera() {
        if (abdera == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            try {
                ClassLoader osgiClassloader = getClass().getClassLoader();
                Thread.currentThread().setContextClassLoader(osgiClassloader);

                abdera = new Abdera();
                Factory f = abdera.getFactory();
                if (f instanceof FOMFactory) {
                    FOMFactory ff = (FOMFactory) f;
                    ff.registerExtension(new MediaExtensionFactory());
                    ff.registerExtension(new OpenSearchExtensionFactory());
                }

            } finally {

                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }

        return abdera;
    }
}
