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
package org.apache.sling.installer.core.impl.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.core.impl.Activator;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@SuppressWarnings("serial")
public class OsgiInstallerWebConsolePlugin extends GenericServlet {

    private final OsgiInstallerImpl installer;

    public static ServiceRegistration register(final BundleContext bundleContext,
            final OsgiInstallerImpl installer) {
        final OsgiInstallerWebConsolePlugin plugin = new OsgiInstallerWebConsolePlugin(
                installer);
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", "osgi-installer");
        props.put("felix.webconsole.title", "OSGi Installer");
        props.put("felix.webconsole.configprinter.modes", new String[] {"zip", "txt"});
        props.put(Constants.SERVICE_VENDOR, Activator.VENDOR);
        props.put(Constants.SERVICE_DESCRIPTION,
            "OSGi Installer Web Console Plugin");
        return bundleContext.registerService("javax.servlet.Servlet", plugin,
            props);
    }

    private OsgiInstallerWebConsolePlugin(
            final OsgiInstallerImpl installer) {
        this.installer = installer;
    }

    /**
     * Internal class to collect the current state.
     */
    private static final class State {
        public final List<EntityResourceList> activeResources = new ArrayList<EntityResourceList>();
        public final List<EntityResourceList> installedResources = new ArrayList<EntityResourceList>();
        public final List<RegisteredResource> untransformedResources = new ArrayList<RegisteredResource>();
    }

    /**
     * Get the current installer state.
     * This method should be called from within a synchronized block for the resources!
     */
    private State getCurrentState() {
        final State state = new State();

        for(final String entityId : this.installer.getPersistentResourceList().getEntityIds()) {
            final EntityResourceList group = this.installer.getPersistentResourceList().getEntityResourceList(entityId);
            if ( group.getActiveResource() != null ) {
                state.activeResources.add(group);
            } else {
                state.installedResources.add(group);
            }
        }
        state.untransformedResources.addAll(this.installer.getPersistentResourceList().getUntransformedResources());

        return state;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
    throws IOException {

        final PrintWriter pw = res.getWriter();

        synchronized ( this.installer.getResourcesLock() ) {
            final State state = this.getCurrentState();

            pw.println("<h1>Active Resources</h1>");
            pw.println("<ul>");
            for(final EntityResourceList group : state.activeResources) {
                final TaskResource toActivate = group.getActiveResource();
                pw.printf("<li>%s: %s, %s, %s</li>%n",
                        toActivate.getEntityId(),
                        toActivate.getDigest(),
                        toActivate.getScheme(),
                        toActivate.getState());
            }
            pw.println("</ul>");

            pw.println("<h1>Installed Resources</h1>");
            pw.println("<ul>");
            for(final EntityResourceList group : state.installedResources) {
                final Collection<TaskResource> resources = group.getResources();
                if (resources.size() > 0) {
                    pw.println("<ul>");
                    for (TaskResource resource : resources) {
                        pw.printf("<li>%s: %s, %s, %s</li>%n",
                            resource.getEntityId(), resource.getDigest(),
                            resource.getScheme(), resource.getState());
                    }
                    pw.println("</ul>");
                }
            }
            pw.println("</ul>");

            pw.println("<h1>Untransformed Resources</h1>");
            pw.println("<ul>");
            for(final RegisteredResource registeredResource : state.untransformedResources) {
                pw.printf("<li>%s: %s, %s</li>%n",
                    registeredResource.getEntityId(),
                    registeredResource.getDigest(), registeredResource.getScheme());
            }
            pw.println("</ul>");
        }
    }

    /**
     * Method for the configuration printer.
     */
    public void printConfiguration(final PrintWriter pw, final String mode) {
        if ( !"zip".equals(mode) && !"txt".equals(mode) ) {
            return;
        }
        pw.println("Apache Sling OSGi Installer");
        pw.println("===========================");
        synchronized ( this.installer.getResourcesLock() ) {
            final State state = this.getCurrentState();
            pw.println("Active Resources:");
            for(final EntityResourceList group : state.activeResources) {
                final TaskResource toActivate = group.getActiveResource();
                pw.printf("- %s: %s, %s, %s%n",
                        toActivate.getEntityId(),
                        toActivate.getDigest(),
                        toActivate.getScheme(),
                        toActivate.getState());
            }
            pw.println();

            pw.println("Installed Resources:");
            for(final EntityResourceList group : state.installedResources) {
                final Collection<TaskResource> resources = group.getResources();
                if (resources.size() > 0) {
                    pw.print("* ");
                    for (TaskResource resource : resources) {
                        pw.printf("- %s: %s, %s, %s%n",
                            resource.getEntityId(), resource.getDigest(),
                            resource.getScheme(), resource.getState());
                    }
                    pw.println("</ul>");
                }
            }
            pw.println();

            pw.println("Untransformed Resources:");
            for(final RegisteredResource registeredResource : state.untransformedResources) {
                pw.printf("- %s: %s, %s%n",
                    registeredResource.getEntityId(),
                    registeredResource.getDigest(), registeredResource.getScheme());
            }
        }
    }
}
